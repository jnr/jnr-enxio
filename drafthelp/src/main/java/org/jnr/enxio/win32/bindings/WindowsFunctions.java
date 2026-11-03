package org.jnr.enxio.win32.bindings;

import static jnr.constants.platform.Errno.E2BIG;
import static jnr.constants.platform.Errno.EACCES;
import static jnr.constants.platform.Errno.EAGAIN;
import static jnr.constants.platform.Errno.EBADF;
import static jnr.constants.platform.Errno.ECHILD;
import static jnr.constants.platform.Errno.EEXIST;
import static jnr.constants.platform.Errno.EFAULT;
import static jnr.constants.platform.Errno.EINTR;
import static jnr.constants.platform.Errno.EINVAL;
import static jnr.constants.platform.Errno.EIO;
import static jnr.constants.platform.Errno.EMFILE;
import static jnr.constants.platform.Errno.ENAMETOOLONG;
import static jnr.constants.platform.Errno.ENODEV;
import static jnr.constants.platform.Errno.ENOENT;
import static jnr.constants.platform.Errno.ENOEXEC;
import static jnr.constants.platform.Errno.ENOMEM;
import static jnr.constants.platform.Errno.ENOSPC;
import static jnr.constants.platform.Errno.ENOTDIR;
import static jnr.constants.platform.Errno.ENOTEMPTY;
import static jnr.constants.platform.Errno.ENXIO;
import static jnr.constants.platform.Errno.EPIPE;
import static jnr.constants.platform.Errno.EROFS;
import static jnr.constants.platform.Errno.EXDEV;
import static jnr.constants.platform.Errno.__UNKNOWN_CONSTANT__;
import static jnr.constants.platform.windows.LastError.ERROR_ACCESS_DENIED;
import static jnr.constants.platform.windows.LastError.ERROR_ALREADY_EXISTS;
import static jnr.constants.platform.windows.LastError.ERROR_ARENA_TRASHED;
import static jnr.constants.platform.windows.LastError.ERROR_AUTODATASEG_EXCEEDS_64k;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_COMMAND;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_ENVIRONMENT;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_EXE_FORMAT;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_FORMAT;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_LENGTH;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_NETPATH;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_NET_NAME;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_PATHNAME;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_PIPE;
import static jnr.constants.platform.windows.LastError.ERROR_BAD_UNIT;
import static jnr.constants.platform.windows.LastError.ERROR_BROKEN_PIPE;
import static jnr.constants.platform.windows.LastError.ERROR_CANNOT_MAKE;
import static jnr.constants.platform.windows.LastError.ERROR_CHILD_NOT_COMPLETE;
import static jnr.constants.platform.windows.LastError.ERROR_CRC;
import static jnr.constants.platform.windows.LastError.ERROR_CURRENT_DIRECTORY;
import static jnr.constants.platform.windows.LastError.ERROR_DIRECTORY;
import static jnr.constants.platform.windows.LastError.ERROR_DIRECT_ACCESS_HANDLE;
import static jnr.constants.platform.windows.LastError.ERROR_DIR_NOT_EMPTY;
import static jnr.constants.platform.windows.LastError.ERROR_DISK_FULL;
import static jnr.constants.platform.windows.LastError.ERROR_DRIVE_LOCKED;
import static jnr.constants.platform.windows.LastError.ERROR_DYNLINK_FROM_INVALID_RING;
import static jnr.constants.platform.windows.LastError.ERROR_EXE_MARKED_INVALID;
import static jnr.constants.platform.windows.LastError.ERROR_FAIL_I24;
import static jnr.constants.platform.windows.LastError.ERROR_FILENAME_EXCED_RANGE;
import static jnr.constants.platform.windows.LastError.ERROR_FILE_EXISTS;
import static jnr.constants.platform.windows.LastError.ERROR_FILE_NOT_FOUND;
import static jnr.constants.platform.windows.LastError.ERROR_GEN_FAILURE;
import static jnr.constants.platform.windows.LastError.ERROR_INFLOOP_IN_RELOC_CHAIN;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_ACCESS;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_BLOCK;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_DATA;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_DRIVE;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_EXE_SIGNATURE;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_FUNCTION;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_HANDLE;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_MINALLOCSIZE;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_MODULETYPE;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_PARAMETER;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_SEGDPL;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_STACKSEG;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_STARTING_CODESEG;
import static jnr.constants.platform.windows.LastError.ERROR_INVALID_TARGET_HANDLE;
import static jnr.constants.platform.windows.LastError.ERROR_IOPL_NOT_ENABLED;
import static jnr.constants.platform.windows.LastError.ERROR_ITERATED_DATA_EXCEEDS_64k;
import static jnr.constants.platform.windows.LastError.ERROR_LOCK_FAILED;
import static jnr.constants.platform.windows.LastError.ERROR_LOCK_VIOLATION;
import static jnr.constants.platform.windows.LastError.ERROR_MAX_THRDS_REACHED;
import static jnr.constants.platform.windows.LastError.ERROR_MOD_NOT_FOUND;
import static jnr.constants.platform.windows.LastError.ERROR_NEGATIVE_SEEK;
import static jnr.constants.platform.windows.LastError.ERROR_NESTING_NOT_ALLOWED;
import static jnr.constants.platform.windows.LastError.ERROR_NETWORK_ACCESS_DENIED;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_DOS_DISK;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_ENOUGH_MEMORY;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_ENOUGH_QUOTA;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_LOCKED;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_READY;
import static jnr.constants.platform.windows.LastError.ERROR_NOT_SAME_DEVICE;
import static jnr.constants.platform.windows.LastError.ERROR_NO_DATA;
import static jnr.constants.platform.windows.LastError.ERROR_NO_MORE_FILES;
import static jnr.constants.platform.windows.LastError.ERROR_NO_PROC_SLOTS;
import static jnr.constants.platform.windows.LastError.ERROR_OPERATION_ABORTED;
import static jnr.constants.platform.windows.LastError.ERROR_OUT_OF_PAPER;
import static jnr.constants.platform.windows.LastError.ERROR_PATH_NOT_FOUND;
import static jnr.constants.platform.windows.LastError.ERROR_PIPE_BUSY;
import static jnr.constants.platform.windows.LastError.ERROR_PIPE_NOT_CONNECTED;
import static jnr.constants.platform.windows.LastError.ERROR_READ_FAULT;
import static jnr.constants.platform.windows.LastError.ERROR_RELOC_CHAIN_XEEDS_SEGLIM;
import static jnr.constants.platform.windows.LastError.ERROR_RING2SEG_MUST_BE_MOVABLE;
import static jnr.constants.platform.windows.LastError.ERROR_SECTOR_NOT_FOUND;
import static jnr.constants.platform.windows.LastError.ERROR_SEEK;
import static jnr.constants.platform.windows.LastError.ERROR_SEEK_ON_DEVICE;
import static jnr.constants.platform.windows.LastError.ERROR_SHARING_BUFFER_EXCEEDED;
import static jnr.constants.platform.windows.LastError.ERROR_SHARING_VIOLATION;
import static jnr.constants.platform.windows.LastError.ERROR_TOO_MANY_OPEN_FILES;
import static jnr.constants.platform.windows.LastError.ERROR_WAIT_NO_CHILDREN;
import static jnr.constants.platform.windows.LastError.ERROR_WRITE_FAULT;
import static jnr.constants.platform.windows.LastError.ERROR_WRITE_PROTECT;
import static jnr.constants.platform.windows.LastError.ERROR_WRONG_DISK;
import static jnr.constants.platform.windows.LastError.WSAEACCES;
import static jnr.constants.platform.windows.LastError.WSAEBADF;
import static jnr.constants.platform.windows.LastError.WSAEFAULT;
import static jnr.constants.platform.windows.LastError.WSAEINTR;
import static jnr.constants.platform.windows.LastError.WSAEINVAL;
import static jnr.constants.platform.windows.LastError.WSAEMFILE;
import static jnr.constants.platform.windows.LastError.WSAENAMETOOLONG;
import static jnr.constants.platform.windows.LastError.WSAENOTEMPTY;

