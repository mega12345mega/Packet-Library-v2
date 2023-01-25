package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import com.luneruniverse.simplepacketlibrary.Client;
import com.luneruniverse.simplepacketlibrary.PacketRegistry;
import com.luneruniverse.simplepacketlibrary.Server;
import com.luneruniverse.simplepacketlibrary.listeners.TypedPacketListener;
import com.luneruniverse.simplepacketlibrary.packets.Packet;
import com.luneruniverse.simplepacketlibrary.packets.PrimitivePacket;

public class SimplePacketLibraryTest4 {
	
	// Custom Packet
	public static class NameRequestPacket extends Packet {
		public NameRequestPacket() {
		}
		public NameRequestPacket(DataInputStream in) {
		}
		public void write(DataOutputStream out) {
		}
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		
		// Tracks all the custom packets
		PacketRegistry registry = new PacketRegistry();
		registry.registerPacket(NameRequestPacket.class);
		
		// Create the server & client
		Server server = new Server(60500);
		Client client = new Client(60500);
		
		// Register all the custom packets
		server.registerPackets(registry);
		client.registerPackets(registry);
		
		// Called when the client first connects
		server.addConnectionListener((connection, wait) -> {
			server.sendPacket(new NameRequestPacket(), (packet, connection2, wait2) -> {
				System.out.println("[Client -> Server] " + (String) ((PrimitivePacket) packet).getValue());
				try {
					// Stop the server when done
					// Will cause the client to also close
					server.close();throw new IOException("test");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		});
		
		// Called when the server sends a packet
		// Uses a TypedPacketListener unlike the first test
		client.addPacketListener(new TypedPacketListener()
				.when(NameRequestPacket.class, (packet, connection, wait) -> {
					client.reply(packet, new PrimitivePacket("alfred"));
				})
				.when(Packet.class, (packet, connection, wait) -> {
					System.out.println("[Server -> Client] " + packet.getClass().getName());
				}));
		
		// Start the server & client
		server.start();
		client.start();
		
	}
	
}
