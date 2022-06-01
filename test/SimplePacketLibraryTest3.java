package test;

import java.io.IOException;
import java.net.UnknownHostException;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

public class SimplePacketLibraryTest3 {
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		// Create the server and client
		Server server = new Server(31415).useWebSocket(true);
		Client client = new Client("ws://localhost", 31415).useWebSocket(true);
		
		// Start the server and client
		server.start();
		client.start();
		
		// Handle packets from the client
		server.addPacketListener((packet, connection, wait) -> {
			if (packet instanceof PrimitivePacket) {
				PrimitivePacket castedPacket = (PrimitivePacket) packet;
				if (castedPacket.isString() && castedPacket.getValue().equals("ping"))
					connection.reply(packet, new PrimitivePacket("pong"));
			}
		});
		
		// Send and receive a packet from the server
		Packet packet = client.sendPacketWithResponse(new PrimitivePacket("ping"));
		System.out.println(((PrimitivePacket) packet).getValue());
		
		// Stop the server
		server.close();
		
		// The client is automatically disconnected and will close too
	}
	
}
