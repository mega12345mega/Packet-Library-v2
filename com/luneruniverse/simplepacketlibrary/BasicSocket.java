package com.luneruniverse.simplepacketlibrary;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;

interface BasicSocket extends Closeable {
	public PacketData readPacket() throws IOException;
	public void sendPacket(byte[] data) throws IOException;
	public boolean isClosed();
	
	default PacketData readPacket(DataInputStream in) throws IOException {
		int id = in.readInt();
		int responseId = in.readInt();
		int packetType = in.readInt();
		byte[] data = new byte[in.readInt()];
		in.read(data);
		
		return new PacketData(id, responseId, packetType, data);
	}
}
