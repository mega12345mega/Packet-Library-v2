package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;

import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Called when a packet is received
 */
@FunctionalInterface
public interface PacketListener {
	/**
	 * Called when a packet is received
	 * @param packet The packet that was received
	 * @param connection What connection the packet is from
	 * @param wait View the {@link WaitState} docs for information
	 * @see ServerConnection
	 * @see Client
	 */
	public void onPacket(Packet packet, Connection connection, WaitState wait) throws IOException;
}
