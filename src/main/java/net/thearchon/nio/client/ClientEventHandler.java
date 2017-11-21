package net.thearchon.nio.client;

import net.thearchon.nio.EventHandler;
import net.thearchon.nio.protocol.Packet;

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
