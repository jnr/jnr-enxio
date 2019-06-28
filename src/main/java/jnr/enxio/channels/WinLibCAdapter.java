/*
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

import java.nio.ByteBuffer;

import jnr.enxio.channels.Native.LibC;
import jnr.enxio.channels.Native.Timespec;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.provider.LoadedLibrary;
import jnr.ffi.types.size_t;
import jnr.ffi.types.ssize_t;

/**
 * MSVCRT.DLL only supports some LibC functions, but the symbols are different.
 * This adapter maps the MSVCRT.DLL names to standard LibC names
 */
public final class WinLibCAdapter implements LibC, LoadedLibrary {

    public static interface LibMSVCRT {

        public int _close(int fd);
        public @ssize_t int _read(int fd, @Out ByteBuffer data, @size_t long size);
        public @ssize_t int _read(int fd, @Out byte[] data, @size_t long size);
        public @ssize_t int _write(int fd, @In ByteBuffer data, @size_t long size);
        public @ssize_t int _write(int fd, @In byte[] data, @size_t long size);
        public int _pipe(@Out int[] fds);

        @IgnoreError String _strerror(int error);

        // These functions don't exist:

        //public int shutdown(int s, int how);
        //public int fcntl(int fd, int cmd, int data);
        //public int poll(@In @Out ByteBuffer pfds, int nfds, int timeout);
        //public int poll(@In @Out Pointer pfds, int nfds, int timeout);
        //public int kqueue();
        //public int kevent(int kq, @In ByteBuffer changebuf, int nchanges,
        //                  @Out ByteBuffer eventbuf, int nevents,
        //                  @In @Transient Timespec timeout);
        //public int kevent(int kq,
        //                  @In Pointer changebuf, int nchanges,
        //                  @Out Pointer eventbuf, int nevents,
        //                  @In @Transient Timespec timeout);
    }

    private LibMSVCRT win;
    public WinLibCAdapter(LibMSVCRT winlibc) { this.win = winlibc; }

    @Override public int close(int fd) { return win._close(fd); }
    @Override public int read(int fd, ByteBuffer data, long size) { return win._read(fd, data, size); }
    @Override public int read(int fd, byte[] data, long size) { return win._read(fd, data, size); }
    @Override public int write(int fd, ByteBuffer data, long size) { return win._write(fd, data, size); }
    @Override public int write(int fd, byte[] data, long size) { return win._write(fd, data, size); }
    @Override public int pipe(int[] fds) { return win._pipe(fds); }
    @Override public String strerror(int error) { return win._strerror(error); }

    @Override
    public Runtime getRuntime() {
        return Runtime.getRuntime(win);
    }

    // Unsupported Operations. Some may be implementable, others like fcntl may not be.

    @Override
    public int fcntl(int fd, int cmd, int data) {
        throw new UnsupportedOperationException("fcntl isn't supported on Windows");
    }

    @Override
    public int poll(ByteBuffer pfds, int nfds, int timeout) {
        throw new UnsupportedOperationException("poll isn't supported on Windows");
    }

    @Override
    public int poll(Pointer pfds, int nfds, int timeout) {
        throw new UnsupportedOperationException("poll isn't supported on Windows");
    }

    @Override
    public int kqueue() {
        throw new UnsupportedOperationException("kqueue isn't supported on Windows");
    }

    @Override
    public int kevent(int kq, ByteBuffer changebuf, int nchanges, ByteBuffer eventbuf, int nevents, Timespec timeout) {
        throw new UnsupportedOperationException("kevent isn't supported on Windows");
    }

    @Override
    public int kevent(int kq, Pointer changebuf, int nchanges, Pointer eventbuf, int nevents, Timespec timeout) {
        throw new UnsupportedOperationException("kevent isn't supported on Windows");
    }

    @Override
    public int shutdown(int s, int how) {
        throw new UnsupportedOperationException("shutdown isn't supported on Windows");
    }
}