package test;

import java.io.File;
import java.io.FileInputStream;

import javax.net.ssl.SSLContext;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

public class SimplePacketLibraryTest3 {
	
	public static void main(String[] args) throws Exception {
		// Create the server and client
		FileInputStream stream = new FileInputStream(new File("src/test/keystore.jks"));
		SSLContext ssl = Server.generateSSLContext(stream, "JKS", "storepassword", "keypassword", "SunX509");
		stream.close();
		Server server = new Server(31415).useWebSocket(true)
				.addServerErrorHandler((e, obj, info) -> e.printStackTrace()).setSecure(ssl)
				.addConnectionErrorHandler((e, obj, info) -> e.printStackTrace());
		Client client = new Client("wss://localhost", 31415).useWebSocket(true)
				.addErrorHandler((e, obj, info) -> e.printStackTrace());
		
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
