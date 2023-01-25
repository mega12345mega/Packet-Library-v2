package com.luneruniverse.simplepacketlibrary.accessors;

public class PacketData {
	
	public final int id;
	public final int responseId;
	public final int packetType;
	public final byte[] data;
	
	public PacketData(int id, int responseId, int packetType, byte[] data) {
		this.id = id;
		this.responseId = responseId;
		this.packetType = packetType;
		this.data = data;
	}
	
}
