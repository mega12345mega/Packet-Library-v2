package com.luneruniverse.simplepacketlibrary;

import java.io.Closeable;
import java.io.IOException;

interface BasicServer extends Closeable {
	public BasicSocket accept() throws IOException, InterruptedException;
}
