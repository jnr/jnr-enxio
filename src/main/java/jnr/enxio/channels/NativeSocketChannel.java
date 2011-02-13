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

import com.kenai.constantine.platform.Errno;
import com.kenai.jaffl.LastError;
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
                switch (Errno.valueOf(LastError.getLastError())) {
                    case EAGAIN:
                    case EWOULDBLOCK:
                        return 0;

                    default:
                        throw new IOException(Native.getLastErrorString());
                }
            default:
                return n;
        }
    }

    public int write(ByteBuffer src) throws IOException {
        return Native.write(fd, src);
    }
}
