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

package jnr.enxio.example;

import jnr.enxio.channels.NativeSelectableChannel;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.enxio.channels.NativeServerSocketChannel;
import jnr.enxio.channels.NativeSocketChannel;
import jnr.ffi.*;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.types.size_t;
import jnr.ffi.types.ssize_t;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;


/**
 *
 * @author wayne
 */
public class TCPServer {
    static final String[] libnames = Platform.getNativePlatform().getOS() == Platform.OS.SOLARIS
                        ? new String[] { "socket", "nsl", "c" }
                        : new String[] { Platform.getNativePlatform().getStandardCLibraryName() };
    static final LibC libc = Library.loadLibrary(LibC.class, libnames);
    static final jnr.ffi.Runtime runtime = jnr.ffi.Runtime.getSystemRuntime();

    public static class SockAddr extends Struct {
        public SockAddr() {
            super(runtime);
        }
    }

    static class BSDSockAddrIN extends SockAddr {

        public final Unsigned8 sin_len = new Unsigned8();
        public final Unsigned8 sin_family = new Unsigned8();
        public final Unsigned16 sin_port = new Unsigned16();
        public final Unsigned32 sin_addr = new Unsigned32();
        public final Padding sin_zero = new Padding(NativeType.SCHAR, 8);
    }

    static class SockAddrIN extends SockAddr {

        public final Unsigned16 sin_family = new Unsigned16();
        public final Unsigned16 sin_port = new Unsigned16();
        public final Unsigned32 sin_addr = new Unsigned32();
        public final Padding sin_zero = new Padding(NativeType.SCHAR, 8);
    }

    public static interface LibC {
        static final int AF_INET = jnr.constants.platform.AddressFamily.AF_INET.intValue();
        static final int SOCK_STREAM = jnr.constants.platform.Sock.SOCK_STREAM.intValue();

        int socket(int domain, int type, int protocol);
        int close(int fd);
        int listen(int fd, int backlog);
        int bind(int fd, SockAddr addr, int len);
        int accept(int fd, @Out SockAddr addr, int[] len);
        @ssize_t int read(int fd, @Out ByteBuffer data, @size_t int len);
        @ssize_t int read(int fd, @Out byte[] data, @size_t int len);
        @ssize_t int write(int fd, @In ByteBuffer data, @size_t int len);
        String strerror(int error);
    }
    static short htons(short val) {
        return Short.reverseBytes(val);
    }
    static NativeServerSocketChannel serverSocket(int port) {
        int fd = libc.socket(LibC.AF_INET, LibC.SOCK_STREAM, 0);
        System.out.println("fd=" + fd);
        SockAddr addr;
        if (Platform.getNativePlatform().isBSD()) {
            BSDSockAddrIN sin = new BSDSockAddrIN();
            sin.sin_family.set((byte) LibC.AF_INET);
            sin.sin_port.set(htons((short) port));
            addr = sin;
        } else {
            SockAddrIN sin = new SockAddrIN();
            sin.sin_family.set(htons((short) LibC.AF_INET));
            sin.sin_port.set(htons((short) port));
            addr = sin;
        }
        System.out.println("sizeof addr=" + Struct.size(addr));
        if (libc.bind(fd, addr, Struct.size(addr)) < 0) {
            System.err.println("bind failed: " + libc.strerror(LastError.getLastError(runtime)));
            System.exit(1);
        }
        if (libc.listen(fd, 5) < 0) {
            System.err.println("listen failed: " + libc.strerror(LastError.getLastError(runtime)));
            System.exit(1);
        }
        System.out.println("bind+listen succeeded");
        return new NativeServerSocketChannel(fd);
    }

    private static abstract class IO {
        protected final SelectableChannel channel;
        protected final Selector selector;
        public IO(Selector selector, SelectableChannel ch) {
            this.selector = selector;
            this.channel = ch;
        }
        public abstract void read();
        public abstract void write();
    }
    
    private static class Accepter extends IO {
        public Accepter(Selector selector, NativeServerSocketChannel ch) {
            super(selector, ch);
        }
        public void read() {
            SockAddrIN sin = new SockAddrIN();
            int[] addrSize = { Struct.size(sin) };
            int clientfd = libc.accept(((NativeSelectableChannel) channel).getFD(), sin, addrSize);
            System.out.println("client fd = " + clientfd);
            NativeSocketChannel ch = new NativeSocketChannel(clientfd);
            try {
                ch.configureBlocking(false);
                ch.register(selector, SelectionKey.OP_READ, new Client(selector, ch));
                selector.wakeup();
            } catch (IOException ex) {}
        }
        public void write() {
            SelectionKey k = channel.keyFor(selector);
            k.interestOps(SelectionKey.OP_ACCEPT);
        }
    }
    private static class Client extends IO {
        private final ByteBuffer buf = ByteBuffer.allocateDirect(1024);
        public Client(Selector selector, NativeSocketChannel ch) {
            super(selector, ch);
        }
        public void read() {
            int n = libc.read(((NativeSelectableChannel) channel).getFD(), buf, buf.remaining());
            System.out.println("Read " + n + " bytes from client");
            if (n <= 0) {
                SelectionKey k = channel.keyFor(selector);
                k.cancel();
                libc.close(((NativeSelectableChannel) channel).getFD());
                return;
            }
            buf.position(n);
            buf.flip();
            channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        }
        public void write() {
            while (buf.hasRemaining()) {
                int n = libc.write(((NativeSelectableChannel) channel).getFD(), buf, buf.remaining());
                System.out.println("write returned " + n);
                if (n > 0) {
                    buf.position(buf.position() + n);
                }
                if (n == 0) {
                    return;
                }
                if (n < 0) {
                    channel.keyFor(selector).cancel();
                    libc.close(((NativeSelectableChannel) channel).getFD());
                    return;
                }
            }
            System.out.println("outbuf empty");
            buf.clear();
            channel.keyFor(selector).interestOps(SelectionKey.OP_READ);
        }
    }
    public static void main(String[] args) {
        short baseport = 2000;
        try {
            Selector selector = NativeSelectorProvider.getInstance().openSelector();
            for (int i = 0; i < 2; ++i) {
                NativeServerSocketChannel ch = serverSocket(baseport + i);
                ch.configureBlocking(false);
                ch.register(selector, SelectionKey.OP_ACCEPT, new Accepter(selector, ch));
            }
            while (true) {
                selector.select();
                for (SelectionKey k : selector.selectedKeys()) {
                    if ((k.readyOps() & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0) {
                        ((IO) k.attachment()).read();
                    }
                    if ((k.readyOps() & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
                        ((IO) k.attachment()).write();
                    }
                }
            }
        } catch (IOException ex) {
        }
        
    }
}
