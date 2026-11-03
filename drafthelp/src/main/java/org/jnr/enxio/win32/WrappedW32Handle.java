package org.jnr.enxio.win32;


import jnr.posix.HANDLE;

/**
 * Interface for Native Win32 IO (Handle and/or FD based)
 */
public interface WrappedW32Handle
{
	int getCfd();
	HANDLE getWin32Handle();
	default boolean hasWin32Handle() {
		return getFdType() != WrappedType.FD_ONLY;
	}
	default boolean hasCfd() {
		WrappedType type = getFdType();
		return (type == WrappedType.FD_ONLY || type == WrappedType.WRAPPED_HANDLE);
	}
	WrappedType getFdType();
	
	static enum WrappedType
	{
		HANDLE_ONLY, // do not wrap
		UNWRAPPED_HANDLE, // you can wrap with msvc, but it's not done yet
		WRAPPED_HANDLE, // msvcrt-wrapped handle. Do not dispose of the handle any more
		FD_ONLY, // msvcrt or fake fd. Handle is not valid
	}
	
	static interface Channel extends WrappedW32Handle, java.nio.channels.Channel {
		
	}
}
