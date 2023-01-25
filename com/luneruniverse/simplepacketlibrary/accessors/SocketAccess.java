package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.luneruniverse.simplepacketlibrary.ServerConnection;

public interface SocketAccess extends Closeable {
	public PacketData readPacket() throws IOException, InterruptedException;
	public void sendPacket(byte[] data) throws IOException;
	public boolean isClosed();
	
	public default void setConnection(ServerConnection serverConnection) {}
	
	public default PacketData readPacket(DataInputStream in) throws IOException {
		int id = in.readInt();
		int responseId = in.readInt();
		int packetType = in.readInt();
		byte[] data = new byte[in.readInt()];
		readBlockingArray(in, data, 0, data.length);
		
		return new PacketData(id, responseId, packetType, data);
	}
	public static int readBlockingArray(InputStream stream, byte[] buf, int offset, int length) throws IOException {
		int numRead = 0;
		while (numRead < length) {
			int readNow = stream.read(buf, offset + numRead, length - numRead);
			if (readNow < 0)
				break;
			numRead += readNow;
		}
		return numRead;
	}
}
