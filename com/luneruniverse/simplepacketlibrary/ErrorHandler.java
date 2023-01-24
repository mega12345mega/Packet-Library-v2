package com.luneruniverse.simplepacketlibrary;

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
		UNREGISTERED_PACKET(CloseInfo.NOTHING),
		CONSTRUCTING_PACKET(CloseInfo.NOTHING),
		HANDLING_PACKETS(CloseInfo.CLOSE_CONNECTION),
		INSIDE_PACKET_LISTENER(CloseInfo.NOTHING),
		CLOSING_CONNECTION(CloseInfo.CLOSE_CONNECTION),
		
		// Server Side
		ACCEPTING_CONNECTIONS(CloseInfo.NOTHING),
		INSIDE_CONNECTION_LISTENER(CloseInfo.NOTHING),
		
		// WebSocket
		READING_WEBSOCKET_PACKET(CloseInfo.NOTHING),
		GENERIC_WEBSOCKET(CloseInfo.UNKNOWN);
		
		private CloseInfo closeInfo;
		private Error(CloseInfo closeInfo) {
			this.closeInfo = closeInfo;
		}
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
