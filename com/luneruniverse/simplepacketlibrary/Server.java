package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Handles connection requests <br>
 * This is what {@link Client} connects to
 */
public class Server extends PacketRegistry implements ErrorHandler<Server> {
	
	private final int port;
	private final Queue<ConnectionListener> connectionListeners;
	private final Queue<PacketListener> packetListeners;
	private Thread thread;
	private volatile boolean connectAllowed;
	private Queue<ServerConnection> connections;
	private final List<ErrorHandler<Server>> serverErrorHandlers;
	private final List<ErrorHandler<ServerConnection>> connectionErrorHandlers;
	
	private BasicServer socket;
	private boolean useWebSocket;
	
	/**
	 * Create a server <br>
	 * Will not start the server <br>
	 * Will not check if the port is open
	 * @param port The port to start the server on
	 * @see #start()
	 */
	public Server(int port) {
		this.port = port;
		this.connectionListeners = new ConcurrentLinkedQueue<>();
		this.packetListeners = new ConcurrentLinkedQueue<>();
		this.connectAllowed = true;
		this.connections = new ConcurrentLinkedQueue<>();
		this.serverErrorHandlers = new ArrayList<>();
		this.connectionErrorHandlers = new ArrayList<>();
		useWebSocket(false);
	}
	
	public int getPort() {
		return port;
	}
	
	/**
	 * Specify whether or not this should use a WebSocket rather than a normal Socket <br>
	 * The client must also be in the same mode <br>
	 * A WebSocket allows communication with the JavaScript variant of this library
	 * @param useWebSocket
	 * @return this
	 * @see #isWebSocket()
	 * @see Client#useWebSocket(boolean)
	 */
	public Server useWebSocket(boolean useWebSocket) {
		this.useWebSocket = useWebSocket;
		return this;
	}
	
	/**
	 * @return If this uses a WebSocket
	 * @see #useWebSocket(boolean)
	 */
	public boolean isWebSocket() {
		return useWebSocket;
	}
	
	/**
	 * The listener is called when a {@link Client} connects <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener
	 * @return this
	 * @see #removeConnectionListener(ConnectionListener)
	 * @see #setConnectAllowed(boolean)
	 */
	public Server addConnectionListener(ConnectionListener listener) {
		connectionListeners.add(listener);
		return this;
	}
	
	/**
	 * The listener will stop being called <br>
	 * If {@link #addConnectionListener(ConnectionListener)} was called twice, it will still be called once
	 * @param listener
	 * @return If the listener was registered
	 * @see #addConnectionListener(ConnectionListener)
	 */
	public boolean removeConnectionListener(ConnectionListener listener) {
		return connectionListeners.remove(listener);
	}
	
	/**
	 * The listener is called when a {@link Packet} is received <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener
	 * @return this
	 * @see #removePacketListener(PacketListener)
	 */
	public Server addPacketListener(PacketListener listener) {
		packetListeners.add(listener);
		return this;
	}
	
	/**
	 * The listener will stop being called <br>
	 * If {@link #addPacketListener(PacketListener)} was called twice, it will still be called once
	 * @param listener
	 * @return If the listener was registered
	 * @see #addPacketListener(PacketListener)
	 */
	public boolean removePacketListener(PacketListener listener) {
		return packetListeners.remove(listener);
	}
	
	/**
	 * Set whether or not the server will accept new connections <br>
	 * If false, connection attempts will be automatically closed <br>
	 * @param connectAllowed If connections should be accepted
	 * @return this
	 * @see #isConnectAllowed()
	 */
	public Server setConnectAllowed(boolean connectAllowed) {
		this.connectAllowed = connectAllowed;
		return this;
	}
	/**
	 * If false, connection attempts will be automatically closed <br>
	 * @return Whether or not the server will accept new connections
	 * @see #setConnectAllowed(boolean)
	 */
	public boolean isConnectAllowed() {
		return connectAllowed;
	}
	
