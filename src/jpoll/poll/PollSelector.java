/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpoll.poll;

import com.googlecode.jffi.Library;
import com.googlecode.jffi.annotations.In;
import com.googlecode.jffi.annotations.Out;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import jpoll.NativeSelectableChannel;

/**
 *
 * @author wayne
 */
public class PollSelector extends java.nio.channels.spi.AbstractSelector {
    private static final LibC libc = Library.loadLibrary("c", LibC.class);

    private PollSelectionKey[] keyArray = new PollSelectionKey[0];
    private ByteBuffer pollData = null;
    private int nfds;
    
    private final int[] pipefd = { -1, -1 };
    private final Object regLock = new Object();
    
    private final Map<SelectionKey, Boolean> keys
            = new ConcurrentHashMap<SelectionKey, Boolean>();
    private final Set<SelectionKey> selected = new HashSet<SelectionKey>();
    public PollSelector(SelectorProvider provider) {
        super(provider);
        libc.pipe(pipefd);
    }
    private static final int fdOffset(PollSelectionKey k) {
        return k.getIndex() * 8;
    }
    private static final int reventsOffset(PollSelectionKey k) {
        return (k.getIndex() * 8) + 6;
    }
    private static final int eventsOffset(PollSelectionKey k) {
        return (k.getIndex() * 8) + 4;
    }
    @Override
    protected void implCloseSelector() throws IOException {
        if (pipefd[0] != -1) {
            libc.close(pipefd[0]);
        }
        if (pipefd[1] != -1) {
            libc.close(pipefd[1]);
        }
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        PollSelectionKey key = new PollSelectionKey(this, (NativeSelectableChannel) ch);
        add(key);
        key.attach(att);
        key.interestOps(ops);
        return key;
    }

    @Override
    public Set<SelectionKey> keys() {
        return new HashSet(Arrays.asList(keyArray).subList(0, nfds));
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return Collections.unmodifiableSet(selected);
    }
    void interestOps(PollSelectionKey k, int ops) {
        short events = 0;
        if ((ops & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ)) != 0) {
            events |= LibC.POLLIN;
        }
        if ((ops & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
            events |= LibC.POLLOUT;
        }
        pollData.putShort(eventsOffset(k), events);
    }
    private void add(PollSelectionKey k) {
        synchronized (regLock) {
            ++nfds;
            if (keyArray.length < nfds) {
                PollSelectionKey[] newArray = new PollSelectionKey[nfds + (nfds / 2)];
                System.arraycopy(keyArray, 0, newArray, 0, nfds - 1);
                keyArray = newArray;
                ByteBuffer newBuffer = ByteBuffer.allocate(newArray.length * 8);
                if (pollData != null) {
                    newBuffer.put(pollData);
                }
                newBuffer.position(0);
                pollData = newBuffer.order(ByteOrder.nativeOrder());
            }
            k.setIndex(nfds - 1);
            keyArray[nfds - 1] = k;
            pollData.putInt(fdOffset(k), k.getFd());
            pollData.putInt(eventsOffset(k), 0);
            keys.put(k, true);
            System.out.println("Added key at index=" + k.getIndex());
        }
    }
    private void remove(PollSelectionKey k) {
        int idx = k.getIndex();
        synchronized (regLock) {
            //
            // If not the last key, swap last one into the removed key's position
            //
            if (idx < (nfds - 1)) {
                System.out.println("Swapping " + (nfds - 1) + " with " + idx);
                PollSelectionKey last = keyArray[nfds - 1];
                keyArray[idx] = last;
                pollData.putLong(idx * 8, pollData.getLong(last.getIndex() * 8));
                last.setIndex(idx);
            } else {
                // Just clear out the key
                pollData.putLong(idx * 8, 0L);
            }
            keyArray[nfds - 1] = null;
            --nfds;
            keys.remove(k);
        }
        deregister(k);
    }
    @Override
    public int selectNow() throws IOException {
        return poll(0);
    }

    @Override
    public int select(long timeout) throws IOException {
        return poll(timeout > 0 ? timeout : -1);
    }

    @Override
    public int select() throws IOException {
        return poll(-1);
    }
    private int poll(long timeout) throws IOException {
        //
        // Remove any cancelled keys
        //
        Set<SelectionKey> cancelled = cancelledKeys();
        synchronized (cancelled) {
            for (SelectionKey k : cancelled) {
                remove((PollSelectionKey) k);
            }
            cancelled.clear();
        }
        selected.clear();
        System.out.print("Before poll(2): ");
        for (int i = 0; i < nfds; ++i) {
            System.out.print(pollData.getInt( i * 8));
            System.out.print(", ");
        }
        System.out.println();
        int n = libc.poll(pollData, nfds, (int) timeout);
        System.out.print("After poll(2): ");
        for (int i = 0; i < nfds; ++i) {
            System.out.print(pollData.getInt(i * 8));
            System.out.print(", ");
        }
        System.out.println();
        for (SelectionKey k : keys.keySet()) {
            PollSelectionKey pk = (PollSelectionKey) k;
            short revents = pollData.getShort(reventsOffset(pk));
            if (revents != 0) {
                int iops = k.interestOps();
                int ops = 0;
                if ((revents & LibC.POLLIN) != 0) {
                    ops = iops & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
                }
                if ((revents & LibC.POLLOUT) != 0) {
                    ops = iops & (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                }
                ((PollSelectionKey) k).readyOps(ops);
                selected.add(k);
            }
        }
        return n;
    }
    @Override
    public Selector wakeup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    private static interface LibC {
        static final int POLLIN = 0x1;
        static final int POLLOUT = 0x4;
        public int poll(ByteBuffer pfds, int nfds, int timeout);
        public int pipe(@Out int[] fds);
        public int close(int fd);
        public int read(int fd, @Out ByteBuffer data, int size);
        public int write(int fd, @In ByteBuffer data, int size);
    }
}
