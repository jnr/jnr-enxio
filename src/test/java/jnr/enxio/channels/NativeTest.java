package jnr.enxio.channels;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

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
        expectedEx.expect(IOException.class);
        Native.close(fd);
    }
}
