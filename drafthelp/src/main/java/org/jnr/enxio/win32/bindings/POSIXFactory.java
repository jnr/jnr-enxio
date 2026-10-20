package org.jnr.enxio.win32.bindings;

import jnr.constants.Constant;
import jnr.ffi.LibraryLoader;
import jnr.ffi.LibraryOption;

import java.util.Collections;
import java.util.HashMap;

import jnr.ffi.mapper.FromNativeContext;
import jnr.ffi.mapper.FromNativeConverter;
import jnr.ffi.mapper.FunctionMapper;
import jnr.ffi.mapper.ToNativeContext;
import jnr.ffi.mapper.ToNativeConverter;
import jnr.ffi.mapper.TypeMapper;
import jnr.posix.BaseNativePOSIX;
import jnr.posix.FileStat;
import jnr.posix.HANDLE;
import jnr.posix.MsgHdr;
import jnr.posix.NativeTimes;
import jnr.posix.SimpleFunctionMapper;
import jnr.posix.WString;

import java.util.Map;

public class POSIXFactory {

    public static WindowsLibC getWin32()
    {
    return DefaultLibCProvider.INSTANCE.getLibC();
}
    		
    private static String[] libraries() {
                return new String[] { "msvcrt", "kernel32" };
    }

    private static FunctionMapper functionMapper() {
                return new SimpleFunctionMapper.Builder()
                        .map("getpid", "_getpid")
                        .map("chmod", "_chmod")
                        .map("fstat", "_fstat64")
                        .map("stat", "_stat64")
                        .map("umask", "_umask")
                        .map("isatty", "_isatty")
                        .map("read", "_read")
                        .map("write", "_write")
                        .map("close", "_close")
                        .map("getcwd", "_getcwd")
                        .map("unlink", "_unlink")
                        .map("access", "_access")
                        .map("open", "_open")
                        .map("dup", "_dup")
                        .map("dup2", "_dup2")
                        .map("lseek", "_lseek")
                        .map("ftruncate", "_chsize")
                        .build();
    }
    static final class POSIXTypeMapper implements TypeMapper {
        public static final TypeMapper INSTANCE = new POSIXTypeMapper();
        
        private POSIXTypeMapper() {}
        
        public FromNativeConverter getFromNativeConverter(Class klazz) {
        	if (HANDLE.class.isAssignableFrom(klazz)) {
                return HANDLE.Converter;
            }
            
            return null;
        }
        
        public ToNativeConverter getToNativeConverter(Class klazz) {
            if (FileStat.class.isAssignableFrom(klazz)) {
                return BaseNativePOSIX.FileStatConverter;

            } else if (NativeTimes.class.isAssignableFrom(klazz)) {
                return BaseNativePOSIX.TimesConverter;

            } else if (Constant.class.isAssignableFrom(klazz)) {
                return BaseNativePOSIX.ConstantConverter;

            } else if (WString.class.isAssignableFrom(klazz)) {
                return WString.Converter;

            } else if (HANDLE.class.isAssignableFrom(klazz)) {
                return HANDLE.Converter;
            } else if (MsgHdr.class.isAssignableFrom(klazz)) {
                return BaseNativePOSIX.MsgHdrConverter;
            }

            return null;
        }

        public final ToNativeConverter getToNativeConverter(Class klazz, ToNativeContext context) {
            return getToNativeConverter(klazz);
        }

        public final FromNativeConverter getFromNativeConverter(Class klazz, FromNativeContext context) {
            return getFromNativeConverter(klazz);
        }
    }

    
    private static Map<LibraryOption, Object> options() {
        Map<LibraryOption, Object> options = new HashMap<LibraryOption, Object>();
//        
//        FunctionMapper functionMapper = functionMapper();
//        if (functionMapper != null) {
//            options.put(LibraryOption.FunctionMapper, functionMapper);
//        }

        options.put(LibraryOption.TypeMapper, POSIXTypeMapper.INSTANCE);
        options.put(LibraryOption.LoadNow, Boolean.TRUE);
        
        return Collections.unmodifiableMap(options);
    }

    private static final class DefaultLibCProvider  {
        public static final DefaultLibCProvider INSTANCE = new DefaultLibCProvider();

        private static final class SingletonHolder {
            public static WindowsLibC libc;
            static {
                LibraryLoader<? extends WindowsLibC> libcLoader = LibraryLoader.create(WindowsLibC.class);

                // always do a default search
                libcLoader.searchDefault();

                for (String library : libraries()) {
                    libcLoader.library(library);
                }

                for (Map.Entry<LibraryOption, Object> entry : options().entrySet()) {
                    libcLoader.option(entry.getKey(), entry.getValue());
                }

                libcLoader.failImmediately();

                libc = libcLoader.load();


            }
        }

        public final WindowsLibC getLibC() {
            return SingletonHolder.libc;
        }

    }
}
