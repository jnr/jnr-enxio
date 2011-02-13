/*
 * This file is part of the JNR project.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package jnr.enxio.channels;

import com.kenai.jaffl.LastError;
import com.kenai.jaffl.Library;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import java.io.IOException;
import java.nio.ByteBuffer;

class Native {
    private static final LibC libc = Library.loadLibrary("c", LibC.class);

    public static int close(int fd) {
        return libc.close(fd);
    }
    public static int read(int fd, ByteBuffer dst) throws IOException {
        if (dst == null) {
            throw new NullPointerException("Destination buffer cannot be null");
        }
        if (dst.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        }

        int n = libc.read(fd, dst, dst.remaining());
        if (n > 0) {
            dst.position(dst.position() + n);
        }
        return n;
    }

    public static int write(int fd, ByteBuffer src) throws IOException {
        if (src == null) {
            throw new NullPointerException("Source buffer cannot be null");
        }
        int n = libc.write(fd, src, src.remaining());
        if (n > 0) {
            src.position(src.position() + n);
        }
        return n;
    }

    public static void setBlocking(int fd, boolean block) {
        int flags = libc.fcntl(fd, LibC.F_GETFL, 0);
        if (block) {
            flags &= ~LibC.O_NONBLOCK;
        } else {
            flags |= LibC.O_NONBLOCK;
        }
        libc.fcntl(fd, LibC.F_SETFL, flags);
    }

    public static String getLastErrorString() {
        return libc.strerror(LastError.getLastError());
    }

    public static interface LibC {
        public static final int F_GETFL = com.kenai.constantine.platform.Fcntl.F_GETFL.value();
        public static final int F_SETFL = com.kenai.constantine.platform.Fcntl.F_SETFL.value();
        public static final int O_NONBLOCK = com.kenai.constantine.platform.OpenFlags.O_NONBLOCK.value();

        public int close(int fd);
        public int read(int fd, @Out ByteBuffer data, int size);
        public int write(int fd, @In ByteBuffer data, int size);
        public int fcntl(int fd, int cmd, int data);
        String strerror(int error);
    }
}
