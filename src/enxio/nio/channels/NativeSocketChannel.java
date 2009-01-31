
package enxio.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class NativeSocketChannel extends NativeSelectableChannel {

    public NativeSocketChannel(int fd) {
        this(fd, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
    public NativeSocketChannel(int fd, int ops) {
        super(NativeSelectorProvider.getInstance(), fd, ops);
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int n = super.read(dst);
        switch (n) {
            case 0:
                return -1;
            case -1:
                throw new IOException(Native.getLastErrorString());
            default:
                return n;
        }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return super.write(src);
    }
}
