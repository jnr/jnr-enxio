/*
 * Copyright (C) 2008 Wayne Meissner
 *
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

package jnr.enxio.channels.kqueue;

import com.kenai.jaffl.Library;
import com.kenai.jaffl.Platform;
import com.kenai.jaffl.annotations.In;
import com.kenai.jaffl.annotations.Out;
import com.kenai.jaffl.annotations.Transient;
import com.kenai.jaffl.struct.Struct;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.enxio.channels.NativeSelectorProvider;

/**
 * An implementation of a {@link java.nio.channels.Selector} that uses the BSD (including MacOS)
 * kqueue(2) mechanism
 */
public class KQSelector extends java.nio.channels.spi.AbstractSelector {
    private static final LibC libc = Library.loadLibrary("c", LibC.class);
    private static final EventIO io = EventIO.getInstance();
    private static final int MAX_EVENTS = 100;
    private static final int EVFILT_READ = -1;
    private static final int EVFILT_WRITE = -2;
    private static final int EV_ADD = 0x0001;
    private static final int EV_DELETE = 0x0002;
    private static final int EV_ENABLE = 0x0004;
    private static final int EV_DISABLE = 0x0008;
    private static final int EV_CLEAR = 0x0020;
    private final ByteBuffer changebuf;
    private final ByteBuffer eventbuf;
    private int kqfd = -1;
    private final int[] pipefd = { -1, -1 };
    private final Object regLock = new Object();
    private final Map<Integer, Descriptor> descriptors
            = new ConcurrentHashMap<Integer, Descriptor>();
    private final Set<SelectionKey> selected = new LinkedHashSet<SelectionKey>();
    private final Set<Descriptor> changed = new LinkedHashSet<Descriptor>();
    private final Timespec ZERO_TIMESPEC = new Timespec(0, 0);
    
    public KQSelector(NativeSelectorProvider provider) {
        super(provider);
        changebuf = ByteBuffer.allocateDirect(io.size() * MAX_EVENTS).order(ByteOrder.nativeOrder());
        eventbuf = ByteBuffer.allocateDirect(io.size() * MAX_EVENTS).order(ByteOrder.nativeOrder());
        
        libc.pipe(pipefd);

        kqfd = libc.kqueue();
        io.putFD(changebuf, 0, pipefd[0]);
        io.putFilter(changebuf, 0, EVFILT_READ);
        io.putFlags(changebuf, 0, EV_ADD);
        libc.kevent(kqfd, changebuf, 1, null, 0, ZERO_TIMESPEC);
    }

    private static class Descriptor {
        private final int fd;
        private final Set<KQSelectionKey> keys = new HashSet<KQSelectionKey>();
        private boolean write = false, read = false;
        public Descriptor(int fd) {
            this.fd = fd;
        }
    }
    @Override
    protected void implCloseSelector() throws IOException {
        if (kqfd != -1) {
            libc.close(kqfd);
        }
        if (pipefd[0] != -1) {
            libc.close(pipefd[0]);
        }
        if (pipefd[1] != -1) {
            libc.close(pipefd[1]);
        }
        pipefd[0] = pipefd[1] = kqfd = -1;
    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        KQSelectionKey k = new KQSelectionKey(this, (NativeSelectableChannel) ch, ops);
        synchronized (regLock) {
            Descriptor d = descriptors.get(k.getFD());
            if (d == null) {
                d = new Descriptor(k.getFD());
                descriptors.put(k.getFD(), d);
            }
            d.keys.add(k);
            changed.add(d);
        }
        k.attach(att);
        return k;
    }

