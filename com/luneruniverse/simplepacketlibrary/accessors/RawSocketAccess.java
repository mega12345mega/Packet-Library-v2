package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class RawSocketAccess implements SocketAccess {
	
	private final Socket socket;
	private final DataInputStream in;
	private final OutputStream out;
	
	public RawSocketAccess(Socket socket) throws IOException {
		this.socket = socket;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = socket.getOutputStream();
	}
	
	@Override
	public PacketData readPacket() throws IOException {
		return readPacket(in);
	}
	
	@Override
	public void sendPacket(byte[] data) throws IOException {
		out.write(data);
		out.flush();
	}
	
	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}
	
	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}
