package com.luneruniverse.packetlibv2;

import java.io.IOException;

/**
 * Called when a {@link Client} connects to a {@link Server}
 */
@FunctionalInterface
public interface ConnectionListener {
	/**
	 * Called when a {@link Client} connects to a {@link Server}
	 * @param connection The new connection
	 * @param wait View the {@link WaitState} docs for information
	 */
	void onConnect(ServerConnection connection, WaitState wait) throws IOException;
}