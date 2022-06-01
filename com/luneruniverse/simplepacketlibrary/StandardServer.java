package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.net.ServerSocket;

class StandardServer implements BasicServer {
	
	private final ServerSocket socket;
	
	public StandardServer(ServerSocket socket) {
		this.socket = socket;
	}
	
	@Override
	public BasicSocket accept() throws IOException {
		return new StandardSocket(socket.accept());
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}
