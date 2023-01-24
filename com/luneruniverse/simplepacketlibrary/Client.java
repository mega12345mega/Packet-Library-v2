package com.luneruniverse.simplepacketlibrary;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.SSLContext;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Connects to a {@link Server}
 */
public class Client extends Connection {
	
	private final String ip;
	private final int port;
	private final List<ErrorHandler<Client>> errorHandlers;
	
	private int connectTimeout;
	private boolean useWebSocket;
	private SSLContext ssl;
	
	/**
	 * Create a client <br>
	 * Will not start the client <br>
	 * Will not check if the ip or port is open
	 * @param ip The ip to connect to
	 * @param port The port to connect to
	 * @see #start()
	 */
	public Client(String ip, int port) {
		super(new ConcurrentLinkedQueue<>());
		this.ip = ip;
		this.port = port;
		this.errorHandlers = new ArrayList<>();
		this.connectTimeout = 5000;
		useWebSocket(false);
	}
	/**
	 * Create a client <br>
	 * Will not start the client <br>
	 * Will not check if the port is open <br>
	 * Connects to localhost
	 * @param port The port to connect to
	 * @see #start()
	 */
	public Client(int port) throws UnknownHostException, IOException {
		this(null, port);
	}
	
	/**
	 * Get the ip
	 * @return ip
	 */
	public String getIp() {
		return ip;
	}
	/**
	 * Get the port
	 * @return port
	 */
	public int getPort() {
		return port;
	}
	
	/**
	 * Set the connect timeout, which defaults to 5000 (or 5 seconds)
	 * @param connectTimeout The timeout, or 0 if {@link #start()} should block indefinitely
	 * @see #getConnectTimeout()
	 * @see #setTimeout(int)
	 */
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	/**
	 * Get the connect timeout
	 * @return The timeout, or 0 if {@link #start()} will block indefinitely
	 * @see #setConnectTimeout(int)
	 * @see #getTimeout()
	 */
	public int getConnectTimeout() {
		return connectTimeout;
	}
	
	/**
	 * Specify whether or not this should use a WebSocket rather than a normal Socket <br>
	 * The server must also be in the same mode <br>
	 * A WebSocket allows communication with the JavaScript variant of this library
	 * @param useWebSocket
	 * @return this
	 * @see #isWebSocket()
	 * @see Server#useWebSocket(boolean)
	 */
	public Client useWebSocket(boolean useWebSocket) {
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
	 * @see #generateSSLContext(File, String, String, String, String)
	 * @see #isSecure()
	 * @see #useWebSocket(boolean)
	 */
	public Client setSecure(SSLContext ssl) {
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
	 * The listener is called when a {@link Packet} is received <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener
	 * @return this
	 * @see #removePacketListener(PacketListener)
	 */
	public Client addPacketListener(PacketListener listener) {
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
	 * Connect the client <br>
	 * @return this
	 * @throws UnknownHostException If the IP address of the host could not be determined
	 * @throws IOException If an I/O error occurs when creating the socket
	 */
	public Client start() throws UnknownHostException, IOException {
		if (isAlive())
			return this;
		
		String ip = (this.ip == null ? "localhost" : this.ip);
		try {
			if (useWebSocket)
				start(new StandardWebSocketClient(this, ip, port, connectTimeout, ssl));
			else {
				Socket socket = new Socket();
				socket.connect(new InetSocketAddress(ip, port), connectTimeout);
				start(new StandardSocket(socket));
			}
			return this;
		} catch (URISyntaxException e) {
			throw new UnknownHostException(e.getInput());
		}
	}
	
	/**
	 * You can start a client after it has been stopped
	 * @return If the client is connected
	 * @see #start()
	 * @see #close()
	 */
	@Override
	public boolean isAlive() {
		return super.isAlive();
	}
	
	
	/**
	 * The handler is called when an error occurs <br>
	 * Calling this twice will cause the handler to be called twice
	 * @param handler
	 * @return this
	 * @see #removeErrorHandler(ErrorHandler)
	 */
	public Client addErrorHandler(ErrorHandler<Client> handler) {
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
	public boolean removeErrorHandler(ErrorHandler<Client> handler) {
		return errorHandlers.remove(handler);
	}
	
	@Override
	public void onError(Exception e, Connection obj, Error error) {
		if (errorHandlers.isEmpty()) {
			System.err.println("Client error: " + error);
			e.printStackTrace();
		} else {
			for (ErrorHandler<Client> errorHandler : errorHandlers)
				errorHandler.onError(e, (Client) obj, error);
		}
	}
	
}
