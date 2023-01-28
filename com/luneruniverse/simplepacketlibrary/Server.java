package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import com.luneruniverse.simplepacketlibrary.accessors.RawServerAccess;
import com.luneruniverse.simplepacketlibrary.accessors.ServerAccess;
import com.luneruniverse.simplepacketlibrary.accessors.SocketAccess;
import com.luneruniverse.simplepacketlibrary.accessors.WebServerAccess;
import com.luneruniverse.simplepacketlibrary.listeners.ConnectionListener;
import com.luneruniverse.simplepacketlibrary.listeners.ErrorHandler;
import com.luneruniverse.simplepacketlibrary.listeners.PacketListener;
import com.luneruniverse.simplepacketlibrary.listeners.WaitState;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Handles connection requests <br>
 * This is what {@link Client} connects to
 */
public class Server extends PacketRegistry implements ErrorHandler<Server> {
	
	/**
	 * Generate a {@link SSLContext} from a keystore
	 * @param keystore The keystore {@link InputStream}
	 * @param storeType The type of keystore
	 * @param storePass The password for the keystore
	 * @param keyPass The password for the key
	 * @param keyAlgorithm The key algorithm used
	 * @return The generated SSLContext
	 * @throws SSLException If an error occurs while generating the context
	 * @see #setSecure(SSLContext)
	 */
	public static SSLContext generateSSLContext(InputStream keystore, String storeType, String storePass, String keyPass, String keyAlgorithm) throws SSLException {
		try {
			KeyStore ks = KeyStore.getInstance(storeType);
			ks.load(keystore, storePass.toCharArray());
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(keyAlgorithm);
			kmf.init(ks, keyPass.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(keyAlgorithm);
			tmf.init(ks);
			
			SSLContext sslContext = null;
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			
			return sslContext;
		} catch (Exception e) {
			throw new SSLException("Unable to load SSLContext", e);
		}
	}
	
	
	
	private final int port;
	private final Queue<ConnectionListener> connectionListeners;
	private final Queue<PacketListener> packetListeners;
	private Thread thread;
	private volatile boolean connectAllowed;
	private Queue<ServerConnection> connections;
	private final List<ErrorHandler<Server>> serverErrorHandlers;
	private final List<ErrorHandler<ServerConnection>> connectionErrorHandlers;
	
	private ServerAccess socket;
	private boolean useWebSocket;
	private SSLContext ssl;
	
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
	
	/**
	 * Get the port
	 * @return port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Specify whether or not this should use a WebSocket rather than a normal Socket <br>
	 * The client must also be in the same mode <br>
	 * A WebSocket allows communication with the JavaScript variant of this library
	 * @param useWebSocket If WebSocket mode should be enabled
	 * @return this
	 * @see #isWebSocket()
	 * @see Client#useWebSocket(boolean)
	 * @see #setSecure(SSLContext)
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
	 * <strong>ONLY WORKS WHEN IN WEBSOCKET MODE</strong> <br>
	 * Allows for wss:// connections, instead of just ws://
	 * @param ssl The SSLContext, or null to disable SSL
	 * @return this
	 * @see #generateSSLContext(InputStream, String, String, String, String)
	 * @see #isSecure()
	 * @see #useWebSocket(boolean)
	 */
	public Server setSecure(SSLContext ssl) {
		this.ssl = ssl;
		return this;
	}
	
	/**
	 * <strong>ONLY WORKS WHEN IN WEBSOCKET MODE</strong> <br>
	 * This function returns as if this socket is in websocket mode <br>
	 * This checks if the last supplied SSLContext was not null
	 * @return If SSL is enabled
	 * @see #setSecure(SSLContext)
	 * @see #useWebSocket(boolean)
	 */
	public boolean isSecure() {
		return this.ssl != null;
	}
	
	/**
	 * The listener is called when a {@link Client} connects <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener The listener to add
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
	 * @param listener The listener to remove
	 * @return If the listener was registered
	 * @see #addConnectionListener(ConnectionListener)
	 */
	public boolean removeConnectionListener(ConnectionListener listener) {
		return connectionListeners.remove(listener);
	}
	
	/**
	 * The listener is called when a {@link Packet} is received <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener The listener to add
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
	 * @param listener The listener to remove
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
	 * @return this
	 * @throws IOException if an I/O error occurs when opening the socket
	 * @see #close()
	 */
	public Server start() throws IOException {
		if (isAlive())
			return this;
		
		socket = (useWebSocket ? new WebServerAccess(this, ssl) : new RawServerAccess(new ServerSocket(port)));
		thread = new Thread(() -> {
			while (true) {
				try {
					SocketAccess newSocket = socket.accept();
					if (!connectAllowed)
						newSocket.close();
					ServerConnection newConn = new ServerConnection(this, packetListeners, connectionErrorHandlers, newSocket) {
						protected void onClose() {
							connections.remove(this);
						}
					};
					connections.add(newConn);
					invokeConnectionListeners(newConn, connectionListeners);
					newConn.start(newSocket);
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
		
		return this;
	}
	private void invokeConnectionListeners(ServerConnection conn, Collection<ConnectionListener> listeners) throws InterruptedException {
		Map<Thread, WaitState> threads = new HashMap<>();
		for (ConnectionListener listener : listeners) {
			WaitState wait = new WaitState();
			Thread thread = new Thread(() -> {
				try {
					listener.onConnect(conn, wait);
				} catch (Exception e) {
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
			Thread.sleep(1);
		}
	}
	
	/**
	 * Send a {@link Packet} to all connections and call the {@link PacketListener} when a response is received <br>
	 * If there is an exception, the packet will continue being sent to the other connections before being re-thrown
	 * @param packet The packet to send
	 * @param response The listener to call on a response
	 * @param excluded ServerConnections to avoid sending the packet to
	 * @throws IOException If there was an error sending the packet
	 * @see #sendPacket(Packet, ServerConnection...)
	 */
	public void sendPacket(Packet packet, PacketListener response, ServerConnection... excluded) throws IOException {
		List<IOException> exceptions = new ArrayList<>();
		Set<ServerConnection> targets = new HashSet<>(connections);
		targets.removeAll(Arrays.asList(excluded));
		for (ServerConnection conn : targets) {
			try {
				conn.sendPacket(packet, response);
			} catch (IOException e) {
				exceptions.add(e);
			}
		}
		if (!exceptions.isEmpty()) {
			IOException e = new IOException("Error broadcasting packet");
			exceptions.forEach(e::addSuppressed);
			throw e;
		}
	}
	/**
	 * Send a {@link Packet} to all connections without a response listener <br>
	 * If there is an exception, the packet will continue being sent to the other connections before being re-thrown
	 * @param packet The packet to send
	 * @param excluded ServerConnections to avoid sending the packet to
	 * @throws IOException If there was an error sending the packet
	 * @see #sendPacket(Packet, PacketListener, ServerConnection...)
	 */
	public void sendPacket(Packet packet, ServerConnection... excluded) throws IOException {
		sendPacket(packet, null, excluded);
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
	 * @param handler The handler to add
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
	 * @param handler The handler to remove
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
	 * Calling this twice will cause the handler to be called twice <br>
	 * The error handler is automatically added to already active connections
	 * @param handler The handler to add
	 * @return this
	 * @see #removeConnectionErrorHandler(ErrorHandler)
	 * @see #addServerErrorHandler(ErrorHandler)
	 */
	public Server addConnectionErrorHandler(ErrorHandler<ServerConnection> handler) {
		connectionErrorHandlers.add(handler);
		for (ServerConnection conn : connections)
			conn.addErrorHandler(handler);
		return this;
	}
	/**
	 * This is specifically for {@link ServerConnection} errors, NOT {@link Server} errors <br>
	 * <br>
	 * The handler will stop being called <br>
	 * If {@link #addConnectionErrorHandler(ErrorHandler)} was called twice, it will still be called once <br>
	 * The error handler is automatically removed from already active connections
	 * @param handler The handler to remove
	 * @return If the handler was registered, including in any of the connections
	 * @see #addConnectionErrorHandler(ErrorHandler)
	 * @see #removeServerErrorHandler(ErrorHandler)
	 */
	public boolean removeConnectionErrorHandler(ErrorHandler<ServerConnection> handler) {
		boolean output = connectionErrorHandlers.remove(handler);
		for (ServerConnection conn : connections)
			output |= conn.removeErrorHandler(handler);
		return output;
	}
	
	@Override
	public void onError(Exception e, Server obj, Error error) {
		if (serverErrorHandlers.isEmpty()) {
			System.err.println("Server error: " + error);
			e.printStackTrace();
		} else {
			for (ErrorHandler<Server> errorHandler : serverErrorHandlers)
				errorHandler.onError(e, obj, error);
		}
	}
	
}
