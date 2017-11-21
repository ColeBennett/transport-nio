package me.bennettca.nio.server;

import me.bennettca.nio.EventHandler;
import me.bennettca.nio.protocol.Packet;

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
