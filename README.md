# Simple Packet Library
A simple library to exchange packets and respond to specific packets

A rewrite of [Packet Library](https://github.com/mega12345mega/Packet-Library) with many important upgrades

# Features
* Custom packets
* Responding to specific packets, creating a "side conversation" of sorts
* Controlling the processing of packets while handling a packet
* Websockets (both ws:// and wss://)
* Easy to begin using
* Fully documented with javadocs

# JavaScript variant
A [JavaScript rewrite](https://github.com/mega12345mega/Simple-Packet-Library-JS) is now available! It doesn't support servers or non-WebSocket clients, but it can still communicate with a Java WebSocket server!
Use this to get started: <br>
`<script src="https://luneruniverse.com/simple-packet-library" crossorigin="anonymous"></script>`

# How to use
Refer to the [Simple Packet Library Test](https://github.com/mega12345mega/Simple-Packet-Library/blob/main/test/SimplePacketLibraryTest.java) for an example with custom packets

This is the [Simple Packet Library Test 2](https://github.com/mega12345mega/Simple-Packet-Library/blob/main/test/SimplePacketLibraryTest2.java) example
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
