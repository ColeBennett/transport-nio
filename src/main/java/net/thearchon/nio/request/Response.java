package net.thearchon.nio.request;

import net.thearchon.nio.protocol.Packet;

public interface Response {

    Packet getPacket();
}
