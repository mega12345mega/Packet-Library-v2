package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.Closeable;
import java.io.IOException;

public interface ServerAccess extends Closeable {
	public SocketAccess accept() throws IOException, InterruptedException;
}
