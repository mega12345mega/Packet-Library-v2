package com.luneruniverse.simplepacketlibrary;

class PacketData {
	
	final int id;
	final int responseId;
	final int packetType;
	final byte[] data;
	
	public PacketData(int id, int responseId, int packetType, byte[] data) {
		this.id = id;
		this.responseId = responseId;
		this.packetType = packetType;
		this.data = data;
	}
	
}
