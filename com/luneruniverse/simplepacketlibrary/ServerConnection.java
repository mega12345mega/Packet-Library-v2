package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * The server side connection itself <br>
 * Created when a {@link Client} connects to a {@link Server}
 */
public class ServerConnection extends Connection {
	
	private final Server server;
	private final List<ErrorHandler<ServerConnection>> errorHandlers;
	
	ServerConnection(Server server, Queue<ConnectionListener> connectionListeners, Queue<PacketListener> packetListeners, BasicSocket socket) throws InterruptedException {
		super(packetListeners);
		this.server = server;
		this.errorHandlers = new ArrayList<>();
		this.socket = socket;
		if (this.socket instanceof StandardWebSocketServerConnection)
			((StandardWebSocketServerConnection) this.socket).setConnection(this);
		this.registerPackets(server);
		invokeConnectionListeners(connectionListeners);
		start(socket);
	}
	private void invokeConnectionListeners(Collection<ConnectionListener> listeners) throws InterruptedException {
		Map<Thread, WaitState> threads = new HashMap<>();
		for (ConnectionListener listener : listeners) {
			WaitState wait = new WaitState();
			Thread thread = new Thread(() -> {
				try {
					listener.onConnect(this, wait);
				} catch (IOException e) {
					onError(e, this, ErrorHandler.Error.INSIDE_CONNECTION_LISTENER);
				}
			}, "Connection Listener");
			thread.start();
			threads.put(thread, wait);
		}
		while (!threads.isEmpty()) {
			if (Thread.interrupted())
				throw new InterruptedException();
			threads.entrySet().removeIf(entry -> !(entry.getKey().isAlive() && entry.getValue().isWaiting()));
			Thread.yield();
		}
	}
	
	/**
	 * @return The server that the client connected to
	 */
	public Server getServer() {
		return server;
	}
	
	
	/**
	 * The handler is called when an error occurs <br>
	 * Calling this twice will cause the handler to be called twice
	 * @param handler
	 * @return this
	 * @see #removeErrorHandler(ErrorHandler)
	 */
	public ServerConnection addErrorHandler(ErrorHandler<ServerConnection> handler) {
		errorHandlers.add(handler);
		return this;
	}
	/**
	 * The handler will stop being called <br>
	 * If {@link #addErrorHandler(ErrorHandler)} was called twice, it will still be called once
	 * @param handler
	 * @return If the handler was registered
	 * @see #addErrorHandler(ErrorHandler)
	 */
	public boolean removeErrorHandler(ErrorHandler<ServerConnection> handler) {
		return errorHandlers.remove(handler);
	}
	
	@Override
	public void onError(Exception e, Connection obj, Error error) {
		for (ErrorHandler<ServerConnection> errorHandler : errorHandlers)
			errorHandler.onError(e, (ServerConnection) obj, error);
	}
	
}
