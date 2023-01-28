package com.luneruniverse.simplepacketlibrary.accessors;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.luneruniverse.simplepacketlibrary.ServerConnection;

/**
 * The interface for the different socket modes
 */
public interface SocketAccess extends Closeable {
	/**
	 * Read in a packet from the internal socket
	 * @return The read packet
	 * @throws IOException If there was an error reading the packet
	 * @throws InterruptedException If the thread was interrupted
	 * @see #readPacket(DataInputStream)
	 */
	public PacketData readPacket() throws IOException, InterruptedException;
	/**
	 * Send a packet to the internal socket
	 * @param data The data to send
	 * @throws IOException If there was an error sending the packet
	 */
	public void sendPacket(byte[] data) throws IOException;
	/**
	 * Check if the internal socket has been closed
	 * @return If the internal socket is closed
	 */
	public boolean isClosed();
	
	/**
	 * Pass the {@link ServerConnection} to the socket implementation
	 * @param serverConnection The connection
	 */
	public default void setConnection(ServerConnection serverConnection) {}
	
	/**
	 * Parse a raw packet from a data stream
	 * @param in The data stream
	 * @return The raw packet
	 * @throws IOException If there was an exception while parsing the packet
	 */
	public default PacketData readPacket(DataInputStream in) throws IOException {
		int id = in.readInt();
		int responseId = in.readInt();
		int packetType = in.readInt();
		byte[] data = new byte[in.readInt()];
		readBlockingArray(in, data, 0, data.length);
		
		return new PacketData(id, responseId, packetType, data);
	}
	/**
	 * Read into an array from a stream as a blocking operation
	 * @param stream The stream to read from
	 * @param buf The target array
	 * @param offset Where the bytes should start being written
	 * @param length The number of bytes to read in
	 * @return The number of bytes read in
	 * @throws IOException If there was an exception while reading the bytes
	 */
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
