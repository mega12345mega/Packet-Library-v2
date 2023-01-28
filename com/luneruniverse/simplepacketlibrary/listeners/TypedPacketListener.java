package com.luneruniverse.simplepacketlibrary.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.Connection;
import com.luneruniverse.simplepacketlibrary.ServerConnection;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Register packet listeners for specific packet types
 * @see #when(Class, GenericPacketListener)
 */
public class TypedPacketListener implements PacketListener {
	
	/**
	 * Called when a packet is received
	 * @param <T> The packet type
	 */
	@FunctionalInterface
	public interface GenericPacketListener<T extends Packet> {
		/**
		 * Called when a packet is received
		 * @param packet The packet that was received
		 * @param connection What connection the packet is from
		 * @param wait View the {@link WaitState} docs for information
		 * @throws Exception If there was an exception handling the packet
		 * @see ServerConnection
		 * @see Client
		 */
		public void onPacket(T packet, Connection connection, WaitState wait) throws Exception;
		
		/**
		 * Convert this to a normal {@link PacketListener} with an unchecked cast <br>
		 * Only call {@link PacketListener#onPacket(Packet, Connection, WaitState)} if the packet is of the correct type
		 * @return The packet listener
		 */
		@SuppressWarnings("unchecked")
		public default PacketListener toPacketListener() {
			return (packet, connection, wait) -> onPacket((T) packet, connection, wait);
		}
	}
	
	private final Map<Class<?>, List<GenericPacketListener<?>>> listeners;
	private final PacketListener defaultListener;
	
	/**
	 * Create a typed packet listener with a default listener,
	 * which will be called when no other packet listeners are called
	 * @param defaultListener The listener
	 * @see #TypedPacketListener()
	 */
	public TypedPacketListener(PacketListener defaultListener) {
		this.listeners = new HashMap<>();
		this.defaultListener = defaultListener;
	}
	/**
	 * Create a typed packet listener without a default listener<br>
	 * Packets which don't correspond to any listeners will be ignored
	 * @see #TypedPacketListener(PacketListener)
	 */
	public TypedPacketListener() {
		this(null);
	}
	
	/**
	 * When the specified packet (or a subclass) is received, the listener will be called
	 * @param <T> The packet type to receive
	 * @param clazz The packet type's class
	 * @param listener The listener to call
	 * @return this
	 */
	public <T extends Packet> TypedPacketListener when(Class<T> clazz, GenericPacketListener<T> listener) {
		listeners.computeIfAbsent(clazz, key -> new ArrayList<>()).add(listener);
		return this;
	}
	
	@Override
	public void onPacket(Packet packet, Connection connection, WaitState wait) throws Exception {
		List<PacketListener> matchedListeners = listeners.entrySet().stream()
				.filter(type -> type.getKey().isAssignableFrom(packet.getClass()))
				.flatMap(entry -> entry.getValue().stream())
				.map(GenericPacketListener::toPacketListener)
				.collect(Collectors.toList());
		
		if (matchedListeners.isEmpty()) {
			if (defaultListener != null)
				defaultListener.onPacket(packet, connection, wait);
		} else {
			try {
				connection.invokePacketListeners(matchedListeners, packet);
			} catch (InterruptedException e) {}
		}
	}
	
}
