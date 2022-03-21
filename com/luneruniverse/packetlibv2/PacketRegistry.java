package com.luneruniverse.packetlibv2;

import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.List;

import com.luneruniverse.packetlibv2.packets.Packet;
import com.luneruniverse.packetlibv2.packets.PrimitivePacket;

/**
 * Tracks all the packets that can be sent and received
 */
public class PacketRegistry {
	
	private final List<Class<? extends Packet>> packetTypes;
	
	/**
	 * Will by include all the default packet types
	 * @see com.luneruniverse.packetlibv2.packets
	 */
	public PacketRegistry() {
		this.packetTypes = new ArrayList<>();
		registerPacket(PrimitivePacket.class);
	}
	
	/**
	 * Allow sending and receiving the packet type
	 * @param packetType The class of the packet type
	 */
	public void registerPacket(Class<? extends Packet> packetType) {
		if (packetTypes.contains(packetType))
			return;
		try {
			packetType.getConstructor(DataInputStream.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new IllegalArgumentException("Packet classes must have a constructor that only takes in a DataInputStream");
		}
		packetTypes.add(packetType);
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
			Class<? extends Packet> packetType = packetTypes.get(i);
			if (packetType.isAssignableFrom(packet.getClass()) && (clazz == null || clazz.isAssignableFrom(packetType))) {
				clazz = packetType;
				id = i;
			}
		}
		if (clazz == null)
			throw new IllegalArgumentException("The packet type " + packet.getClass().getName() + " is not registered!");
		return id;
	}
	
	Class<? extends Packet> getPacketType(int id) {
		try {
			return packetTypes.get(id);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
}
