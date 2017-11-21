package me.bennettca.nio.protocol;

public abstract class LengthAwarePacket extends Packet {

    public LengthAwarePacket(short id) {
        super(id);
    }

    public abstract int length();
}
