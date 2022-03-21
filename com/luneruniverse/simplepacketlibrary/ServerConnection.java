package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * The server side connection itself <br>
 * Created when a {@link Client} connects to a {@link Server}
 */
public class ServerConnection extends Connection {
	
	private final Server server;
	
	ServerConnection(Server server, Queue<ConnectionListener> connectionListeners, Queue<PacketListener> packetListeners, Socket socket) throws InterruptedException {
		super(packetListeners);
		this.server = server;
		this.socket = socket;
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
					e.printStackTrace();
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
	
}
