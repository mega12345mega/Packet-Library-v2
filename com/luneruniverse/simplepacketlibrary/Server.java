package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Handles connection requests <br>
 * This is what {@link Client} connects to
 */
public class Server extends PacketRegistry {
	
	private final int port;
	private final Queue<ConnectionListener> connectionListeners;
	private final Queue<PacketListener> packetListeners;
	private Thread thread;
	private volatile boolean connectAllowed;
	private ServerSocket socket;
	private Queue<ServerConnection> connections;
	
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
	}
	
	/**
	 * The listener is called when a {@link Client} connects <br>
	 * Calling this twice will cause the listener to be called twice
	 * @param listener
	 * @see #removeConnectionListener(ConnectionListener)
	 * @see #setConnectAllowed(boolean)
	 */
	public void addConnectionListener(ConnectionListener listener) {
		connectionListeners.add(listener);
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
	 * @see #removePacketListener(PacketListener)
	 */
	public void addPacketListener(PacketListener listener) {
		packetListeners.add(listener);
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
	 * @see #isConnectAllowed()
	 */
	public void setConnectAllowed(boolean connectAllowed) {
		this.connectAllowed = connectAllowed;
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
		
		socket = new ServerSocket(port);
		thread = new Thread(() -> {
			while (true) {
				try {
					Socket newSocket = socket.accept();
					if (!connectAllowed)
						socket.close();
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
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, "Server [" + port + "]");
		thread.start();
	}
	
	/**
	 * Stops accepting connections and closes the current ones <br>
	 * Will block until the internal thread exits <br>
	 * You can start the server after it has been stopped
	 * @throws InterruptedException If this thread is interrupted before the internal thread exits
	 * @throws IOException If an I/O error occurs when closing the socket
	 * @see #start()
	 */
	public void close() throws InterruptedException, IOException {
		thread.interrupt();
		for (ServerConnection connection : connections)
			connection.close();
		socket.close();
		thread.join();
		thread = null;
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
	
}
