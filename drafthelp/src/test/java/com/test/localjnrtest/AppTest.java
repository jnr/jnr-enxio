package com.test.localjnrtest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

import org.jnr.enxio.win32.NativePipe;
import org.jnr.enxio.win32.PipeSelectorProvider;
import org.jnr.enxio.win32.WrappedW32Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for simple App.
 */
public class AppTest {
	Logger log = null;
	
	@BeforeEach
	public void before(TestInfo test) throws IOException
	{
		log = LoggerFactory.getLogger(test.getDisplayName());
//		var selector = (NativeWin32_PipePollSelector)NativeWin32PipeSelectorProvider.getInstance().openSelector();
//		selector.debug(log);
		log.info(" ==== Starting test {} =====", test.getDisplayName());
	}

//	// SOcket sizes
//	int FILLING_SIZE = 409600;
//	int HALF_SIZE= 40960;
//	boolean WORKING_SELECT_WRITE=true;
	
	// PIPE sizes
	int FILLING_SIZE = 63500;
	int HALF_SIZE= 36500;
	boolean WORKING_SELECT_WRITE=false;
	

	Pipe pipeOpen() throws IOException
	{
		//return Pipe.open();
		return NativePipe.open();
	}
	

	
    @Test
    public void testBlockingEmpty() throws Throwable
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] result = new int[] {-2};
    			assertThat(tstamp(() -> {
    				assertFalse(tryRead(source, result,  buffer, Duration.ofSeconds(1)));
    			}), greaterThan(1000l));
    			
    		}
    	}
    }

    @Test
    public void testNonBlockingEmpty() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] result = new int[] {-2};
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, result,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(0, result[0]);
    			
    		}
    	}
    }

    @Test
    public void testSelectorEmpty() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] results = new int[] {-2};
    			int[] resultr = new int[] {-2};
    			
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(0, resultr[0]);
    			resultr[0] = -3;

    			assertTimeout(Duration.ofMillis(1500), () -> {
    				Thread t = trySelect(selector, results);
    				assertFalse(t.join(Duration.ofSeconds(1)));
    				t.interrupt();
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertEquals(0, results[0]);

    			sourceKey.cancel();
    		}
    	}
    }
    @Test
    public void testSelectorEmpty2() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] results = new int[] {-2};

    			assertTimeout(Duration.ofMillis(1500), () -> {
    				Thread t = trySelect(selector, results);
    				assertFalse(t.join(Duration.ofSeconds(1)));
    				selector.wakeup();
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertEquals(0, results[0]);

    			sourceKey.cancel();
    		}
    	}
    }
    @Test
    public void testSelectorEmpty3() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] results = new int[] {-2};
    			int[] resultr = new int[] {-2};

    			assertTimeout(Duration.ofMillis(500), () -> {
    				selector.wakeup();
    				Thread t = trySelect(selector, results);
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertEquals(0, results[0]);

    			sourceKey.cancel();

				System.err.println("ENFO F");
    		}
    	}
    }

    @Test
    public void testBlockingFull() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			byte[] padding = new byte[4096];
    			Arrays.fill(padding, (byte)0x30);
    			var buffer = ByteBuffer.wrap(padding);
    			int total = 0;
//    			for (int i = 0; i < 4096; i++)
//    			{
	    			int[] result = new int[] {-2};
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertEquals(4096, result[0]);

	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertEquals(0, result[0]);
	    			
//    			}
    			
    		}
    	}
    }

    @Test
    public void testNonBlockingFull() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			byte[] padding = new byte[4096];
    			Arrays.fill(padding, (byte)0x30);
    			var buffer = ByteBuffer.wrap(padding);
    			int total = 0;
//    			for (int i = 0; i < 4096; i++)
//    			{
	    			int[] result = new int[] {-2};
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertEquals(4096, result[0]);

	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertEquals(0, result[0]);
	    			
//    			}
    			
    		}
    	}
    }
    
    @Test
    public void testSelectorFull() throws IOException, InterruptedException
    {
    	assumeTrue(WORKING_SELECT_WRITE);
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
//    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			var sinkKey = sink.register(selector, SelectionKey.OP_WRITE);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			byte[] padding = new byte[FILLING_SIZE];
    			Arrays.fill(padding, (byte)0x30);
    			var buffer = ByteBuffer.wrap(padding);
    			int total = 0;
//    			for (int i = 0; i < 4096; i++)
//    			{
	    			int[] result = new int[] {-2};
	    			
	    			/////
//	    			tryWrite(sink, result,  buffer, Duration.ofSeconds(1));
//	    			Thread.sleep(1000000);
	    			/// /////
	    			
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertThat(result[0], greaterThan(HALF_SIZE));
	    			buffer.rewind()
;	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				assertTrue(tryWrite(sink, result,  buffer, Duration.ofSeconds(1)));
	    			});
	    			total += result[0];
	    			log.info("total write: {}", total);
	    			assertEquals(0, result[0]);
	    			
