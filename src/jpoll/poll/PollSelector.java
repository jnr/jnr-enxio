/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpoll.poll;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

/**
 *
 * @author wayne
 */
public class PollSelector extends java.nio.channels.spi.AbstractSelector {
    PollSelector(SelectorProvider provider) {
        super(provider);
    }
    @Override
    protected void implCloseSelector() throws IOException {
        
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<SelectionKey> keys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int selectNow() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int select(long timeout) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int select() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Selector wakeup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
