# Simple Packet Library
A simple library to exchange packets and respond to specific packets

A rewrite of [Packet Library](https://github.com/mega12345mega/Packet-Library) with many important upgrades

# Features
* Custom packets
* Responding to specific packets, creating a "side conversation" of sorts
* Controlling the processing of packets while handling a packet
* Easy to begin using
* Fully documented with javadocs

# How to use
Refer to the [Simple Packet Library Test](https://github.com/mega12345mega/Simple-Packet-Library/blob/main/test/SimplePacketLibraryTest.java) for a full example
```
// Create the server and client
Server server = new Server(31415);
Client client = new Client("localhost", 31415);

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
```
