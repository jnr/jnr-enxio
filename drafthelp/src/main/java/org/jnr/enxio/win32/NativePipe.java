package org.jnr.enxio.win32;

import java.io.IOException;
import java.nio.channels.Pipe;

/**
 * Native, Win32 C Runtime fd-based pipes
 */
public class NativePipe extends Pipe
{
	// read and write channels
	private final SourceChannel source;
	private final SinkChannel sink;

	@Override
	public SourceChannel source()
	{
		return source;
	}

	@Override
	public SinkChannel sink()
	{
		return sink;
	}


	NativePipe(PipeSelectorProvider provider, String path)
	{
		// TODO: close the handles!
		int[] pipes = new int[2];
		int result = provider.getWinPosix().named_w32_pipe(path, pipes);
		if (result != 0)
		{
			throw new RuntimeException("PipeErro"); // TODO: error handling
		}
		source = new C_PipeSourcePolledChannel(provider, provider.getWinLibC()._get_osfhandle(pipes[0]), pipes[0]);
		sink = new C_PipeSinkTrueChannel(provider, provider.getWinLibC()._get_osfhandle(pipes[1]), pipes[1]);
	}
	
	public static Pipe open() throws IOException
	{
		return PipeSelectorProvider.getInstance().openPipe();
	}

	public static Pipe openNamed(String name) throws IOException
	{
		return PipeSelectorProvider.getInstance().openPipe(name);
	}
}
