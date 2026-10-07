package com.test.localjnrtest;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicLong;

import jnr.enxio.channels.NativeDeviceChannel;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.WindowsPOSIX;

/**
 * Hello world!
 */
public class App
{
	public static void main(String[] args)
	{
		System.out.println("Hello World!");
	}


}
