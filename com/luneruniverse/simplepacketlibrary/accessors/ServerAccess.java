package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.Closeable;
import java.io.IOException;

/**
 * The interface for the different server modes
 */
public interface ServerAccess extends Closeable {
	/**
	 * Receive a new connection
	 * @return The new connection
	 * @throws IOException If there was an exception receiving a connection
	 * @throws InterruptedException If the thread was interrupted
	 */
	public SocketAccess accept() throws IOException, InterruptedException;
}
