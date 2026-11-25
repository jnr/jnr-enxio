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
import jnr.enxio.channels.WinLibCAdapter.LibMSVCRT;
import jnr.ffi.*;
import jnr.ffi.Platform.OS;
import jnr.ffi.Runtime;
import jnr.ffi.annotations.IgnoreError;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.Transient;
import jnr.ffi.annotations.Variadic;
import jnr.ffi.types.size_t;
import jnr.ffi.types.ssize_t;
import jnr.ffi.Platform;
import jnr.ffi.types.u_int64_t;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReferenceArray;

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
        @Variadic(fixedCount = 2)
        public int fcntl(int fd, int cmd, @u_int64_t int data);
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
        static final LibC libc;
        static final jnr.ffi.Runtime runtime;

        static {
            Platform platform = Platform.getNativePlatform();
            LibraryLoader<LibC> loader = LibraryLoader.create(LibC.class);
            loader.library(platform.getStandardCLibraryName());
            if (platform.getOS() == OS.SOLARIS) {
                loader.library("socket");
            }
            LibC straight = loader.load();
            if (platform.getOS() == OS.WINDOWS)    {
                LibMSVCRT mslib = LibraryLoader.create(LibMSVCRT.class).load(platform.getStandardCLibraryName());
                libc = new WinLibCAdapter(mslib);
            } else {
                libc = straight;
            }
            runtime = Runtime.getRuntime(libc);
        }
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

    public static boolean getBlocking(int fd) {
        int flags = libc().fcntl(fd, LibC.F_GETFL, 0);

        return !((flags & LibC.O_NONBLOCK) == LibC.O_NONBLOCK);
    }
    
    public static int shutdown(int fd, int how) {
        return libc().shutdown(fd, how);
    }

    // <poll.h> values shared by Linux, macOS, the BSDs and Solaris, the platforms isPollSupported accepts
    public static final short POLLIN = 0x1;
    public static final short POLLOUT = 0x4;
    public static final short POLLERR = 0x8;
    public static final short POLLHUP = 0x10;
    public static final short POLLNVAL = 0x20;

    // struct pollfd { int fd; short events; short revents; }
    static final int POLLFD_SIZE = 8;
    static final int FD_OFFSET = 0;
    static final int EVENTS_OFFSET = 4;
    static final int REVENTS_OFFSET = 6;

    // Reused, since a fresh buffer per call makes poll two to three times slower. A caller owns the buffer it takes
    // out of the slot for its thread id until it puts it back, so threads, virtual threads and fibers never share one.
    // The slot count is the next power of two at or above twice the CPUs, so a thread id can be masked to a slot.
    static final int POLLFDS_SLOTS = Integer.highestOneBit(java.lang.Runtime.getRuntime().availableProcessors() * 2 - 1) << 1;
    private static final int POLLFDS_SPACING = 32; // at least a 128-byte cache line of references
    private static final AtomicReferenceArray<Pointer> POLLFDS =
            new AtomicReferenceArray<Pointer>((POLLFDS_SLOTS + 1) * POLLFDS_SPACING);

    private static final boolean POLL_SUPPORTED = pollSupported();

    // AIX, for one, has other values, and Windows has no poll(2)
    private static boolean pollSupported() {
        Platform platform = Platform.getNativePlatform();
        OS os = platform.getOS();
        return platform.isBSD() || os == OS.LINUX || os == OS.ZLINUX || os == OS.SOLARIS;
    }

    /** Whether {@link #poll} and the POLL* constants can be used on this platform. */
    public static boolean isPollSupported() {
        return POLL_SUPPORTED;
    }

    /**
     * poll(2) over plain descriptors, only where {@link #isPollSupported()} is true. Fills {@code revents} unless it
     * returns -1, when the error is in {@link #getLastError()}; EINTR isn't retried. The timeout is in milliseconds,
     * and a negative one waits forever. A negative descriptor is skipped and gets 0. List each descriptor once, as
     * macOS reports a duplicate in only one entry, and prefer a zero timeout: any other can block in native code, out
     * of reach of interrupts and pinning a virtual thread's carrier.
     */
    public static int poll(int[] fds, short[] events, short[] revents, int timeout) {
        if (!POLL_SUPPORTED) {
            throw new UnsupportedOperationException("poll isn't supported on " + Platform.getNativePlatform().getOS());
        }

        int nfds = fds.length;
        if (events.length < nfds || revents.length < nfds) {
            throw new IllegalArgumentException("events and revents need an entry for every descriptor");
        }

        // + 1 keeps slots off the array header's cache line
        int slot = (((int) Thread.currentThread().getId() & (POLLFDS_SLOTS - 1)) + 1) * POLLFDS_SPACING;
        Pointer pollfds = POLLFDS.getAndSet(slot, null);
        if (pollfds == null || pollfds.size() < (long) nfds * POLLFD_SIZE) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(Math.max(nfds, 8) * POLLFD_SIZE).order(ByteOrder.nativeOrder());
            pollfds = Pointer.wrap(getRuntime(), buffer);
        }

        for (int i = 0; i < nfds; i++) {
            long pollfd = (long) i * POLLFD_SIZE;
            pollfds.putInt(pollfd + FD_OFFSET, fds[i]);
            pollfds.putShort(pollfd + EVENTS_OFFSET, events[i]);
            pollfds.putShort(pollfd + REVENTS_OFFSET, (short) 0);
        }

        int n = libc().poll(pollfds, nfds, timeout);
        if (n >= 0) {
            for (int i = 0; i < nfds; i++) {
                revents[i] = pollfds.getShort((long) i * POLLFD_SIZE + REVENTS_OFFSET);
            }
        }

        POLLFDS.lazySet(slot, pollfds);
        return n;
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
