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
}
