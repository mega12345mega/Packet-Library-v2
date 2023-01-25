package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.listeners.ErrorHandler;

public class ClientWebSocketAccess extends WebSocketClient implements SocketAccess {
	
	private final Client client;
	private final Queue<PacketData> packets;
	
	public ClientWebSocketAccess(Client client, String ip, int port, int timeout, SSLContext ssl) throws URISyntaxException, IOException {
		super(new URI(ip + ":" + port));
		this.client = client;
		this.packets = new ConcurrentLinkedQueue<>();
		if (ssl != null)
			setSocketFactory(ssl.getSocketFactory());
		try {
			if (!this.connectBlocking(timeout, TimeUnit.MILLISECONDS))
				throw new IOException("Unable to connect to server");
		} catch (InterruptedException e) {
			throw new IOException("Unable to connect to server", e);
		}
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		
	}
	
	@Override
	public void onMessage(String message) {
		onMessage(ByteBuffer.wrap(message.getBytes()));
	}
	
	@Override
	public void onMessage(ByteBuffer buf) {
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		try {
			packets.add(readPacket(new DataInputStream(new ByteArrayInputStream(data))));
		} catch (IOException e) {
			client.onError(e, client, ErrorHandler.Error.READING_WEBSOCKET_PACKET);
		}
	}
	
	@Override
	public void onError(Exception e) {
		client.onError(e, client, ErrorHandler.Error.GENERIC_WEBSOCKET);
	}
	
	@Override
	public void onClose(int code, String reason, boolean remote) {
		packets.clear();
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
	
	@Override
	public void sendPacket(byte[] data) throws IOException {
		send(data);
	}
	
}
