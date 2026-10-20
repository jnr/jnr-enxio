package org.jnr.enxio.win32.bindings;

import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;
import jnr.ffi.annotations.Out;
import jnr.ffi.annotations.StdCall;
import jnr.ffi.annotations.Transient;
import jnr.ffi.byref.IntByReference;
import jnr.posix.HANDLE;
import jnr.posix.WindowsSecurityAttributes;

public interface WindowsLibC{

    public static final int FILE_TYPE_DISK = 0x0001;
    public static final int FILE_TYPE_CHAR = 0x0002;
    public static final int FILE_TYPE_PIPE = 0x0003;
    public static final int FILE_TYPE_REMOTE = 0x8000;
    public static final int FILE_TYPE_UNKNOWN = 0x0000;

    public static final int PROCESS_QUERY_INFORMATION = 0x0400;

    public static final int FPIPE = 0x08;
    public static final int FNOINTERIT = 0x10;
    public static final int FOPEN = 0x01;

    public int _open_osfhandle(HANDLE handle, int flags);
    public HANDLE _get_osfhandle(int fd);
    public int _close(int fd);
    public int _getpid();

    @StdCall
    HANDLE CreateFileW(
            byte[] lpFileName,
            int dwDesiredAccess,
            int dwShareMode,
            Pointer lpSecurityAttributes,
            int dwCreationDisposition,
            int dwFlagsAndAttributes,
            int hTemplateFile
    );
    
    @StdCall
    public HANDLE CreateNamedPipeA(
            @In String oldname,
            @In int dwOpenMode,
            @In int dwPipeMode,
            @In int dwOpenMaxInstances,
            @In int nOutBufferSize,
            @In int nInBufferSize,
            @In int nDefaultTimeOut,
            @In WindowsSecurityAttributes lpSecurityAttributes);

    @StdCall
    boolean PeekNamedPipe(
    		@In HANDLE handle,
    		@Out @Transient byte[] lpBuffer,
    		@In int nBufferSize,
    		@Out IntByReference lpBytesRead,
    		@Out IntByReference lpTotalBytesAvail,
    		@Out IntByReference lpBytesLeftThisMessage);
    

    @StdCall
    boolean SetNamedPipeHandleState(
    		@In HANDLE hNamedPipe,
    		@In IntByReference lpMode,
    		@In IntByReference lpMaxCollectionCount,
    		@In IntByReference lpCollectDataTimeout);
    
    @StdCall
    boolean CancelIoEx(@In HANDLE hFile, @In Pointer lpOverlapped);
    
    @StdCall
    boolean CloseHandle(HANDLE handle);
    
    @StdCall
    int WaitForSingleObject(HANDLE handle, int milliseconds);

}
