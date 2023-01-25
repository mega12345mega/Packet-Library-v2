package com.luneruniverse.simplepacketlibrary.listeners;

import com.luneruniverse.simplepacketlibrary.Connection;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

/**
 * Whether or not the {@link Connection} should wait to handle more {@link Packet}s <br>
 * By default the handler will wait <br>
 * Not waiting can cause the {@link PacketListener}s to receive packets in an incorrect order <br>
 * Waiting will prevent responses from being received <br>
 * Don't use {@link Connection#sendPacketWithResponse(Packet, int)} without calling {@link #dontWait()}
 */
public class WaitState {
	
	private boolean wait;
	
	public WaitState() {
		wait = true;
	}
	
	/**
	 * Stop waiting for the {@link PacketListener}s to finish executing before handling another {@link Packet}
	 */
	public void dontWait() {
		wait = false;
	}
	
	/**
	 * @return If the {@link Connection} is waiting for the {@link PacketListener} to finish
	 */
	public boolean isWaiting() {
		return wait;
	}
	
}
