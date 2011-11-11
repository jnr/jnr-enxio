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

import jnr.ffi.LastError;
import jnr.ffi.Library;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import java.io.IOException;
import java.nio.ByteBuffer;

class Native {
    static final jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getSystemRuntime();
    private static final class LibCHolder {
        private static final LibC libc = Library.loadLibrary("c", LibC.class);
    }

    private static LibC libc() {
        return LibCHolder.libc;
    }

    public static int close(int fd) {
        return libc().close(fd);
    }
    public static int read(int fd, ByteBuffer dst) throws IOException {
        if (dst == null) {
            throw new NullPointerException("Destination buffer cannot be null");
        }
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }

        int n = libc().read(fd, dst, dst.remaining());
        if (n > 0) {
            dst.position(dst.position() + n);
        }
        return n;
    }

    public static int write(int fd, ByteBuffer src) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source buffer cannot be null");
        }
        int n = libc().write(fd, src, src.remaining());
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

    public static String getLastErrorString() {
        return libc().strerror(LastError.getLastError(runtime));
    }

    public static interface LibC {
        public static final int F_GETFL = jnr.constants.platform.Fcntl.F_GETFL.intValue();
        public static final int F_SETFL = jnr.constants.platform.Fcntl.F_SETFL.intValue();
        public static final int O_NONBLOCK = jnr.constants.platform.OpenFlags.O_NONBLOCK.intValue();

        public int close(int fd);
        public int read(int fd, @Out ByteBuffer data, int size);
        public int write(int fd, @In ByteBuffer data, int size);
        public int fcntl(int fd, int cmd, int data);
        String strerror(int error);
    }
}
