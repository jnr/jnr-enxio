package jnr.enxio.channels;

import jnr.ffi.Platform;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jnr.constants.platform.AddressFamily;
import jnr.constants.platform.Sock;
import jnr.ffi.LibraryLoader;
import jnr.ffi.annotations.Out;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

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

    /**
     * kqueue identifies a kevent by its (ident, filter) pair, so a descriptor that is both
     * readable and writable can be reported twice for the same key. The selector must
     * accumulate the ready ops across those events rather than let the second overwrite the
     * first.
     */
    @Test
    public void readableAndWritableReportedTogether() throws Exception {
        // Skip on non-unix
        if (!Platform.getNativePlatform().isUnix()) return;

        // a socketpair is the one cheap way to get an fd that is readable and writable at once
        int[] fds = new int[2];
        Assert.assertEquals(0, SocketPair.INSTANCE.socketpair(
                AddressFamily.AF_UNIX.intValue(), Sock.SOCK_STREAM.intValue(), 0, fds));

        NativeSocketChannel local = new NativeSocketChannel(fds[0]);
        NativeSocketChannel remote = new NativeSocketChannel(fds[1]);
        Selector selector = NativeSelectorProvider.getInstance().openSelector();
        try {
            remote.write(ByteBuffer.wrap(new byte[] { 'x' }));

            local.configureBlocking(false);
            SelectionKey key = local.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            Assert.assertEquals("one key became ready", 1, selector.selectNow());
            Assert.assertTrue("expected readable", key.isReadable());
            Assert.assertTrue("expected writable", key.isWritable());
        } finally {
            selector.close();
            local.close();
            remote.close();
        }
    }

    /** Declared here rather than in {@link Native} because only this test needs it. */
    public interface SocketPair {
        SocketPair INSTANCE = LibraryLoader.create(SocketPair.class)
                .load(Platform.getNativePlatform().getStandardCLibraryName());

        int socketpair(int domain, int type, int protocol, @Out int[] fds);
    }
}
