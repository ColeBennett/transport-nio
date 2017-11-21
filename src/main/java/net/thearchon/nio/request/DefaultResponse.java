package net.thearchon.nio.request;

import net.thearchon.nio.protocol.Packet;

public class DefaultResponse implements Response {

    private final Packet packet;

    public DefaultResponse(Packet packet) {
        this.packet = packet;
    }

    @Override
    public Packet getPacket() {
        return packet;
    }
}
