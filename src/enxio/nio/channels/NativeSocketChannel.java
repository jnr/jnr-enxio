
package enxio.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

public class NativeSocketChannel extends AbstractSelectableChannel
        implements ByteChannel, NativeSelectableChannel {

    private final int fd;
    private final int validOps;

    public NativeSocketChannel(int fd) {
        this(NativeSelectorProvider.getInstance(), fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    public NativeSocketChannel(int fd, int ops) {
        this(NativeSelectorProvider.getInstance(), fd, ops);
    }
    NativeSocketChannel(SelectorProvider provider, int fd, int ops) {
        super(provider);
        this.fd = fd;
        this.validOps = ops;
    }

    @Override
    protected void implCloseSelectableChannel() throws IOException {
       Native.close(fd);
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        Native.setBlocking(fd, block);
    }

    @Override
    public final int validOps() {
        return validOps;
    }
    public final int getFD() {
        return fd;
    }
    public int read(ByteBuffer dst) throws IOException {
        int n = Native.read(fd, dst);
        switch (n) {
            case 0:
                return -1;
            case -1:
                throw new IOException(Native.getLastErrorString());
            default:
                return n;
        }
    }

    public int write(ByteBuffer src) throws IOException {
        return Native.write(fd, src);
    }
}
