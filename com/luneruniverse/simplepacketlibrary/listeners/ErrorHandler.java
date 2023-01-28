package com.luneruniverse.simplepacketlibrary.listeners;

/**
 * Called when an error is thrown
 * @see Error
 */
public interface ErrorHandler<T> {
	/**
	 * How the error was handled by the library
	 * @see CloseInfo#NOTHING
	 * @see CloseInfo#CLOSE_CONNECTION
	 * @see CloseInfo#CLOSE_SERVER
	 * @see CloseInfo#UNKNOWN
	 * @see Error
	 */
	public enum CloseInfo {
		/**
		 * Indicates nothing has changed; the connection is still alive
		 * @see CloseInfo
		 */
		NOTHING,
		/**
		 * Indicates that the connection was closed, but if this is a server, the server is still alive
		 * @see CloseInfo
		 */
		CLOSE_CONNECTION,
		/**
		 * Indicates the entire server was stopped
		 * @see CloseInfo
		 */
		CLOSE_SERVER,
		/**
		 * This only occurs when there was an error with the WebSocket <br>
		 * This does NOT always occur when in WebSocket mode
		 * @see CloseInfo
		 */
		UNKNOWN
	}
	
	/**
	 * What error occurred and how it was handled
	 * @see ErrorHandler
	 * @see CloseInfo
	 */
	public enum Error {
		// Connection
		/**
		 * The library received a packet with an invalid id
		 */
		UNREGISTERED_PACKET(CloseInfo.NOTHING),
		/**
		 * A packet constructor threw an exception
		 */
		CONSTRUCTING_PACKET(CloseInfo.NOTHING),
		/**
		 * There was an exception in the main connection thread
		 */
		HANDLING_PACKETS(CloseInfo.CLOSE_CONNECTION),
		/**
		 * A packet listener threw an exception
		 */
		INSIDE_PACKET_LISTENER(CloseInfo.NOTHING),
		/**
		 * There was an exception while closing the connection
		 */
		CLOSING_CONNECTION(CloseInfo.CLOSE_CONNECTION),
		
		// Server Side
		/**
		 * There was an exception while a connection was being accepted
		 */
		ACCEPTING_CONNECTIONS(CloseInfo.NOTHING),
		/**
		 * A connection listener threw an exception
		 */
		INSIDE_CONNECTION_LISTENER(CloseInfo.NOTHING),
		
		// WebSocket
		/**
		 * There was an exception while reading a packet from a WebSocket
		 */
		READING_WEBSOCKET_PACKET(CloseInfo.NOTHING),
		/**
		 * There was an unknown exception in a WebSocket
		 */
		GENERIC_WEBSOCKET(CloseInfo.UNKNOWN);
		
		private CloseInfo closeInfo;
		private Error(CloseInfo closeInfo) {
			this.closeInfo = closeInfo;
		}
		/**
		 * Get the result of the error
		 * @return What was closed
		 */
		public CloseInfo getCloseInfo() {
			return closeInfo;
		}
	}
	
	/**
	 * Called when an error is thrown
	 * @param e The error
	 * @param obj Where the error originated
	 * @param error What error occurred
	 */
	public void onError(Exception e, T obj, Error error);
}
