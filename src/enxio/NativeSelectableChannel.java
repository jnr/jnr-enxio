/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package enxio;

import com.kenai.jaffl.Library;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 *
 * @author wayne
 */
public class NativeSelectableChannel extends AbstractSelectableChannel
        implements ByteChannel {
    private static final LibC libc = Library.loadLibrary("c", LibC.class);
    
    private final int fd;
    private final int validOps;

    public NativeSelectableChannel(SelectorProvider provider, int fd, int ops) {
        super(provider);
        this.fd = fd;
        this.validOps = ops;
    }
    public static NativeSelectableChannel forSocket(int fd) {
        return new NativeSelectableChannel(NativeSelectorProvider.getInstance(), fd,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT | SelectionKey.OP_ACCEPT);
    }
    public static NativeSelectableChannel forServerSocket(int fd) {
        return forSocket(fd);
    }
    public static NativeSelectableChannel forDevice(int fd) {
        return new NativeSelectableChannel(NativeSelectorProvider.getInstance(), fd,
                SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    
    @Override
    protected void implCloseSelectableChannel() throws IOException {
       libc.close(fd);
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        int flags = libc.fcntl(fd, LibC.F_GETFL, 0);
        if (block) {
            flags &= ~LibC.O_NONBLOCK;
        } else {
            flags |= LibC.O_NONBLOCK;
        }
        libc.fcntl(fd, LibC.F_SETFL, flags);
    }

    @Override
    public int validOps() {
        return validOps;
    }
    public int getFD() {
        return fd;
    }
    public int read(ByteBuffer dst) throws IOException {
        int n = libc.read(fd, dst, dst.remaining());
        if (n > 0) {
            dst.position(dst.position() + n);
        }
        return n;
    }

    public int write(ByteBuffer src) throws IOException {
        int n = libc.write(fd, src, src.remaining());
        if (n > 0) {
            src.position(src.position() + n);
        }
        return n;
    }
    private static interface LibC {
        public static final int F_GETFL = 3;
        public static final int F_SETFL = 4;
        public static final int O_NONBLOCK = 4;
        
        public int close(int fd);
        public int read(int fd, @Out ByteBuffer data, int size);
        public int write(int fd, @In ByteBuffer data, int size);
        public int fcntl(int fd, int cmd, int data);
    }
    public static void main(String[] args) {
        byte[] msg = { 'T', 'e', 's', 't', '\n', 0 };
        libc.write(1, ByteBuffer.wrap(msg), 5);
    }
}
