package com.luneruniverse.simplepacketlibrary.accessors;

/**
 * Represents a raw packet just read in from an internal socket
 */
public class PacketData {
	
	/**
	 * The id of the packet
	 */
	public final int id;
	/**
	 * The packet that this packet is responding to
	 */
	public final int responseId;
	/**
	 * The type of the packet
	 */
	public final int packetType;
	/**
	 * The packet's payload
	 */
	public final byte[] data;
	
	/**
	 * Create a raw packet
	 * @param id The id
	 * @param responseId The packet that this packet is responding to
	 * @param packetType The type
	 * @param data The payload
	 */
	public PacketData(int id, int responseId, int packetType, byte[] data) {
		this.id = id;
		this.responseId = responseId;
		this.packetType = packetType;
		this.data = data;
	}
	
}