    @Override
    public Set<SelectionKey> keys() {
        Set<SelectionKey> keys = new HashSet<SelectionKey>();
        for (Descriptor fd : descriptors.values()) {
            keys.addAll(fd.keys);
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return Collections.unmodifiableSet(selected);
    }

    @Override
    public int selectNow() throws IOException {
        return poll(0);
    }

    @Override
    public int select(long timeout) throws IOException {
        return poll(timeout);
    }

    @Override
    public int select() throws IOException {
        return poll(-1);
    }
    private int poll(long timeout) {
        int nchanged = 0;
        //
        // Remove any cancelled keys
        //
        Set<SelectionKey> cancelled = cancelledKeys();
        synchronized (cancelled) {
            synchronized (regLock) {
                for (SelectionKey k : cancelled) {
                    KQSelectionKey kqs = (KQSelectionKey) k;
                    Descriptor d = descriptors.get(kqs.getFD());
                    deregister(kqs);
                    d.keys.remove(kqs);
                    if (d.keys.isEmpty()) {
                        io.put(changebuf, nchanged++, kqs.getFD(), EVFILT_READ, EV_DELETE);
                        io.put(changebuf, nchanged++, kqs.getFD(), EVFILT_WRITE, EV_DELETE);
                        descriptors.remove(kqs.getFD());
                        changed.remove(d);
                    }
                    if (nchanged >= MAX_EVENTS) {
                        libc.kevent(kqfd, changebuf, nchanged, null, 0, ZERO_TIMESPEC);
                        nchanged = 0;
                    }
                }
            }
            cancelled.clear();
        }
        
        synchronized (regLock) {
            for (Descriptor d : changed) {
                int writers = 0, readers = 0;
                for (KQSelectionKey k : d.keys) {
                    if ((k.interestOps() & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ)) != 0) {
                        ++readers;
                    }
                    if ((k.interestOps() & (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE)) != 0) {
                        ++writers;
                    }
                }
                for (Integer filt : new Integer[] { EVFILT_READ, EVFILT_WRITE }) {
                    int flags = 0;
                    //
                    // If no one is interested in events on the fd, disable it
                    //
                    if (filt == EVFILT_READ) {
                        if (readers > 0 && !d.read) {
                            flags = EV_ADD |EV_ENABLE | EV_CLEAR;
                            d.read = true;
                        } else if (readers == 0 && d.read) {
                            flags = EV_DISABLE;
                            d.read = false;
                        }
                    }
                    if (filt == EVFILT_WRITE) {
                        if (writers > 0 && !d.write) {
                            flags = EV_ADD | EV_ENABLE | EV_CLEAR;
                            d.write = true;
                        } else if (writers == 0 && d.write) {
                            flags = EV_DISABLE;
                            d.write = false;
                        }
                    }
                    System.out.printf("Updating fd %d filt=0x%x flags=0x%x\n",
                        d.fd, filt, flags);
                    if (flags != 0) {
                        io.put(changebuf, nchanged++, d.fd, filt, flags);
                    }
                    if (nchanged >= MAX_EVENTS) {
                        libc.kevent(kqfd, changebuf, nchanged, null, 0, ZERO_TIMESPEC);
                        nchanged = 0;
                    }
                }
            }
            changed.clear();
        }
        Timespec ts = null;
        if (timeout >= 0) {
            long sec = TimeUnit.MILLISECONDS.toSeconds(timeout);
            long nsec = TimeUnit.MILLISECONDS.toNanos(timeout % 1000);
            ts = new Timespec(sec, nsec);
        }
        selected.clear();
        System.out.printf("nchanged=%d\n", nchanged);
        begin();
        int n = libc.kevent(kqfd, changebuf, nchanged, eventbuf, MAX_EVENTS, ts);
        end();
        System.out.println("kevent returned " + n + " events ready");
        
        synchronized (regLock) {
            for (int i = 0; i < n; ++i) {
                int fd = io.getFD(eventbuf, i);
                Descriptor d = descriptors.get(fd);
                if (d != null) {
                    int filt = io.getFilter(eventbuf, i);
                    System.out.printf("fd=%d filt=0x%x\n", d.fd, filt);
                    for (KQSelectionKey k : d.keys) {
                        int iops = k.interestOps();
                        int ops = 0;
                        if (filt == EVFILT_READ) {
                            ops |= iops & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
                        }
                        if (filt == EVFILT_WRITE) {
                            ops |= iops & (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                        }
                        k.readyOps(ops);
                        selected.add(k);
                    }
                } else if (fd == pipefd[0]) {
                    System.out.println("Waking up");
                    wakeupReceived();
                }
            }
        }
        return n;
    }
    
    private void wakeupReceived() {
        libc.read(pipefd[0], ByteBuffer.allocate(1), 1);
    }
    
    @Override
    public Selector wakeup() {
        libc.write(pipefd[1], ByteBuffer.allocate(1), 1);
        return this;
    }

    void interestOps(KQSelectionKey k, int ops) {
        synchronized (regLock) {
            changed.add(descriptors.get(k.getFD()));
        }
    }
    private static abstract class EventIO {
        private final int eventSize, identOffset, filterOffset, flagsOffset;

        public static EventIO getInstance() {
            return Platform.getPlatform().addressSize() == 64 ? EventIO64.INSTANCE : EventIO32.INSTANCE;
        }

        public EventIO(int eventSize, int identOffset, int filterOffset, int flagsOffset) {
            this.eventSize = eventSize;
            this.identOffset = identOffset;
            this.filterOffset = filterOffset;
            this.flagsOffset = flagsOffset;
        }


        abstract void clear(ByteBuffer buf, int index);
        abstract void putFD(ByteBuffer buf, int index,int fd);
        abstract int getFD(ByteBuffer buf, int index);

        public final void put(ByteBuffer buf, int index, int fd, int filt, int flags) {
            putFD(buf, index, fd);
            putFilter(buf, index, filt);
            putFlags(buf, index, flags);
        }
        
        public final int size() {
            return eventSize;
        }

        public final void putFilter(ByteBuffer buf, int index, int filter) {
            buf.putShort(eventSize * index + filterOffset, (short) filter);
        }
        public final int getFilter(ByteBuffer buf, int index) {
            return buf.getShort(eventSize * index + filterOffset);
        }

        public final void putFlags(ByteBuffer buf, int index, int flags) {
            buf.putShort(eventSize * index + flagsOffset, (short) flags);
        }

        public final int getFlags(ByteBuffer buf, int index) {
            return buf.getShort(eventSize * index + flagsOffset);
        }
    }

    private static final class EventIO32 extends EventIO {
        public static final EventIO INSTANCE = new EventIO32();
        static final int EVENT_SIZE = 20;
        static final int IDENT_OFFSET = 0;
        static final int FILTER_OFFSET = IDENT_OFFSET + 4;
        static final int FLAGS_OFFSET = FILTER_OFFSET + 2;

        public EventIO32() {
            super(EVENT_SIZE, IDENT_OFFSET, FILTER_OFFSET, FLAGS_OFFSET);
        }


        public void clear(ByteBuffer buf, int index) {
            buf.putLong(index, 0L);
            buf.putLong(index + 8, 0L);
            buf.putInt(index + 16, 0);
        }
        public void putFD(ByteBuffer buf, int index, int fd) {
            buf.putInt(EVENT_SIZE * index + IDENT_OFFSET, fd);
        }
        public int getFD(ByteBuffer buf, int index) {
            return buf.getInt(EVENT_SIZE * index + IDENT_OFFSET);
        }
    }

    private static final class EventIO64 extends EventIO {
        public static final EventIO INSTANCE = new EventIO64();
        static final int EVENT_SIZE = 32;
        static final int IDENT_OFFSET = 0;
        static final int FILTER_OFFSET = IDENT_OFFSET + 8;
        static final int FLAGS_OFFSET = FILTER_OFFSET + 2;
        static final int FFLAGS_OFFSET = FLAGS_OFFSET + 2;
        static final int DATA_OFFSET = FFLAGS_OFFSET + 4;
        static final int UDATA_OFFSET = DATA_OFFSET + 8;

        public EventIO64() {
            super(EVENT_SIZE, IDENT_OFFSET, FILTER_OFFSET, FLAGS_OFFSET);
        }

        public void clear(ByteBuffer buf, int index) {
            for (int i = 0; i < 4; ++i) {
                buf.putLong(index + (i * 8), 0L);
            }
        }

        public void putFD(ByteBuffer buf, int index, int fd) {
            buf.putLong(EVENT_SIZE * index + IDENT_OFFSET, fd);
        }

        public int getFD(ByteBuffer buf, int index) {
            return (int) buf.getLong(EVENT_SIZE * index + IDENT_OFFSET);
        }
    }

    static class Timespec extends Struct {
        public final SignedLong tv_sec = new SignedLong();
        public final SignedLong tv_nsec = new SignedLong();

        public Timespec() {

        }

        public Timespec(long sec, long nsec) {
            tv_sec.set(sec);
            tv_nsec.set(nsec);
        }
    }

    private static interface LibC {
        
        public int kqueue();
        public int kevent(int kq, @In ByteBuffer changebuf, int nchanges,
                @Out ByteBuffer eventbuf, int nevents,
                @In @Transient Timespec timeout);
        public int pipe(@Out int[] fds);
        public int close(int fd);
        public int read(int fd, @Out ByteBuffer data, int size);
        public int write(int fd, @In ByteBuffer data, int size);
    }
}
