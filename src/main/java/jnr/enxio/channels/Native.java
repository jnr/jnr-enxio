/*
 * Copyright (C) 2008 Wayne Meissner
 *
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jnr.enxio.channels;

import jnr.constants.platform.Errno;
import jnr.ffi.*;
import jnr.ffi.Runtime;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Transient;
import jnr.ffi.types.size_t;
import jnr.ffi.types.ssize_t;
import jnr.ffi.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class Native {

    public static interface LibC {
        public static final int F_GETFL = jnr.constants.platform.Fcntl.F_GETFL.intValue();
        public static final int F_SETFL = jnr.constants.platform.Fcntl.F_SETFL.intValue();
        public static final int O_NONBLOCK = jnr.constants.platform.OpenFlags.O_NONBLOCK.intValue();

        public int close(int fd);
        public @ssize_t int read(int fd, @Out ByteBuffer data, @size_t long size);
        public @ssize_t int read(int fd, @Out byte[] data, @size_t long size);
        public @ssize_t int write(int fd, @In ByteBuffer data, @size_t long size);
        public @ssize_t int write(int fd, @In byte[] data, @size_t long size);
        public int fcntl(int fd, int cmd, int data);
        public int poll(@In @Out ByteBuffer pfds, int nfds, int timeout);
        public int poll(@In @Out Pointer pfds, int nfds, int timeout);
        public int kqueue();
        public int kevent(int kq, @In ByteBuffer changebuf, int nchanges,
                          @Out ByteBuffer eventbuf, int nevents,
                          @In @Transient Timespec timeout);
        public int kevent(int kq,
                          @In Pointer changebuf, int nchanges,
                          @Out Pointer eventbuf, int nevents,
                          @In @Transient Timespec timeout);
        public int pipe(@Out int[] fds);
        public int shutdown(int s, int how);

        @IgnoreError String strerror(int error);
    }

    private static final class SingletonHolder {
        static final LibC libc = LibraryLoader.create(LibC.class).load(Platform.getNativePlatform().getStandardCLibraryName());
        static final jnr.ffi.Runtime runtime = Runtime.getRuntime(libc);
    }

    static LibC libc() {
        return SingletonHolder.libc;
    }

    static jnr.ffi.Runtime getRuntime() {
        return SingletonHolder.runtime;
    }

    public static int close(int fd) throws IOException {
        int rc;
        do {
            rc = libc().close(fd);
        } while (rc < 0 && Errno.EINTR.equals(getLastError()));

        if (rc < 0) {
            String message = String.format("Error closing fd %d: %s", fd, getLastErrorString());
            throw new NativeException(message, getLastError());
        } else {
            return rc;
        }
    }

    public static int read(int fd, ByteBuffer dst) throws IOException {
        if (dst == null) {
            throw new NullPointerException("Destination buffer cannot be null");
        }
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }

        int n;
        do {
            n = libc().read(fd, dst, dst.remaining());
        } while (n < 0 && Errno.EINTR.equals(getLastError()));

        if (n > 0) {
            dst.position(dst.position() + n);
        }

        return n;
    }

    public static int write(int fd, ByteBuffer src) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source buffer cannot be null");
        }

        int n;
        do {
            n = libc().write(fd, src, src.remaining());
        } while (n < 0 && Errno.EINTR.equals(getLastError()));

        if (n > 0) {
            src.position(src.position() + n);
        }

        return n;
    }

    public static void setBlocking(int fd, boolean block) {
        int flags = libc().fcntl(fd, LibC.F_GETFL, 0);
        if (block) {
            flags &= ~LibC.O_NONBLOCK;

        } else {
            flags |= LibC.O_NONBLOCK;
        }

        libc().fcntl(fd, LibC.F_SETFL, flags);
    }
    
    public static int shutdown(int fd, int how) {
        return libc().shutdown(fd, how);
    }

    public static String getLastErrorString() {
        return libc().strerror(LastError.getLastError(getRuntime()));
    }

    public static Errno getLastError() {
        return Errno.valueOf(LastError.getLastError(getRuntime()));
    }

    public static final class Timespec extends Struct {
            public final SignedLong tv_sec = new SignedLong();
            public final SignedLong tv_nsec = new SignedLong();

            public Timespec() {
                 super(Native.getRuntime());
            }

            public Timespec(Runtime runtime) {
                super(runtime);
            }

            public Timespec(long sec, long nsec) {
                super(Native.getRuntime());
                tv_sec.set(sec);
                tv_nsec.set(nsec);
            }
        }

}
