package com.luneruniverse.simplepacketlibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.luneruniverse.simplepacketlibrary.accessors.SocketAccess;
import com.luneruniverse.simplepacketlibrary.listeners.ErrorHandler;
import com.luneruniverse.simplepacketlibrary.listeners.PacketListener;

/**
 * The server side connection itself <br>
 * Created when a {@link Client} connects to a {@link Server}
 */
public class ServerConnection extends Connection {
	
	private final Server server;
	private final List<ErrorHandler<ServerConnection>> errorHandlers;
	
	ServerConnection(Server server, Queue<PacketListener> packetListeners, List<ErrorHandler<ServerConnection>> errorHandlers, SocketAccess socket) throws InterruptedException {
		super(packetListeners);
		this.server = server;
		this.errorHandlers = new ArrayList<>(errorHandlers);
		this.socket = socket;
		this.socket.setConnection(this);
		this.registerPackets(server);
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
		if (errorHandlers.isEmpty()) {
			System.err.println("Server connection error: " + error);
			e.printStackTrace();
		} else {
			for (ErrorHandler<ServerConnection> errorHandler : errorHandlers)
				errorHandler.onError(e, (ServerConnection) obj, error);
		}
	}
	
}