//    			}
    			int[] results = new int[] {-2};

    			assertTimeout(Duration.ofMillis(1500), () -> {
    				Thread t = trySelect(selector, results);
    				assertFalse(t.join(Duration.ofSeconds(1)));
    				selector.wakeup();
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertFalse(sinkKey.isWritable());
    			assertFalse(sinkKey.isReadable());
    			assertFalse(selector.selectedKeys().contains(sinkKey));

    			assertEquals(0, selector.selectedKeys().size());
    			assertEquals(0, results[0]);

    			assertTimeout(Duration.ofMillis(500), () -> {
    				Thread t = trySelect(selector, results);
	    			int[] resultr = new int[] {-2};
	    			var bufferR = ByteBuffer.allocate(HALF_SIZE);
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				tryRead(source, resultr, bufferR, Duration.ofSeconds(1));
	    			});
	    			assertEquals(HALF_SIZE, resultr[0]);
	    			assertEquals(bufferR.array()[0], 0x30);
	    			resultr[0] = -4;
	    			bufferR.clear();
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				tryRead(source, resultr, bufferR, Duration.ofSeconds(1));
	    			});
	    			assertEquals(HALF_SIZE, resultr[0]);
	    			assertEquals(bufferR.array()[0], 0x30);
	    			resultr[0] = -4;
	    			bufferR.clear();
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				tryRead(source, resultr, bufferR, Duration.ofSeconds(1));
	    			});
	    			assertEquals(HALF_SIZE, resultr[0]);
	    			assertEquals(bufferR.array()[0], 0x30);
	    			resultr[0] = -4;
	    			bufferR.clear();
	    			assertTimeout(Duration.ofMillis(500), () -> {
	    				tryRead(source, resultr, bufferR, Duration.ofSeconds(1));
	    			});
	    			assertEquals(HALF_SIZE, resultr[0]);
	    			assertEquals(bufferR.array()[0], 0x30);
	    			resultr[0] = -4;
	    			bufferR.clear();
	    			
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertTrue(sinkKey.isWritable());
    			assertFalse(sinkKey.isReadable());
    			assertTrue(selector.selectedKeys().contains(sinkKey));

    			assertEquals(1, selector.selectedKeys().size());
    			assertEquals(1, results[0]);
    			var ir = selector.selectedKeys().iterator();
    			ir.next();
    			ir.remove();

//    			sourceKey.cancel();
    			sinkKey.cancel();
    			
    		}
    	}
    }
    

    @Test
    public void testBlockingSimple() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] resultw = new int[] {-2};
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryWrite(sink, resultw,  u8("Write"), Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultw[0]);
//    			assertTrue(tryWrite(sink, resultw,  u8("Second"), Duration.ofSeconds(1)));
//    			assertEquals(6, resultw[0]);
//    			
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultr[0]);
    			assertEquals("Write", u8(buffer, resultr[0]));
    			
    		}
    	}
    }
    @Test
    public void testNonBlockingSimple() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] resultw = new int[] {-2};
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryWrite(sink, resultw,  u8("Write"), Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultw[0]);
//    			assertTrue(tryWrite(sink, resultw,  u8("Second"), Duration.ofSeconds(1)));
//    			assertEquals(6, resultw[0]);
//    			
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultr[0]);
    			assertEquals("Write", u8(buffer, resultr[0]));
    			
    		}
    	}
    }

    @Test
    public void testSelectorSimple() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			var sinkKey = sink.register(selector, SelectionKey.OP_WRITE);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] resultw = new int[] {-2};
    			int[] results = new int[] {-2};
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryWrite(sink, resultw,  u8("Write"), Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultw[0]);
//    			assertTrue(tryWrite(sink, resultw,  u8("Second"), Duration.ofSeconds(1)));
//    			assertEquals(6, resultw[0]);


    			assertTimeout(Duration.ofMillis(500), () -> {
    				Thread t = trySelect(selector, results);
    				assertTrue(t.join(Duration.ofSeconds(1)));
    			});
    			assertTrue(sinkKey.isWritable());
    			assertTrue(sourceKey.isReadable());
    			assertFalse(sourceKey.isWritable());
    			assertFalse(sinkKey.isReadable());
    			assertTrue(selector.selectedKeys().contains(sinkKey));
    			assertTrue(selector.selectedKeys().contains(sourceKey));

    			assertEquals(2, selector.selectedKeys().size());
    			assertEquals(2, results[0]);// FIXME: WRONG?!
//    			
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultr[0]);
    			assertEquals("Write", u8(buffer, resultr[0]));
    			
    		}
    	}
    }

    @Test
    public void testBlockingAsyncRead() throws Throwable
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] resultw = new int[] {-2};
    			assertThat(tstamp(() -> {
    				assertFalse(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			}), greaterThan(1000l));
    			assertEquals(-2, resultr[0]);
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryWrite(sink, resultw,  u8("Write"), Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultw[0]);
//    			assertTrue(tryWrite(sink, resultw,  u8("Second"), Duration.ofSeconds(1)));
//    			assertEquals(6, resultw[0]);
    			Thread.sleep(Duration.ofMillis(200));
//    			
    			assertEquals(5, resultr[0]);
    			assertEquals("Write", u8(buffer, resultr[0]));
    			
    		}
    	}
    }
    @Test
    public void testNonBlockingAsyncRead() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] resultr = new int[] {-2};
    			int[] resultw = new int[] {-2};
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(0, resultr[0]);
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryWrite(sink, resultw,  u8("Write"), Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultw[0]);
//    			assertTrue(tryWrite(sink, resultw,  u8("Second"), Duration.ofSeconds(1)));
//    			assertEquals(6, resultw[0]);
    			Thread.sleep(Duration.ofMillis(200));
