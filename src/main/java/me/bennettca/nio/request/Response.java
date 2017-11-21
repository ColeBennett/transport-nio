package me.bennettca.nio.request;

import me.bennettca.nio.protocol.Packet;

public interface Response {

    Packet getPacket();
}
