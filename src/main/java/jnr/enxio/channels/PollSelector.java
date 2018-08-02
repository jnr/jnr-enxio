/*
 * This file is part of the JNR project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jnr.enxio.channels;

import jnr.constants.platform.Errno;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of a {@link java.nio.channels.Selector} that uses good old
 * poll(2)
 */
class PollSelector extends java.nio.channels.spi.AbstractSelector {
    private static final int POLLFD_SIZE = 8;
    private static final int FD_OFFSET = 0;
    private static final int EVENTS_OFFSET = 4;
    private static final int REVENTS_OFFSET = 6;

    static final int POLLIN = 0x1;
    static final int POLLOUT = 0x4;
    static final int POLLERR = 0x8;
    static final int POLLHUP = 0x10;

    
    private PollSelectionKey[] keyArray = new PollSelectionKey[0];
    private ByteBuffer pollData = null;
    private int nfds;
    
    private final int[] pipefd = { -1, -1 };
    private final Object regLock = new Object();
    
    private final Map<SelectionKey, Boolean> keys = new ConcurrentHashMap<SelectionKey, Boolean>();
    private final Set<SelectionKey> selected = new HashSet<SelectionKey>();


    public PollSelector(SelectorProvider provider) {
        super(provider);
        Native.libc().pipe(pipefd);
        // Register the wakeup pipe as the first element in the pollfd array
        pollData = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        putPollFD(0, pipefd[0]);
        putPollEvents(0, POLLIN);
        nfds = 1;
    }
    
    private void putPollFD(int idx, int fd) {
        pollData.putInt((idx * POLLFD_SIZE) + FD_OFFSET, fd);
    }

    private void putPollEvents(int idx, int events) {
        pollData.putShort((idx * POLLFD_SIZE) + EVENTS_OFFSET, (short) events);
    }

    private int getPollFD(int idx) {
        return pollData.getInt((idx * POLLFD_SIZE) + FD_OFFSET);
    }

    private short getPollEvents(int idx) {
        return pollData.getShort((idx * POLLFD_SIZE) + EVENTS_OFFSET);
    }

    private short getPollRevents(int idx) {
        return pollData.getShort((idx * POLLFD_SIZE) + REVENTS_OFFSET);
    }

    private void putPollRevents(int idx, int events) {
        pollData.putShort((idx * POLLFD_SIZE) + REVENTS_OFFSET, (short) events);
    }

    @Override
    protected void implCloseSelector() throws IOException {
        if (pipefd[0] != -1) {
            Native.close(pipefd[0]);
        }
        if (pipefd[1] != -1) {
            Native.close(pipefd[1]);
        }

        // remove all keys
        for (SelectionKey key : keys.keySet()) {
            remove((PollSelectionKey)key);
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
        return new HashSet<SelectionKey>(Arrays.asList(keyArray).subList(0, nfds));
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return selected;
    }


    void interestOps(PollSelectionKey k, int ops) {
        short events = 0;
        if ((ops & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ)) != 0) {
            events |= POLLIN;
        }
        if ((ops & (SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT)) != 0) {
            events |= POLLOUT;
        }
        putPollEvents(k.getIndex(), events);
    }


    private void add(PollSelectionKey k) {
        synchronized (regLock) {
            ++nfds;
            if (keyArray.length < nfds) {
                PollSelectionKey[] newArray = new PollSelectionKey[nfds + (nfds / 2)];
                System.arraycopy(keyArray, 0, newArray, 0, nfds - 1);
                keyArray = newArray;
                ByteBuffer newBuffer = ByteBuffer.allocateDirect(newArray.length * 8);
                if (pollData != null) {
                    newBuffer.put(pollData);
                }
                newBuffer.position(0);
                pollData = newBuffer.order(ByteOrder.nativeOrder());
            }
            k.setIndex(nfds - 1);
            keyArray[nfds - 1] = k;
            putPollFD(k.getIndex(), k.getFD());
            putPollEvents(k.getIndex(), 0);
            keys.put(k, true);
        }
    }


    private void remove(PollSelectionKey k) {
        int idx = k.getIndex();
        synchronized (regLock) {
            //
            // If not the last key, swap last one into the removed key's position
            //
            if (idx < (nfds - 1)) {
                PollSelectionKey last = keyArray[nfds - 1];
                keyArray[idx] = last;
                // Copy the data for the last key into place
                putPollFD(idx, getPollFD(last.getIndex()));
                putPollEvents(idx, getPollEvents(last.getIndex()));
                last.setIndex(idx);
            } else {
                putPollFD(idx, -1);
                putPollEvents(idx, 0);
            }
            keyArray[nfds - 1] = null;
            --nfds;
            synchronized (selected) {
                selected.remove(k);
            }
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

        int nready = 0;
        try {
            begin();

            do {
                nready = Native.libc().poll(pollData, nfds, (int) timeout);
            } while (nready < 0 && Errno.EINTR.equals(Errno.valueOf(Native.getRuntime().getLastError())));

        } finally {
            end();
        }

        if (nready < 1) {
            return nready;
        }

        if ((getPollRevents(0) & POLLIN) != 0) {
            wakeupReceived();
        }

        int updatedKeyCount = 0;
        for (SelectionKey k : keys.keySet()) {
            PollSelectionKey pk = (PollSelectionKey) k;
            int revents = getPollRevents(pk.getIndex());
            if (revents != 0) {
                putPollRevents(pk.getIndex(), 0);
                int iops = k.interestOps();
                int ops = 0;

                if ((revents & POLLIN) != 0) {
                    ops |= iops & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
                }

                if ((revents & POLLOUT) != 0) {
                    ops |= iops & (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                }

                // If an error occurred, enable all interested ops and let the
                // event handling code deal with it
                if ((revents & (POLLHUP | POLLERR)) != 0) {
                    ops = iops;
                }

                ((PollSelectionKey) k).readyOps(ops);
                ++updatedKeyCount;
                if (!selected.contains(k)) {
                    selected.add(k);
                }
            }
        }

        return updatedKeyCount;
    }

    private void wakeupReceived() throws IOException {
        Native.read(pipefd[0], ByteBuffer.allocate(1));
    }

    @Override
    public Selector wakeup() {
        try {
            Native.write(pipefd[1], ByteBuffer.allocate(1));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        return this;
    }
}