//    			
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, resultr[0]);
    			assertEquals("Write", u8(buffer, resultr[0]));
    			
    		}
    	}
    }

    @Test
    public void testBlockingRead() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] result = new int[] {-2};
    			assertEquals(5, sink.write(u8("test1")));
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, result,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, result[0]);
    			assertEquals("test1", u8(buffer, result[0]));
    			
    		}
    	}
    }
    @Test
    public void testNonBlockingRead() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] result = new int[] {-2};
    			assertEquals(5, sink.write(u8("test1")));
    			assertTimeout(Duration.ofMillis(500), () -> {
    				assertTrue(tryRead(source, result,  buffer, Duration.ofSeconds(1)));
    			});
    			assertEquals(5, result[0]);
    			assertEquals("test1", u8(buffer, result[0]));
    			
    		}
    	}
    }
    
    @Test
    public void testSelectorRead() throws IOException, InterruptedException
    {
    	var pipe = pipeOpen();
    	try(var sink = pipe.sink())
    	{
    		try(var source = pipe.source())
    		{
    			source.configureBlocking(false);
    			sink.configureBlocking(false);
    			var selector = source.provider().openSelector();
    			var sourceKey = source.register(selector, SelectionKey.OP_READ);
    			
    			logstate(source, "unix-source");
    			logstate(sink, "unix-sink");
    			var buffer = ByteBuffer.allocate(16);
    			int[] results = new int[] {-2};
    			int[] resultr = new int[] {-2};
    			assertEquals(5, sink.write(u8("test1")));
    			assertTimeout(Duration.ofMillis(500), () -> {
    				Thread t = trySelect(selector, results);
    				assertTrue(t.join(Duration.ofSeconds(1)));
        			assertTrue(tryRead(source, resultr,  buffer, Duration.ofSeconds(1)));
    			});

    			assertEquals(1, results[0]);
    			
    			assertEquals(5, resultr[0]);
    			assertEquals("test1", u8(buffer, resultr[0]));
    			
    			sourceKey.cancel();
    		}
    	}
    }
    
    private String u8(ByteBuffer buff, int len)
    {
    	return new String(buff.array(), 0, len, StandardCharsets.UTF_8);
    }
    
    private ByteBuffer u8(String s)
    {
    	return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }
    
    private long tstamp(Executable r) throws Throwable
    {
    	long start = System.currentTimeMillis();
    	r.execute();
    	long end = System.currentTimeMillis();
    	return end - start;
    }

    private Thread trySelect(Selector s, int[] results) throws InterruptedException
    {
    	var t = new Thread(()->{
    		try
			{
				results[0] = s.select();
	    		log.info("Select success, wrote {} bytes", results[0]);
			} catch (IOException e)
			{

	    		log.info("Select Failure", e);
			}
    	});
    	t.setDaemon(true);
    	t.start();
//    	Thread.sleep(1000000);
    	return t;
    }
    

    private boolean tryRead(SourceChannel src, int[] results, ByteBuffer buf, Duration duration) throws InterruptedException
    {
    	var t = new Thread(()->{
    		try
			{
				results[0] = src.read(buf);
	    		log.info("Read success, wrote {} bytes", results[0]);
			} catch (IOException e)
			{

	    		log.info("Read Failure", e);
			}
    	});
    	t.setDaemon(true);
    	t.start();
    	return t.join(duration);
    }
    
    private boolean tryWrite(SinkChannel src, int[] results, ByteBuffer buf, Duration duration) throws InterruptedException
    {
    	var t = new Thread(()->{
    		try
			{
				results[0] = src.write(buf);
	    		log.info("write success, read {} bytes", results[0]);
			} catch (IOException e)
			{

	    		log.info("write Failure", e);
			}
    	});
    	t.setDaemon(true);
    	t.start();
    	return t.join(duration);
    }

//	private void logstate(AbstractSelectableChannel source, String name)
//	{
//		log.info("blocking {}? lock={}, block={}, reg={}, open={}",name, source.blockingLock(), source.isBlocking(), source.isRegistered(), source.isOpen());
//	}
	private void logstate(AbstractSelectableChannel source, String name)
	{
		var wwh = (WrappedW32Handle.Channel)source;
		log.info("blocking {}? lock={}, block={}, reg={}, open={}, fd={}, hndl={}",
				name, source.blockingLock(), source.isBlocking(), source.isRegistered(), source.isOpen()
				,wwh.getCfd(), wwh.getWin32Handle().toPointer().address());
	}
}
