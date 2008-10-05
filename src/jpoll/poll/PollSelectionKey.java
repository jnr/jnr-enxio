/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpoll.poll;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;
import jpoll.NativeSelectableChannel;

/**
 *
 * @author wayne
 */
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
        return channel;
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
