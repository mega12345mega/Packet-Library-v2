package com.luneruniverse.simplepacketlibrary.listeners;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.Connection;
import com.luneruniverse.simplepacketlibrary.ServerConnection;
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
	 * @throws Exception If there was an exception handling the packet
	 * @see ServerConnection
	 * @see Client
	 */
	public void onPacket(Packet packet, Connection connection, WaitState wait) throws Exception;
}
