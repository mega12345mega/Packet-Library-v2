package com.luneruniverse.packetlibv2;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.luneruniverse.packetlibv2.packets.Packet;

/**
 * Connects to a {@link Server}
 */
public class Client extends Connection {
	
	private final String ip;
	private final int port;
	
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
	 * Connect the client <br>
	 * @throws UnknownHostException If the IP address of the host could not be determined
	 * @throws IOException If an I/O error occurs when creating the socket
	 */
	public void start() throws UnknownHostException, IOException {
		if (isAlive())
			return;
		
		start(new Socket(ip, port));
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
	
}
