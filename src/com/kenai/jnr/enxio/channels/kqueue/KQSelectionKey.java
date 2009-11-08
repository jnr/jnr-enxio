
package com.kenai.jnr.enxio.channels.kqueue;


import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;
import com.kenai.jnr.enxio.channels.NativeSelectableChannel;

class KQSelectionKey extends AbstractSelectionKey {
    private final KQSelector selector;
    private final NativeSelectableChannel channel;
    private int interestOps = 0;
    private int readyOps = 0;
    
    public KQSelectionKey(KQSelector selector, NativeSelectableChannel channel, int ops) {
        this.selector = selector;
        this.channel = channel;
        this.interestOps = ops;
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
