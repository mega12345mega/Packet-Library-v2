package com.luneruniverse.simplepacketlibrary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Handles sending and receiving packets
 * @see PacketListener
 */
public abstract class Connection extends PacketRegistry implements ErrorHandler<Connection> {
	
	protected final Queue<PacketListener> packetListeners;
	private volatile int timeout;
	private final Map<Integer, Map.Entry<Long, PacketListener>> responseListeners; // Waiting for a response for sent packets
	private volatile int lastPacketId;
	private final Map<Packet, Integer> responseIds; // Received a packet that may want a response
	protected BasicSocket socket;
	private Thread thread;
	
	Connection(Queue<PacketListener> packetListeners) {
		this.packetListeners = packetListeners;
		this.timeout = 5000;
		this.responseListeners = new ConcurrentHashMap<>();
		this.lastPacketId = -1;
		this.responseIds = Collections.synchronizedMap(new WeakHashMap<>());
	}
	protected void start(BasicSocket socket) {
		this.socket = socket;
		thread = new Thread(() -> {
			try {
				while (isAlive() && !Thread.interrupted()) {
					PacketData packetData = socket.readPacket();
					PacketConstructor<? extends Packet> packetType = getPacketType(packetData.packetType);
					if (packetType == null) {
						onError(new Exception("Unregistered packet type received!"), this, ErrorHandler.Error.UNREGISTERED_PACKET);
						continue;
					}
					Packet packet;
					try {
						packet = packetType.get(new DataInputStream(new ByteArrayInputStream(packetData.data)));
					} catch (Exception e) {
						onError(new Exception("The supplier for a registered received packet threw an exception", e), this, ErrorHandler.Error.CONSTRUCTING_PACKET);
						continue;
					}
					responseIds.put(packet, packetData.id);
					if (packetData.responseId != -1) {
						Map.Entry<Long, PacketListener> listener = responseListeners.get(packetData.responseId);
						if (listener != null && listener.getKey() != -1 && listener.getKey() < System.currentTimeMillis())
							responseListeners.remove(packetData.responseId);
						else if (listener != null)
							invokePacketListeners(Collections.singletonList(listener.getValue()), packet);
					} else
						invokePacketListeners(packetListeners, packet);
				}
			} catch (InterruptedException | EOFException e) {
				// Connection closed
			} catch (IOException e) {
				if (Thread.interrupted())
					return;
				onError(e, this, ErrorHandler.Error.HANDLING_PACKETS);
				try {
					close();
				} catch (IOException | InterruptedException e1) {
					onError(e1, this, ErrorHandler.Error.CLOSING_CONNECTION);
				}
			} finally {
				onClose();
			}
		}, "Connection");
		thread.start();
	}
	void invokePacketListeners(Collection<PacketListener> listeners, Packet packet) throws InterruptedException {
		Map<Thread, WaitState> threads = new HashMap<>();
		for (PacketListener listener : listeners) {
			WaitState wait = new WaitState();
			Thread thread = new Thread(() -> {
				try {
					listener.onPacket(packet, this, wait);
				} catch (IOException e) {
					onError(e, this, ErrorHandler.Error.INSIDE_PACKET_LISTENER);
				}
			}, "Packet Listener");
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
	
	private int sendPacket(Packet packet, int responseId, PacketListener response) throws IOException {
		if (socket == null || socket.isClosed())
			throw new IllegalStateException("The connection isn't alive!");
		
		cleanResponseListeners();
		
		int id = ++lastPacketId;
		
		if (response != null) {
			long packetTimeout = timeout == -1 ? -1 : System.currentTimeMillis() + timeout;
			responseListeners.put(id, new Map.Entry<Long, PacketListener>() {
				@Override
				public Long getKey() {
					return packetTimeout;
				}
				@Override
				public PacketListener getValue() {
					return response;
				}
				@Override
				public PacketListener setValue(PacketListener value) {
					throw new UnsupportedOperationException("Map entry is immutable");
				}
			});
		}
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		out.writeInt(id);
		out.writeInt(responseId);
		out.writeInt(getPacketId(packet));
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		packet.write(new DataOutputStream(data));
		out.writeInt(data.size());
		out.write(data.toByteArray());
		out.flush();
		socket.sendPacket(buf.toByteArray());
		
		return id;
	}
	/**
	 * Send a {@link Packet} and call the {@link PacketListener} when a response is received
	 * @param packet The packet to send
	 * @param response The listener to call on a response
	 * @return The packet id for canceling the response listener
	 * @throws IOException If there was an error sending the packet
	 * @see #sendPacket(Packet)
	 * @see #sendPacketWithResponse(Packet)
	 * @see #setTimeout(int)
	 * @see #removeResponseListener(int)
	 * @see #removeResponseListener(PacketListener)
	 */
	public int sendPacket(Packet packet, PacketListener response) throws IOException {
		return sendPacket(packet, -1, response);
	}
	/**
	 * Send a {@link Packet} without a response listener
	 * @param packet The packet to send
	 * @return The packet id
	 * @throws IOException If there was an error sending the packet
	 * @see #sendPacket(Packet, PacketListener)
	 */
	public int sendPacket(Packet packet) throws IOException {
		return sendPacket(packet, null);
	}
	/**
	 * Reply to a {@link Packet} and call the {@link PacketListener} when a response is received
	 * @param toReply The packet to reply to
	 * @param packet The packet to send
	 * @param response The listener to call on response
	 * @return The packet id for canceling the response listener
	 * @throws IOException If there was an error sending the packet
	 * @see #reply(Packet, Packet)
	 * @see #replyWithResponse(Packet, Packet)
	 * @see #setTimeout(int)
	 * @see #removeResponseListener(int)
	 * @see #removeResponseListener(PacketListener)
	 */
	public int reply(Packet toReply, Packet packet, PacketListener response) throws IOException {
		return sendPacket(packet, responseIds.get(toReply), response);
	}
	/**
	 * Reply to a {@link Packet} without a response listener
	 * @param toReply The packet to reply to
	 * @param packet The packet to send
	 * @return The packet id
	 * @throws IOException If there was an error sending the packet
	 * @see #reply(Packet, Packet, PacketListener)
	 */
	public int reply(Packet toReply, Packet packet) throws IOException {
		return reply(toReply, packet, null);
	}
	private Packet sendPacketWithResponse(Packet packet, int responseId) throws IOException, InterruptedException {
		AtomicReference<Packet> responseRef = new AtomicReference<>();
		int id = sendPacket(packet, responseId, (response, connection, wait) -> {
			responseRef.set(response);
		});
		while (responseRef.get() == null) {
			if (Thread.interrupted())
				throw new InterruptedException();
			Map.Entry<Long, PacketListener> timeout = responseListeners.get(id);
			if (timeout == null)
				break;
			if (timeout.getKey() != -1 && timeout.getKey() < System.currentTimeMillis()) {
				responseListeners.remove(id);
				break;
			}
			Thread.sleep(1);
		}
		return responseRef.get();
	}
	/**
	 * Send a {@link Packet} and wait for a response <br>
	 * Warning: If the timeout is -1, and no response is received, this will block indefinitely
	 * @param packet The packet to send
	 * @return The response packet, or null if the response timed out
	 * @throws IOException If there was an error sending the packet
	 * @throws InterruptedException If the thread was interrupted while waiting for a response
	 * @see #replyWithResponse(Packet, Packet)
	 * @see #sendPacket(Packet, PacketListener)
	 */
	public Packet sendPacketWithResponse(Packet packet) throws IOException, InterruptedException {
		return sendPacketWithResponse(packet, -1);
	}
	/**
	 * Reply to a {@link Packet} and wait for a response <br>
	 * Warning: If the timeout is -1, and no response is received, this will block indefinitely
	 * @param toReply The packet to reply to
	 * @param packet The packet to send
	 * @return The response packet, or null if the response timed out
	 * @throws IOException If there was an error sending the packet
	 * @throws InterruptedException If the thread was interrupted while waiting for a response
	 * @see #sendPacketWithResponse(Packet)
	 * @see #reply(Packet, Packet, PacketListener)
	 */
	public Packet replyWithResponse(Packet toReply, Packet packet) throws IOException, InterruptedException {
		return sendPacketWithResponse(packet, responseIds.get(toReply));
	}
	
	/**
	 * Set how long the response listeners will stick around <br>
	 * A very high timeout can cause a memory leak <br>
	 * A very low timeout can cause the listener to be removed before a response is received <br>
	 * You can also manually remove a response listener <br>
	 * The timeout defaults to 5000 (or 5 seconds)
	 * @param timeout The timeout in milliseconds, or -1 for no timeout
	 * @see #getTimeout()
	 * @see #removeResponseListener(int)
	 * @see #removeResponseListener(PacketListener)
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	/**
	 * How long the response listeners will stick around
	 * @return Time time in milliseconds, or -1 for no timeout
	 * @see #setTimeout(int)
	 */
	public int getTimeout() {
		return timeout;
	}
	/**
	 * Remove a response listener based on the packet
	 * @param packetId The id of the packet, from {@link #sendPacket(Packet, PacketListener)}
	 * @return If the packet had a listener registered
	 * @see #removeResponseListener(PacketListener)
	 * @see #setTimeout(int)
	 */
	public boolean removeResponseListener(int packetId) {
		return responseListeners.remove((Integer) packetId) != null;
	}
	/**
	 * Remove a response listener based on the listener itself <br>
	 * If multiple packets reference the same listener, it will be removed from all of them
	 * @param listener The listener to remove
	 * @return If the listener was registered
	 * @see #removeResponseListener(int)
	 * @see #setTimeout(int)
	 */
	public boolean removeResponseListener(PacketListener listener) {
		return responseListeners.entrySet().removeIf(entry -> entry.getValue().getValue() == listener);
	}
	/**
	 * Force the connection to clean expired response listeners <br>
	 * Automatically called when a packet is sent/replied to
	 * @see #setTimeout(int)
	 * @see #removeResponseListener(int)
	 * @see #removeResponseListener(PacketListener)
	 */
	public void cleanResponseListeners() {
		long time = System.currentTimeMillis();
		responseListeners.entrySet().removeIf(entry ->
			entry.getValue().getKey() != -1 && entry.getValue().getKey() < time);
	}
	
	/**
	 * @return If the connection is alive
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
	 * Stops accepting packets and closes the connection <br>
	 * Will block until the internal thread exits
	 * @throws InterruptedException If this thread is interrupted before the internal thread exits
	 * @throws IOException If an I/O error occurs when closing the socket
	 */
	public void close() throws IOException, InterruptedException {
		thread.interrupt();
		try {
			socket.close();
		} finally {
			responseListeners.clear();
			responseIds.clear();
			thread.join();
		}
	}
	
	protected void onClose() {}
	
}