import java.util.HashMap;
import java.util.Map;

import jnr.constants.platform.Errno;
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
	    private final static Map<Integer, Errno> errorToErrnoMapper
        = new HashMap<Integer, Errno>();

static {
    errorToErrnoMapper.put(ERROR_INVALID_FUNCTION.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_FILE_NOT_FOUND.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_PATH_NOT_FOUND.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_TOO_MANY_OPEN_FILES.value(), EMFILE);
    errorToErrnoMapper.put(ERROR_ACCESS_DENIED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_INVALID_HANDLE.value(), EBADF );
    errorToErrnoMapper.put(ERROR_ARENA_TRASHED.value(), ENOMEM);
    errorToErrnoMapper.put(ERROR_NOT_ENOUGH_MEMORY.value(), ENOMEM);
    errorToErrnoMapper.put(ERROR_INVALID_BLOCK.value(), ENOMEM);
    errorToErrnoMapper.put(ERROR_BAD_ENVIRONMENT.value(), E2BIG );
    errorToErrnoMapper.put(ERROR_BAD_FORMAT.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_ACCESS.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_INVALID_DATA.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_INVALID_DRIVE.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_CURRENT_DIRECTORY.value(), EACCES);
    errorToErrnoMapper.put(ERROR_NOT_SAME_DEVICE.value(), EXDEV );
    errorToErrnoMapper.put(ERROR_NO_MORE_FILES.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_WRITE_PROTECT.value(), EROFS );
    errorToErrnoMapper.put(ERROR_BAD_UNIT.value(), ENODEV);
    errorToErrnoMapper.put(ERROR_NOT_READY.value(), ENXIO );
    errorToErrnoMapper.put(ERROR_BAD_COMMAND.value(), EACCES);
    errorToErrnoMapper.put(ERROR_CRC.value(), EACCES);
    errorToErrnoMapper.put(ERROR_BAD_LENGTH.value(), EACCES);
    errorToErrnoMapper.put(ERROR_SEEK.value(), EIO);
    errorToErrnoMapper.put(ERROR_NOT_DOS_DISK.value(), EACCES);
    errorToErrnoMapper.put(ERROR_SECTOR_NOT_FOUND.value(), EACCES);
    errorToErrnoMapper.put(ERROR_OUT_OF_PAPER.value(), EACCES);
    errorToErrnoMapper.put(ERROR_WRITE_FAULT.value(), EIO);
    errorToErrnoMapper.put(ERROR_READ_FAULT.value(), EIO);
    errorToErrnoMapper.put(ERROR_GEN_FAILURE.value(), EACCES);
    errorToErrnoMapper.put(ERROR_LOCK_VIOLATION.value(), EACCES);
    errorToErrnoMapper.put(ERROR_SHARING_VIOLATION.value(), EACCES);
    errorToErrnoMapper.put(ERROR_WRONG_DISK.value(), EACCES);
    errorToErrnoMapper.put(ERROR_SHARING_BUFFER_EXCEEDED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_BAD_NETPATH.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_NETWORK_ACCESS_DENIED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_BAD_NET_NAME.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_FILE_EXISTS.value(), EEXIST);
    errorToErrnoMapper.put(ERROR_CANNOT_MAKE.value(), EACCES);
    errorToErrnoMapper.put(ERROR_FAIL_I24.value(), EACCES);
    errorToErrnoMapper.put(ERROR_INVALID_PARAMETER.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_NO_PROC_SLOTS.value(), EAGAIN);
    errorToErrnoMapper.put(ERROR_DRIVE_LOCKED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_BROKEN_PIPE.value(), EPIPE);
    errorToErrnoMapper.put(ERROR_DISK_FULL.value(), ENOSPC);
    errorToErrnoMapper.put(ERROR_INVALID_TARGET_HANDLE.value(), EBADF);
    errorToErrnoMapper.put(ERROR_INVALID_HANDLE.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_WAIT_NO_CHILDREN.value(), ECHILD);
    errorToErrnoMapper.put(ERROR_CHILD_NOT_COMPLETE.value(), ECHILD);
    errorToErrnoMapper.put(ERROR_DIRECT_ACCESS_HANDLE.value(), EBADF);
    errorToErrnoMapper.put(ERROR_NEGATIVE_SEEK.value(), EINVAL);
    errorToErrnoMapper.put(ERROR_SEEK_ON_DEVICE.value(), EACCES);
    errorToErrnoMapper.put(ERROR_DIR_NOT_EMPTY.value(), ENOTEMPTY);
    errorToErrnoMapper.put(ERROR_DIRECTORY.value(), ENOTDIR);
    errorToErrnoMapper.put(ERROR_NOT_LOCKED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_BAD_PATHNAME.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_MAX_THRDS_REACHED.value(), EAGAIN);
    errorToErrnoMapper.put(ERROR_LOCK_FAILED.value(), EACCES);
    errorToErrnoMapper.put(ERROR_ALREADY_EXISTS.value(), EEXIST);
    errorToErrnoMapper.put(ERROR_INVALID_STARTING_CODESEG.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_STACKSEG.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_MODULETYPE.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_EXE_SIGNATURE.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_EXE_MARKED_INVALID.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_BAD_EXE_FORMAT.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_ITERATED_DATA_EXCEEDS_64k.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_MINALLOCSIZE.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_DYNLINK_FROM_INVALID_RING.value(),ENOEXEC);
    errorToErrnoMapper.put(ERROR_IOPL_NOT_ENABLED.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INVALID_SEGDPL.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_AUTODATASEG_EXCEEDS_64k.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_RING2SEG_MUST_BE_MOVABLE.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_RELOC_CHAIN_XEEDS_SEGLIM.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_INFLOOP_IN_RELOC_CHAIN.value(), ENOEXEC);
    errorToErrnoMapper.put(ERROR_FILENAME_EXCED_RANGE.value(), ENOENT);
    errorToErrnoMapper.put(ERROR_NESTING_NOT_ALLOWED.value(), EAGAIN);
    // ERROR_PIPE_LOCAL (in MRI)
    errorToErrnoMapper.put(229, EPIPE);
    errorToErrnoMapper.put(ERROR_BAD_PIPE.value(), EPIPE);
    errorToErrnoMapper.put(ERROR_PIPE_BUSY.value(), EAGAIN);
    errorToErrnoMapper.put(ERROR_NO_DATA.value(), EPIPE);
    errorToErrnoMapper.put(ERROR_PIPE_NOT_CONNECTED.value(), EPIPE);
    errorToErrnoMapper.put(ERROR_OPERATION_ABORTED.value(), EINTR);
    errorToErrnoMapper.put(ERROR_NOT_ENOUGH_QUOTA.value(), ENOMEM);
    errorToErrnoMapper.put(ERROR_MOD_NOT_FOUND.value(), ENOENT);
    errorToErrnoMapper.put(WSAENAMETOOLONG.value(), ENAMETOOLONG);
    errorToErrnoMapper.put(WSAENOTEMPTY.value(), ENOTEMPTY);
    errorToErrnoMapper.put(WSAEINTR.value(), EINTR);
    errorToErrnoMapper.put(WSAEBADF.value(), EBADF);
    errorToErrnoMapper.put(WSAEACCES.value(), EACCES);
    errorToErrnoMapper.put(WSAEFAULT.value(), EFAULT);
    errorToErrnoMapper.put(WSAEINVAL.value(), EINVAL);
    errorToErrnoMapper.put(WSAEMFILE.value(), EMFILE);
}

	 

	    public static Errno mapErrorToErrno(int error) {
	        Errno errno = errorToErrnoMapper.get(error);
	        if (errno == null) {
	            errno = __UNKNOWN_CONSTANT__;
	        }
	        return errno;
	    }

}
