package com.luneruniverse.simplepacketlibrary.listeners;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.ServerConnection;

/**
 * Called when a {@link Client} connects to a {@link Server}
 */
@FunctionalInterface
public interface ConnectionListener {
	/**
	 * Called when a {@link Client} connects to a {@link Server}
	 * @param connection The new connection
	 * @param wait View the {@link WaitState} docs for information
	 * @throws Exception If there was an exception handling the connection
	 */
	void onConnect(ServerConnection connection, WaitState wait) throws Exception;
}