	/**
	 * Start the server <br>
	 * Use addConnectionHandler to know when clients have connected
	 * @throws IOException if an I/O error occurs when opening the socket
	 * @see #close()
	 */
	public void start() throws IOException {
		if (isAlive())
			return;
		
		socket = (useWebSocket ? new StandardWebSocketServer(port) : new StandardServer(new ServerSocket(port)));
		thread = new Thread(() -> {
			while (true) {
				try {
					BasicSocket newSocket = socket.accept();
					if (!connectAllowed)
						newSocket.close();
					connections.add(new ServerConnection(this, connectionListeners, packetListeners, newSocket) {
						protected void onClose() {
							connections.remove(this);
						}
					});
				} catch (InterruptedException e) {
					break;
				} catch (SocketException e) {
					if (Thread.interrupted())
						break;
					onError(e, this, ErrorHandler.Error.ACCEPTING_CONNECTIONS);
				} catch (IOException e) {
					onError(e, this, ErrorHandler.Error.ACCEPTING_CONNECTIONS);
				}
			}
		}, "Server [" + port + "]");
		thread.start();
	}
	
	/**
	 * Stops accepting connections and closes the current ones <br>
	 * Will block until the internal thread exits <br>
	 * You can start the server after it has been stopped
	 * @return this
	 * @throws InterruptedException If this thread is interrupted before the internal thread exits
	 * @throws IOException If an I/O error occurs when closing the socket
	 * @see #start()
	 */
	public Server close() throws InterruptedException, IOException {
		thread.interrupt();
		for (ServerConnection connection : connections)
			connection.close();
		socket.close();
		thread.join();
		thread = null;
		return this;
	}
	
	/**
	 * You can start a server after it has been stopped
	 * @return If the server is online
	 * @see #start()
	 * @see #close()
	 */
	public boolean isAlive() {
		if (thread == null)
			return false;
		if (thread.isAlive())
			return true;
		thread = null;
		return false;
	}
	
	/**
	 * Get all the current connections <br>
	 * If called from a {@link ConnectionListener}, this may not include the new connection yet <br>
	 * The return value will automatically update for new or closed connections (it is a read-only view)
	 * @return An unmodifiable collection of the current connections
	 * @see #addConnectionListener(ConnectionListener)
	 */
	public Collection<ServerConnection> getConnections() {
		return Collections.unmodifiableCollection(connections);
	}
	
	
	/**
	 * This is specifically for {@link Server} errors, NOT {@link ServerConnection} errors <br>
	 * <br>
	 * The handler is called when an error occurs <br>
	 * Calling this twice will cause the handler to be called twice
	 * @param handler
	 * @return this
	 * @see #removeServerErrorHandler(ErrorHandler)
	 * @see #addConnectionErrorHandler(ErrorHandler)
	 */
	public Server addServerErrorHandler(ErrorHandler<Server> handler) {
		serverErrorHandlers.add(handler);
		return this;
	}
	/**
	 * This is specifically for {@link Server} errors, NOT {@link ServerConnection} errors <br>
	 * <br>
	 * The handler will stop being called <br>
	 * If {@link #addServerErrorHandler(ErrorHandler)} was called twice, it will still be called once
	 * @param handler
	 * @return If the handler was registered
	 * @see #addServerErrorHandler(ErrorHandler)
	 * @see #removeConnectionErrorHandler(ErrorHandler)
	 */
	public boolean removeServerErrorHandler(ErrorHandler<Server> handler) {
		return serverErrorHandlers.remove(handler);
	}
	
	/**
	 * This is specifically for {@link ServerConnection} errors, NOT {@link Server} errors <br>
	 * <br>
	 * The handler is called when an error occurs <br>
	 * Calling this twice will cause the handler to be called twice
	 * @param handler
	 * @return this
	 * @see #removeConnectionErrorHandler(ErrorHandler)
	 * @see #addServerErrorHandler(ErrorHandler)
	 */
	public Server addConnectionErrorHandler(ErrorHandler<ServerConnection> handler) {
		connectionErrorHandlers.add(handler);
		return this;
	}
	/**
	 * This is specifically for {@link ServerConnection} errors, NOT {@link Server} errors <br>
	 * <br>
	 * The handler will stop being called <br>
	 * If {@link #addConnectionErrorHandler(ErrorHandler)} was called twice, it will still be called once
	 * @param handler
	 * @return If the handler was registered
	 * @see #addConnectionErrorHandler(ErrorHandler)
	 * @see #removeServerErrorHandler(ErrorHandler)
	 */
	public boolean removeConnectionErrorHandler(ErrorHandler<ServerConnection> handler) {
		return connectionErrorHandlers.remove(handler);
	}
	
	@Override
	public void onError(Exception e, Server obj, Error error) {
		for (ErrorHandler<Server> errorHandler : serverErrorHandlers)
			errorHandler.onError(e, obj, error);
	}
	
}
