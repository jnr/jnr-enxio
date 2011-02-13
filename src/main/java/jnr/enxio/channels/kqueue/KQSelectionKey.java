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


import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectionKey;
import jnr.enxio.channels.NativeSelectableChannel;

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
