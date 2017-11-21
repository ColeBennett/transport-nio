package me.bennettca.nio.client;

import me.bennettca.nio.EventHandler;
import me.bennettca.nio.protocol.Packet;

public interface ClientEventHandler extends EventHandler {

    /**
     * Packet received from remote host.
     * @param packet packet received
     */
    void packetReceived(Packet packet);

    /**
     * Connection lost from remote host.
     */
    void connectionLost();
}
