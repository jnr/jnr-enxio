package jnr.enxio.channels;

import jnr.constants.platform.Errno;

import java.io.IOException;

public class NativeException extends IOException {
    private final Errno errno;

    public NativeException(String message, Errno errno) {
        super(message);
        this.errno = errno;
    }

    public Errno getErrno() {
        return errno;
    }
}
