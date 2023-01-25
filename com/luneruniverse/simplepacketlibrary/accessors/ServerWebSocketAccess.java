package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.WebSocket;

import com.luneruniverse.simplepacketlibrary.ServerConnection;

public class ServerWebSocketAccess implements SocketAccess {
	
	private ServerConnection connection;
	private final WebSocket socket;
	private final Queue<PacketData> packets;
	
	public ServerWebSocketAccess(WebSocket socket) {
		this.socket = socket;
		this.packets = new ConcurrentLinkedQueue<>();
	}
	
	public void setConnection(ServerConnection connection) {
		this.connection = connection;
	}
	public ServerConnection getConnection() {
		return connection;
	}
	
	@Override
	public PacketData readPacket() throws IOException, InterruptedException {
		while (packets.isEmpty()) {
			if (isClosed())
				throw new EOFException();
			Thread.sleep(1);
		}
		return packets.remove();
	}
	void packetReceived(ByteBuffer message) throws IOException {
		byte[] data = new byte[message.remaining()];
		message.get(data);
		packets.add(readPacket(new DataInputStream(new ByteArrayInputStream(data))));
	}
	
	@Override
	public void sendPacket(byte[] data) throws IOException {
		socket.send(data);
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
