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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jnr.enxio.win32.WrappedW32Handle.Channel;
import org.jnr.enxio.win32.WrappedW32Handle.WrappedType;

import jnr.constants.platform.LastError;
import jnr.enxio.channels.Native;
import jnr.enxio.channels.NativeSelectableChannel;
import jnr.ffi.byref.IntByReference;
import jnr.posix.HANDLE;
import jnr.posix.WindowsPOSIX;

/**
 * C (MSVCRT) fd-based Pipe SourceChanel (read), polling selected
 */
public class C_PipeSourcePolledChannel extends Pipe.SourceChannel implements NativeSelectableChannel, WrappedW32Handle.Channel {

    private final int fd;
	private HANDLE handle;

    public C_PipeSourcePolledChannel(SelectorProvider provider, HANDLE handle, int fd) {
        super(provider);
        this.fd = fd;
        this.handle = handle;
    }

    @Override
    public int getFD() // FIXME: should this be one method or two?
    {
    	return fd;
    }
    @Override
    public int getCfd()
    {
    	return fd;
    }
    @Override
    public HANDLE getWin32Handle()
    {
    	return handle;
    }
    @Override
    public WrappedType getFdType()
    {
    	return WrappedType.WRAPPED_HANDLE;
    }
    
    private PipeSelectorProvider win32Provider()
	{
		return (PipeSelectorProvider)this.provider();
	}
    
    AtomicBoolean needsCancel = new AtomicBoolean(false);
    
    @Override
    protected void implCloseSelectableChannel() throws IOException {
    	if (needsCancel.get())// avoid a read + close hang
    		if (!win32Provider().getWinLibC().CancelIoEx(handle, null))
    		{
          	  	var ws = WindowsPOSIX.mapErrorToErrno(win32Provider().getWinPosix().errno());
          	  	System.err.println("cwx = " + ws + " n = ");
          	  LastError val =  LastError.valueOf(win32Provider().getWinPosix().errno());
          	  System.err.println("Last error was " + val + " || " + win32Provider().getWinPosix().errno());
    			throw new IOException(Native.getLastErrorString());
    		}
    	System.err.println("Closingww " + fd);
        int n = Native.close(fd);
    	System.err.println("Closedww " + fd);
        if (n < 0) {
            throw new IOException(Native.getLastErrorString());
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
//        Native.setBlocking(fd, block);
    	IntByReference mode = new IntByReference(block ? WindowsPOSIX.PIPE_WAIT : WindowsPOSIX.PIPE_NOWAIT);
    	if (!win32Provider().getWinLibC().SetNamedPipeHandleState(handle, mode, null, null))
    	{
    		new RuntimeException("it bad"); // TODO: fixme
    	}
    	
    }
@Override
    public int read(ByteBuffer dst) throws IOException
    {
    if (!isOpen())
        throw new ClosedChannelException();
	needsCancel.set(true);
	int n ;
	try
	{
  n = Native.read(fd, dst);
	}
	finally
	{
		needsCancel.set(false);
	}
  switch (n) {
      case 0:
          return -1; // EOF

      	// TODO: if async close, close?

      case -1:
    	  var ws = WindowsPOSIX.mapErrorToErrno(win32Provider().getWinPosix().errno());
          switch (ws) {
              case EAGAIN:
              case EWOULDBLOCK:
              case EPIPE:
                  return 0;
              case EINTR:
                  throw new ClosedChannelException();

              default:
            	  System.err.println("ws = " + ws.name());
            	  //ERROR_NO_DATA
            	  LastError val =  LastError.valueOf(win32Provider().getWinPosix().errno());
            	  System.err.println("Last error was " + val + " || " + win32Provider().getWinPosix().errno());
            	  throw new IOException(val.toString());
//                  throw new IOException(Native.getLastErrorString());
          }

      default:
          return n;
  }
    }
@Override
public long read(ByteBuffer[] dsts) throws IOException
{
	return read(dsts, 0, dsts.length);
}
@Override
public long read(ByteBuffer[] dsts, int offset, int length) throws IOException
{
    Objects.checkFromIndexSize(offset, length, dsts.length);
    long written = 0;
    for (int i = 0; i < length; i++)
        written += (long)read(dsts[offset + i]);
   return written;
}

}
