
package org.jnr.enxio.win32;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jnr.ffi.byref.IntByReference;
import jnr.posix.HANDLE;

/**
 * An implementation of a {@link java.nio.channels.Selector} that uses Win32
 * Pipe polling. Writing is always unblocked, incorrectly, but matching ruby
 */
class PipePollSelector extends AbstractSelector
{
//    private final jnr.ffi.Runtime runtime = NativeRuntime.getSystemRuntime();

	private final Object regLock = new Object();
	private final Set<C_PipeSelectionKey> keys = new LinkedHashSet<C_PipeSelectionKey>();
	private final Set<SelectionKey> publicKeys = Collections.unmodifiableSet(keys);
	private final Set<SelectionKey> selected = new LinkedHashSet<SelectionKey>();

	public PipePollSelector(PipeSelectorProvider provider)
	{
		super(provider);
	}

	private PipeSelectorProvider win32Provider()
	{
		return (PipeSelectorProvider) this.provider();
	}

	@Override
	protected void implCloseSelector() throws IOException
	{
		wakeup();
		selected.clear();
	}

	@Override
	protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att)
	{
		if (ch instanceof C_PipeSinkTrueChannel)
		{
			// Always true
			var key = new C_PipeSelectionKey((C_PipeSinkTrueChannel) ch, ops, SelectionKey.OP_WRITE, false);
			synchronized (regLock)
			{
				key.attach(att);
				key.interestOps(ops);
				keys.add(key);
			}
			return key;
		} else if (ch instanceof C_PipeSourcePolledChannel)
		{
			var key = new C_PipeSelectionKey((C_PipeSourcePolledChannel) ch, ops, SelectionKey.OP_READ,
					(ops & SelectionKey.OP_READ) != 0);
			synchronized (regLock)
			{
				key.attach(att);
				key.interestOps(ops);
				keys.add(key);
			}
			return key;
		} else
		{
			throw new RuntimeException("Not impl");// FIXME: wrong error
		}
	}

	boolean pipeHasBytesAvailable(HANDLE pipe)
	{
		IntByReference available = new IntByReference();
		boolean success = win32Provider().getWinLibC().PeekNamedPipe(pipe, null, 0, null, available, null);
		if (!success)
		{
			throw new RuntimeException("Pipe peek error");
		}
		return available.getValue() > 0;
	}

	Semaphore selectWakeup = new Semaphore(0);

	@Override
	public int select(long timeout) throws IOException
	{
		System.out.println("Doing! ------");
		// forEachRead => peekNamedPipe
		if (selectWakeup.drainPermits() > 0)
			timeout = 0;
		

		long startTime = System.nanoTime(); // Monotonic clock
		long now = startTime;
		long expirationTime = startTime + (timeout * 1000000);
		if (expirationTime < startTime && timeout > 0)
			throw new IllegalArgumentException("Timeout is too large");
		int keychanges = 0;

		handleCancelledKeys();
		var keyCopy = getLiveKeys();
		while (keychanges == 0 && (now <= expirationTime || timeout < 0) && keyCopy.size() > 0)
		{
			for (var key : keyCopy)
			{
				// for all always-true items, or if the pipe is ready
				if (!key.isActuallyPoll() || pipeHasBytesAvailable(key.getCh().getWin32Handle()))
				{
					int dir = !key.isActuallyPoll() ? key.supportedOps() : SelectionKey.OP_READ;
					int newops = (key.interestOps() & dir);
					synchronized (selected)
					{
						if (selected.contains(key))
						{
							key.readyOps(key.readyOps() | newops);
						} else
						{
							key.readyOps(newops);
							selected.add(key);
						}
					}
					keychanges++;
				}
			}
			try
			{
				if (timeout == 0 || selectWakeup.tryAcquire(10, TimeUnit.MILLISECONDS))
				{
					System.err.println("acquired");
					return keychanges;// WAKEUP!
				}
			} catch (InterruptedException e)
			{
				System.err.println("Interrupted");
				return keychanges;// supresssed
			}
			keyCopy = getLiveKeys();
		}
		System.out.println("DONE ------");
		return keychanges;

	}
	
	private HashSet<C_PipeSelectionKey> getLiveKeys()
	{
        synchronized (regLock) {
        	return new HashSet<>(keys);
        }
	}
	

    private int handleCancelledKeys() {
        Set<SelectionKey> cancelled = cancelledKeys();
        synchronized (cancelled) {
            int nchanged = 0;
            synchronized (regLock) {
                for (SelectionKey k : cancelled) {
                	C_PipeSelectionKey kqs = (C_PipeSelectionKey) k;
                    deregister(kqs);
                    synchronized (selected) {
                        selected.remove(kqs);
                    }
                    keys.remove(kqs);
                }
            }
            cancelled.clear();
            return nchanged;
        }
        
    }


	@Override
	public int select() throws IOException
	{
		return select(-1);
	}

	@Override
	public int selectNow() throws IOException
	{
		return select(0);
	}

	@Override
	public Selector wakeup()
	{
		selectWakeup.release();
		return this;
	}

	public final class C_PipeSelectionKey extends AbstractSelectionKey
	{
		private WrappedW32Handle.Channel ch;
		private int interestOps;
		private int readyOps = 0;
		private boolean actuallyPoll;
		private int supportedOps;

		C_PipeSelectionKey(WrappedW32Handle.Channel ch, int ops, int supportedOps, boolean actuallyPoll)
		{
			this.ch = ch;
			interestOps = ops;
			this.actuallyPoll = actuallyPoll;
			this.supportedOps = supportedOps;
			if (!actuallyPoll)
				readyOps = supportedOps;// TODO: hard coded
		}

		public int supportedOps()
		{
			return supportedOps;
		}

		public boolean isActuallyPoll()
		{
			return actuallyPoll;
		}

		@Override
		public SelectableChannel channel()
		{
			return (SelectableChannel) ch;
		}

		public WrappedW32Handle.Channel getCh()
		{
			return ch;
		}

		@Override
		public Selector selector()
		{
			return PipePollSelector.this;
		}

		@Override
		public int interestOps()
		{
			return interestOps;
		}

		@Override
		public SelectionKey interestOps(int ops)
		{
			interestOps = ops;
			return this;
		}

		@Override
		public int readyOps()
		{
			return readyOps;
		}

		void readyOps(int readyOps)
		{
			this.readyOps = readyOps;
		}

	}

	private void ensureOpen()
	{
		if (!isOpen())
			throw new ClosedSelectorException();
	}

	@Override
	public Set<SelectionKey> keys()
	{
		ensureOpen();
		return publicKeys;
	}

	@Override
	public Set<SelectionKey> selectedKeys()
	{
		ensureOpen();
		return selected;
	}

}