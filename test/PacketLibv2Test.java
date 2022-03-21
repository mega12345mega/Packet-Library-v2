package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import com.luneruniverse.packetlibv2.Client;
import com.luneruniverse.packetlibv2.PacketRegistry;
import com.luneruniverse.packetlibv2.Server;
import com.luneruniverse.packetlibv2.packets.Packet;
import com.luneruniverse.packetlibv2.packets.PrimitivePacket;

public class PacketLibv2Test {
	
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
			connection.sendPacket(new NameRequestPacket(), (packet, connection2, wait2) -> {
				System.out.println("[Client -> Server] " + (String) ((PrimitivePacket) packet).getValue());
				try {
					// Stop the server when done
					// Will cause the client to also close
					server.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		});
		
		// Called when the server sends a packet
		client.addPacketListener((packet, connection, wait) -> {
			System.out.println("[Server -> Client] " + packet.getClass().getName());
			if (packet instanceof NameRequestPacket) {
				client.reply(packet, new PrimitivePacket("alfred"));
			}
		});
		
		// Start the server & client
		server.start();
		client.start();
		
	}
	
}
