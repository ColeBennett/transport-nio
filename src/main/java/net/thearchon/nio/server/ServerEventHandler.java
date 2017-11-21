package net.thearchon.nio.server;

import net.thearchon.nio.EventHandler;
import net.thearchon.nio.protocol.Packet;

public interface ServerEventHandler extends EventHandler {

    /**
     * Packet received from a client.
     * @param client client which sent the packet
     * @param packet packet received
     */
    void packetReceived(ClientListener client, Packet packet);

    /**
     * Client connected to the server.
     * @param client connected client
     */
    void clientConnected(ClientListener client);

    /**
     * Client disconnceted from the server.
     * @param client disconnected client
     */
    void clientDisconnected(ClientListener client);
}
