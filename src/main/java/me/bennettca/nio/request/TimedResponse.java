package me.bennettca.nio.request;

import me.bennettca.nio.protocol.Packet;

public class TimedResponse implements Response {

    private final Packet packet;
    private final long timeCompleted;

    public TimedResponse(Packet packet, long timeCompleted) {
        this.packet = packet;
        this.timeCompleted = timeCompleted;
    }

    @Override
    public Packet getPacket() {
        return packet;
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }
}
