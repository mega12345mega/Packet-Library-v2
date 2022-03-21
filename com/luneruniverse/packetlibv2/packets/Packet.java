package com.luneruniverse.packetlibv2.packets;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Reads and writes the information to be sent <br>
 * A constructor that accepts only a DataInputStream is required <br>
 * You have to register the packet class to allow sending and receiving
 * @see com.luneruniverse.packetlibv2.PacketRegistry#registerPacket(Class)
 */
public abstract class Packet {
	/**
	 * Saves the packet data to a stream
	 * @param out The stream to save the data to
	 * @throws IOException
	 */
	public abstract void write(DataOutputStream out) throws IOException;
}
