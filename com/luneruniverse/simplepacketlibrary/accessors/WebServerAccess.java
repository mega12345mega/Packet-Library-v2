package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLContext;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.ServerConnection;
import com.luneruniverse.simplepacketlibrary.listeners.ErrorHandler;

public class WebServerAccess extends WebSocketServer implements ServerAccess {
	
	private final Server server;
	private final Map<WebSocket, ServerWebSocketAccess> connections;
	private final Queue<ServerWebSocketAccess> connectionsQueue;
	
	public WebServerAccess(Server server, SSLContext ssl) {
		super(new InetSocketAddress(server.getPort()));
		this.server = server;
		this.connections = new WeakHashMap<>();
		this.connectionsQueue = new ConcurrentLinkedQueue<>();
		if (ssl != null)
			setWebSocketFactory(new DefaultSSLWebSocketServerFactory(ssl));
		this.start();
	}
	
	@Override
	public void onStart() {
		
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		ServerWebSocketAccess connection = new ServerWebSocketAccess(conn);
		connections.put(conn, connection);
		connectionsQueue.add(connection);
	}
	
	@Override
	public void onMessage(WebSocket conn, String message) {
		onMessage(conn, ByteBuffer.wrap(message.getBytes()));
	}
	
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		try {
			connections.get(conn).packetReceived(message);
		} catch (IOException e) {
			ServerConnection connection = connections.get(conn).getConnection();
			if (connection != null)
				connection.onError(e, connection, ErrorHandler.Error.READING_WEBSOCKET_PACKET);
		}
	}
	
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		connectionsQueue.remove(connections.remove(conn));
	}
	
	@Override
	public void onError(WebSocket conn, Exception e) {
		if (conn == null)
			server.onError(e, server, ErrorHandler.Error.GENERIC_WEBSOCKET);
		else {
			ServerConnection connection = connections.get(conn).getConnection();
			if (connection != null)
				connection.onError(e, connection, ErrorHandler.Error.GENERIC_WEBSOCKET);
		}
	}
	
	@Override
	public SocketAccess accept() throws IOException, InterruptedException {
		while (connectionsQueue.isEmpty()) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				close();
				throw e;
			}
		}
		return connectionsQueue.remove();
	}
	
	@Override
	public void close() throws IOException {
		try {
			this.stop();
		} catch (InterruptedException e) {
			throw new IOException("Error stopping server", e);
		}
	}
	
}
