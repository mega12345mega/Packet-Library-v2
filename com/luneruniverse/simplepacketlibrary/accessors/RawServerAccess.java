package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.IOException;
import java.net.ServerSocket;

public class RawServerAccess implements ServerAccess {
	
	private final ServerSocket socket;
	
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
