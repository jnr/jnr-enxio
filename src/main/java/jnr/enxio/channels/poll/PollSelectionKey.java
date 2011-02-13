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

package jnr.enxio.channels.poll;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;
import jnr.enxio.channels.NativeSelectableChannel;

class PollSelectionKey extends AbstractSelectionKey {
    private final PollSelector selector;
    private final NativeSelectableChannel channel;
    private int interestOps = 0;
    private int readyOps = 0;
    private int index = -1;
    
    public PollSelectionKey(PollSelector selector, NativeSelectableChannel channel) {
        this.selector = selector;
        this.channel = channel;
    }

    void setIndex(int index) {
        this.index = index;
    }
    int getIndex() {
        return index;
    }
    int getFD() {
        return channel.getFD();
    }
    
    @Override
    public SelectableChannel channel() {
        return (SelectableChannel) channel;
    }

    @Override
    public Selector selector() {
        return selector;
    }

    @Override
    public int interestOps() {
        return interestOps;
    }

    @Override
    public SelectionKey interestOps(int ops) {
        interestOps = ops;
        selector.interestOps(this, ops);
        return this;
    }

    @Override
    public int readyOps() {
        return readyOps;
    }
    void readyOps(int readyOps) {
        this.readyOps = readyOps;
    }
}
