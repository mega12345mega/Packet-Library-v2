package com.luneruniverse.simplepacketlibrary;

import java.io.DataInputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

/**
 * Tracks all the packets that can be sent and received
 */
public class PacketRegistry {
	
	@FunctionalInterface
	public interface PacketConstructor<T extends Packet> {
		public T get(DataInputStream in) throws Exception;
	}
	private static class PacketType<T extends Packet> {
		private final Class<T> clazz;
		private final PacketConstructor<T> constructor;
		private PacketType(Class<T> clazz, PacketConstructor<T> constructor) {
			this.clazz = clazz;
			this.constructor = constructor;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PacketType<?>)
				return clazz.equals(((PacketType<?>) obj).clazz);
			return false;
		}
	}
	
	
	private final List<PacketType<? extends Packet>> packetTypes;
	
	/**
	 * Will by include all the default packet types
	 * @see com.luneruniverse.simplepacketlibrary.packets
	 */
	public PacketRegistry() {
		this.packetTypes = new ArrayList<>();
		registerPacket(PrimitivePacket.class);
	}
	
	/**
	 * Allow sending and receiving the packet type
	 * @param <T> The packet type
	 * @param clazz The packet class
	 * @param constructor The packet's constructor
	 * @see #registerPacket(Class)
	 */
	public <T extends Packet> void registerPacket(Class<T> clazz, PacketConstructor<T> constructor) {
		PacketType<T> fullType = new PacketType<>(clazz, constructor);
		if (packetTypes.contains(fullType))
			return;
		packetTypes.add(fullType);
	}
	
	/**
	 * Allow sending and receiving the packet type <br>
	 * The packet MUST have a constructor for ({@link DataInputStream})
	 * @param <T> The packet type
	 * @param clazz The packet class
	 * @throws IllegalArgumentException If the constructor is missing
	 * @see #registerPacket(Class, PacketConstructor)
	 */
	public <T extends Packet> void registerPacket(Class<T> clazz) throws IllegalArgumentException {
		try {
			Constructor<T> constructor = clazz.getConstructor(DataInputStream.class);
			registerPacket(clazz, constructor::newInstance);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to get constructor for packet class!", e);
		}
	}
	
	/**
	 * Register all the packet types included in the registry
	 * @param registry The packet types to include
	 */
	public void registerPackets(PacketRegistry registry) {
		registry.packetTypes.forEach(packetType -> {
			if (!packetTypes.contains(packetType))
				packetTypes.add(packetType);
		});
	}
	
	int getPacketId(Packet packet) {
		Class<? extends Packet> clazz = null;
		int id = 0;
		for (int i = 0; i < packetTypes.size(); i++) {
			Class<? extends Packet> packetType = packetTypes.get(i).clazz;
			if (packetType.isAssignableFrom(packet.getClass()) && (clazz == null || clazz.isAssignableFrom(packetType))) {
				clazz = packetType;
				id = i;
			}
		}
		if (clazz == null)
			throw new IllegalArgumentException("The packet type " + packet.getClass().getName() + " is not registered!");
		return id;
	}
	
	PacketConstructor<? extends Packet> getPacketType(int id) {
		try {
			return packetTypes.get(id).constructor;
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
}
