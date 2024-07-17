package jnr.enxio.channels;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.channels.Pipe;

public class NativeTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void closeThrowsOnNativeError() throws Exception {
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
}
