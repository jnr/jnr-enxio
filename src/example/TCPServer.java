/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package example;

import com.kenai.jaffl.Library;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
//import com.kenai.jaffl.byref.IntByReference;
import com.kenai.jaffl.struct.Struct;
import com.kenai.jaffl.struct.StructUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import enxio.NativeSelectableChannel;
import enxio.NativeSelectorProvider;

/**
 *
 * @author wayne
 */
public class TCPServer {
    static final LibC libc = Library.loadLibrary("c", LibC.class);
    static class sockaddr_in extends Struct {

        public final Unsigned8 sin_len = new Unsigned8();
        public final Unsigned8 sin_family = new Unsigned8();
        public final Unsigned16 sin_port = new Unsigned16();
        public final Unsigned32 sin_addr = new Unsigned32();
        public final Signed8[] sin_zero = array(new Signed8[8]);
    }
    private static interface LibC {
        static final int AF_INET = 2;
        static final int SOCK_STREAM = 1;

        int socket(int domain, int type, int protocol);
        int close(int fd);
        int listen(int fd, int backlog);
        int bind(int fd, sockaddr_in addr, int len);
        int accept(int fd, @Out sockaddr_in addr, int[] len);
        int read(int fd, @Out ByteBuffer data, int len);
        int read(int fd, @Out byte[] data, int len);
        int write(int fd, @In ByteBuffer data, int len);
    }
    static short htons(short val) {
        return (short) (((val >> 8) & 0xff) | ((val & 0xff) << 8));
    }
    static NativeSelectableChannel serverSocket(int port) {
        int fd = libc.socket(LibC.AF_INET, LibC.SOCK_STREAM, 0);
        System.out.println("fd=" + fd);
        sockaddr_in sin = new sockaddr_in();
        sin.sin_family.set((byte) LibC.AF_INET);
        sin.sin_port.set(htons((short) port));

        int rc = libc.bind(fd, sin, StructUtil.getSize(sin));
        System.out.println("bind() returned " + rc);
        rc = libc.listen(fd, 5);
        System.out.println("listen() returned " + rc);
        return NativeSelectableChannel.forServerSocket(fd);
    }
    private static abstract class IO {
        protected final NativeSelectableChannel channel;
        protected final Selector selector;
        public IO(Selector selector, NativeSelectableChannel ch) {
            this.selector = selector;
            this.channel = ch;
        }
        public abstract void read();
        public abstract void write();
    }
    private static class Accepter extends IO {
        public Accepter(Selector selector, NativeSelectableChannel ch) {
            super(selector, ch);
        }
        public void read() {
            sockaddr_in sin = new sockaddr_in();
            int[] addrSize = { StructUtil.getSize(sin) };
            int clientfd = libc.accept(channel.getFD(), sin, addrSize);
            System.out.println("client fd = " + clientfd);
            NativeSelectableChannel ch = NativeSelectableChannel.forSocket(clientfd);
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
        public Client(Selector selector, NativeSelectableChannel ch) {
            super(selector, ch);
        }
        public void read() {
            int n = libc.read(channel.getFD(), buf, buf.remaining());
            System.out.println("Read " + n + " bytes from client");
            if (n <= 0) {
                SelectionKey k = channel.keyFor(selector);
                k.cancel();
                libc.close(channel.getFD());
                return;
            }
            buf.position(n);
            buf.flip();
            channel.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        }
        public void write() {
            while (buf.hasRemaining()) {
                int n = libc.write(channel.getFD(), buf, buf.remaining());
                System.out.println("write returned " + n);
                if (n > 0) {
                    buf.position(buf.position() + n);
                }
                if (n == 0) {
                    return;
                }
                if (n < 0) {
                    channel.keyFor(selector).cancel();
                    libc.close(channel.getFD());
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
                NativeSelectableChannel ch = serverSocket(baseport + i);
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