package org.jnr.enxio.win32.bindings;

import jnr.ffi.LastError;
import jnr.posix.HANDLE;
import jnr.posix.WindowsSecurityAttributes;
import jnr.posix.util.WindowsHelpers;

public class WindowsFunctions
{    
	

    private static final int GENERIC_READ = 0x80000000;
    private static final int GENERIC_WRITE = 0x40000000;
    private static final int OPEN_EXISTING = 3;
    
    
    // pipe dwOpenMode
    public static final int PIPE_ACCESS_DUPLEX = 0x0003;
    public static final int PIPE_ACCESS_INBOUND = 0x0001;
    public static final int PIPE_ACCESS_OUTBOUND = 0x0002;
    public static final int FILE_FLAG_FIRST_PIPE_INSTANCE = 0x00080000;
    public static final int FILE_FLAG_WRITE_THROUGH = 0x80000000;
    public static final int FILE_FLAG_OVERLAPPED = 0x40000000;
    public static final int WRITE_DAC = 0x00040000;
    public static final int WRITE_OWNER = 0x00080000;
    public static final int ACCESS_SYSTEM_SECURITY = 0x01000000;

    // pipe dwPipeMode
    public static final int PIPE_TYPE_BYTE = 0x00000000;
    public static final int PIPE_TYPE_MESSAGE = 0x00000004;
    public static final int PIPE_READMODE_BYTE = 0x00000000;
    public static final int PIPE_READMODE_MESSAGE = 0x00000002;
    public static final int PIPE_WAIT = 0x00000000;
    public static final int PIPE_NOWAIT = 0x00000001;
    public static final int PIPE_ACCEPT_REMOTE_CLIENTS = 0x00000000;
    public static final int PIPE_REJECT_REMOTE_CLIENTS = 0x00000008;

    public static final int PIPE_UNLIMITED_INSTANCES = 255;
    
    public WindowsLibC wlibc()
	{
		return POSIXFactory.getWin32();
	}
    jnr.ffi.Runtime getRuntime() {
        return jnr.ffi.Runtime.getRuntime(wlibc());
    }

    public int errno() {
        return LastError.getLastError(getRuntime());
    }
    

    
	 public int named_w32_pipe(String pipename, int[] fds) {
	        System.out.println("Starting Named");
	        WindowsSecurityAttributes secAttributes = new WindowsSecurityAttributes(getRuntime());
	        // Note: CRuby has inheritHandle = false, the oppsite of the default secAttributes

	        // Open the "server" side (read)
	        // AsciiString astr = new AsciiString(pipename.length());
	        // astr.set(pipename);
	        HANDLE handle_read = wlibc().CreateNamedPipeA(
	            pipename,
	            PIPE_ACCESS_DUPLEX | FILE_FLAG_OVERLAPPED, 
	            PIPE_WAIT, 2,
	            65536, 65536, 0, 
	            secAttributes);
	        if (!handle_read.isValid())
	        {
	            int error = errno();
	        System.out.println("ERRNO Named read " + error);
//	            handler.error(mapErrorToErrno(error), "pipe", pipename);
	            return -1;
	        }
//	        if (fds[0] == 682)
//	        {
//	            System.out.println("fastskip Named======");
//	            int rd = wlibc()._open_osfhandle(handle_read, 0);/wlibc().FOPEN | wlibc().FNOINTERIT | wlibc().FPIPE);
//	            
//	            System.out.println("ret Named");
//	            fds[0] = rd;
//	            fds[1] = -1;
//	            return 0;
//	        }
	        System.out.println("second Named");
	        // Open the "client" side (write)
	        HANDLE handle_write = wlibc().CreateFileW(
	            WindowsHelpers.toWPath(pipename),
	            GENERIC_READ | GENERIC_WRITE, 0, 
	            WindowsSecurityAttributes.getMemory(secAttributes),
	            OPEN_EXISTING, FILE_FLAG_OVERLAPPED, 0);
	        if (!handle_write.isValid())
	        {
	            int error = errno();
	            System.out.println("ERRNO Second named read " + error);
//	            handler.error(mapErrorToErrno(error), "pipe", pipename);
	            wlibc().CloseHandle(handle_read);
	            return -1;
	        }
	        System.out.println("naooubg Named");
	        int rd = wlibc()._open_osfhandle(handle_read, 0);//wlibc().RD //wlibc().FOPEN | wlibc().FNOINTERIT | wlibc().FPIPE);
	        int wr = wlibc()._open_osfhandle(handle_write, 0);//wlibc().FOPEN | wlibc().FNOINTERIT | wlibc().FPIPE);
	        
	        System.out.println("ret Named");
	        fds[0] = rd;
	        fds[1] = wr;
	        return 0;
	    }
}
