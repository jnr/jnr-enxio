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

package org.jnr.enxio.win32;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicLong;

import org.jnr.enxio.win32.bindings.POSIXFactory;
import org.jnr.enxio.win32.bindings.WindowsFunctions;
import org.jnr.enxio.win32.bindings.WindowsLibC;

public final class PipeSelectorProvider extends SelectorProvider
{
	private static final class SingletonHolder
	{
		static PipeSelectorProvider INSTANCE = new PipeSelectorProvider();
	}

	public static final PipeSelectorProvider getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static final class Win32SingletonHolder
	{
		static WindowsFunctions INSTANCE = new WindowsFunctions();
		// (WindowsPOSIX) POSIXFactory.getNativePOSIX();
	}

	private PipeCreationMode mode = PipeCreationMode.NAMED_MSVCRT_FD_POLLING;
	private static AtomicLong pipeNum = new AtomicLong();

	public WindowsFunctions getWinPosix()
	{
		return Win32SingletonHolder.INSTANCE;
	}

	public WindowsLibC getWinLibC()
	{
		return POSIXFactory.getWin32();
	}

	public void setMode(PipeCreationMode mode)
	{
		this.mode = mode;
	}

	public PipeCreationMode getMode()
	{
		return mode;
	}

	@Override
	public DatagramChannel openDatagramChannel() throws IOException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public DatagramChannel openDatagramChannel(java.net.ProtocolFamily family) throws IOException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Pipe openPipe() throws IOException
	{
		return openPipe(String.format("\\\\.\\pipe\\jnr-%x-%x", ProcessHandle.current().pid(), pipeNum.incrementAndGet()));
	}

	public Pipe openPipe(String name) throws IOException
	{
		return new NativePipe(this, name);
	}

	@Override
	public AbstractSelector openSelector() throws IOException
	{
		return new PipePollSelector(this);
	}

	@Override
	public ServerSocketChannel openServerSocketChannel() throws IOException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public SocketChannel openSocketChannel() throws IOException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public static enum PipeCreationMode
	{
		/**
		 * Pipe uses MSVCRT read() + write() and polling for read selection.
		 * IO.select([],[write],[]) is always available
		 */
		NAMED_MSVCRT_FD_POLLING,
		/**
		 * Pipe uses Win32 ReadFile() + WriteFile() and WaitForObjects for read+write
		 * selection. Not yet implemented
		 */
		NAMED_HANDLE_IO,
	}

}
