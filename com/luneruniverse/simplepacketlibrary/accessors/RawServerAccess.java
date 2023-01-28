package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * The implementation for the raw socket mode for the server
 */
public class RawServerAccess implements ServerAccess {
	
	private final ServerSocket socket;
	
	/**
	 * Internal use only <br>
	 * Create a raw server
	 * @param socket The internal server
	 */
	public RawServerAccess(ServerSocket socket) {
		this.socket = socket;
	}
	
	@Override
	public SocketAccess accept() throws IOException {
		return new RawSocketAccess(socket.accept());
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}
