package jnr.enxio.channels;

import jnr.ffi.Platform;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Sock;
import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.Out;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class NativeTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void closeThrowsOnNativeError() throws Exception {
        // Skip on non-unix
        if (!Platform.getNativePlatform().isUnix()) return;

        FileOutputStream fos = new FileOutputStream("/dev/null");
        FileDescriptor descriptor = fos.getFD();
        Field fdField = descriptor.getClass().getDeclaredField("fd");
        fdField.setAccessible(true);
        int fd = (int)(Integer)fdField.get(descriptor);
        Native.close(fd);
        expectedEx.expect(NativeException.class);
        Native.close(fd);
    }

    @Test
    public void setBlocking() throws Exception {
        // Skip on non-unix
        if (!Platform.getNativePlatform().isUnix()) return;

        Pipe pipe = Pipe.open();
        Pipe.SinkChannel sink = pipe.sink();
//        sink.getClass().getModule().addOpens("sun.nio.ch", NativeTest.class.getModule());
        Field fd1 = sink.getClass().getDeclaredField("fd");
        fd1.setAccessible(true);
        FileDescriptor descriptor = (FileDescriptor) fd1.get(sink);
        Field fdField = descriptor.getClass().getDeclaredField("fd");
        fdField.setAccessible(true);
        int fd = (int)(Integer)fdField.get(descriptor);
        Assert.assertEquals(true, Native.getBlocking(fd));
        Native.setBlocking(fd, false);
        Assert.assertEquals(false, Native.getBlocking(fd));
    }

    @Test
    public void selectReportsReadableAndWritableTogether() throws Exception {
        if (!Platform.getNativePlatform().isUnix()) return;

        // kqueue reports EVFILT_READ and EVFILT_WRITE as separate events for one key;
        // the selector must merge them, not let the second overwrite the first
        int[] fds = new int[2];
        Assert.assertEquals(0, SocketPair.INSTANCE.socketpair(
                AddressFamily.AF_UNIX.intValue(), Sock.SOCK_STREAM.intValue(), 0, fds));

        try (Selector selector = NativeSelectorProvider.getInstance().openSelector();
             NativeSocketChannel local = new NativeSocketChannel(fds[0]);
             NativeSocketChannel remote = new NativeSocketChannel(fds[1])) {
            remote.write(ByteBuffer.wrap(new byte[] { 'x' }));
            local.configureBlocking(false);
            SelectionKey key = local.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            Assert.assertEquals(1, selector.selectNow());
            Assert.assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key.readyOps());
        }
    }

    public interface SocketPair {
        SocketPair INSTANCE = LibraryLoader.create(SocketPair.class)
                .load(Platform.getNativePlatform().getStandardCLibraryName());

        int socketpair(int domain, int type, int protocol, @Out int[] fds);
    }

    @Test
    public void pollSelectorReportsDeviceReady() throws Exception {
        // Skip on non-unix
        if (!Platform.getNativePlatform().isUnix()) return;

        try (RandomAccessFile file = new RandomAccessFile("/dev/null", "rw")) {
            FileDescriptor descriptor = file.getFD();
            Field fdField = descriptor.getClass().getDeclaredField("fd");
            fdField.setAccessible(true);
            int fd = (int)(Integer)fdField.get(descriptor);

            NativeDeviceChannel channel = new NativeDeviceChannel(fd, true);
            channel.configureBlocking(false);
            try (Selector selector = channel.provider().openSelector()) {
                SelectionKey key = channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                Assert.assertEquals(1, selector.selectNow());
                Assert.assertEquals(SelectionKey.OP_READ | SelectionKey.OP_WRITE, key.readyOps());
            }
        }
    }

    @Test
    public void pollReportsPipeReadiness() throws Exception {
        Assume.assumeTrue(Native.isPollSupported());

        int[] pipe = pipe();
        try {
            int[] fds = { pipe[0], pipe[1] };
            short[] events = { Native.POLLIN, Native.POLLOUT };
            short[] revents = new short[2];

            // nothing written yet: only the write end is ready
            Assert.assertEquals(1, Native.poll(fds, events, revents, 0));
            Assert.assertEquals(0, revents[0]);
            Assert.assertEquals(Native.POLLOUT, revents[1]);

            Assert.assertEquals(1, Native.libc().write(pipe[1], new byte[] { 1 }, 1));

            Assert.assertEquals(2, Native.poll(fds, events, revents, 0));
            Assert.assertEquals(Native.POLLIN, revents[0]);
            Assert.assertEquals(Native.POLLOUT, revents[1]);
        } finally {
            close(pipe);
        }
    }

    @Test
    public void pollReportsHangup() throws Exception {
        Assume.assumeTrue(Native.isPollSupported());

        int[] pipe = pipe();
        Native.close(pipe[1]);
        try {
            short[] revents = new short[1];

            Assert.assertEquals(1, Native.poll(new int[] { pipe[0] }, new short[] { Native.POLLIN }, revents, 0));
            // EOF is POLLIN on some systems and POLLHUP on others
            Assert.assertNotEquals(0, revents[0] & (Native.POLLIN | Native.POLLHUP));
        } finally {
            Native.close(pipe[0]);
        }
    }

    @Test
    public void pollReportsInvalidDescriptors() {
        Assume.assumeTrue(Native.isPollSupported());

        // never open, so no other thread can reuse it
        short[] revents = new short[1];
        Assert.assertEquals(1, Native.poll(new int[] { Integer.MAX_VALUE }, new short[] { Native.POLLIN }, revents, 0));
        Assert.assertEquals(Native.POLLNVAL, revents[0]);
    }

    @Test
    public void pollGrowsItsBuffer() throws Exception {
        Assume.assumeTrue(Native.isPollSupported());

        // more than the smallest buffer holds, all distinct
        int count = 20;
        int[][] pipes = new int[count][];
        int[] fds = new int[count];
        short[] events = new short[count];
        short[] revents = new short[count];
        Arrays.fill(events, Native.POLLOUT);
        try {
            for (int i = 0; i < count; i++) {
                pipes[i] = pipe();
                fds[i] = pipes[i][1];
            }

            // leave the smallest buffer in the slot, so the next poll has to grow it
            Assert.assertEquals(1, Native.poll(new int[] { fds[0] }, new short[] { Native.POLLOUT }, new short[1], 0));
            Assert.assertEquals(count, Native.poll(fds, events, revents, 0));
            for (short r : revents) Assert.assertEquals(Native.POLLOUT, r);
        } finally {
            for (int[] pipe : pipes) if (pipe != null) close(pipe);
        }
    }

    @Test
    public void pollKeepsConcurrentCallersApart() throws Exception {
        Assume.assumeTrue(Native.isPollSupported());

        // more threads than slots, so threads share slots
        int threads = 2 * Native.POLLFDS_SLOTS;
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
        int[][] pipes = new int[2][];
        Thread[] workers = new Thread[threads];
        try {
            pipes[0] = pipe();
            pipes[1] = pipe();
            Assert.assertEquals(1, Native.libc().write(pipes[0][1], new byte[] { 1 }, 1));

            // no timeout on the latch for a slow host to trip
            try {
                for (int t = 0; t < threads; t++) {
                    final boolean readable = t % 2 == 0;
                    final int[] fds = { pipes[t % 2][0] };
                    workers[t] = new Thread(new Runnable() {
                        public void run() {
                            try {
                                short[] events = { Native.POLLIN };
                                short[] revents = new short[1];
                                short expected = readable ? Native.POLLIN : 0;

                                start.await();
                                for (int i = 0; i < 10000; i++) {
                                    Assert.assertEquals(readable ? 1 : 0, Native.poll(fds, events, revents, 0));
                                    Assert.assertEquals(expected, revents[0]);
                                }
                            } catch (Throwable e) {
                                failure.compareAndSet(null, e);
                            }
                        }
                    });
                    workers[t].start();
                }
            } finally {
                start.countDown();
            }
            for (Thread worker : workers) if (worker != null) worker.join();
        } finally {
            for (int[] pipe : pipes) if (pipe != null) close(pipe);
        }
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test
    public void pollRejectsShortEventArrays() {
        Assume.assumeTrue(Native.isPollSupported());

        expectedEx.expect(IllegalArgumentException.class);
        Native.poll(new int[2], new short[1], new short[2], 0);
    }

    @Test
    public void pollRejectsShortReventArrays() {
        Assume.assumeTrue(Native.isPollSupported());

        expectedEx.expect(IllegalArgumentException.class);
        Native.poll(new int[2], new short[2], new short[1], 0);
    }

    private static int[] pipe() {
        int[] pipe = new int[2];
        Assert.assertEquals(0, Native.libc().pipe(pipe));
        return pipe;
    }

    private static void close(int[] pipe) throws Exception {
        Native.close(pipe[0]);
        Native.close(pipe[1]);
    }
}
