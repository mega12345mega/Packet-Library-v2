package com.luneruniverse.simplepacketlibrary.packets;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Reads and writes the information to be sent <br>
 * A constructor that accepts only a DataInputStream is required <br>
 * You have to register the packet class to allow sending and receiving
 * @see com.luneruniverse.simplepacketlibrary.PacketRegistry#registerPacket(Class)
 */
public abstract class Packet {
	/**
	 * Saves the packet data to a stream
	 * @param out The stream to save the data to
	 * @throws IOException If there was an exception writing the packet
	 */
	public abstract void write(DataOutputStream out) throws IOException;
}
