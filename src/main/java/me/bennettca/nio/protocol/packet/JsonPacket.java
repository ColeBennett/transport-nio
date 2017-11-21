package me.bennettca.nio.protocol.packet;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import me.bennettca.nio.protocol.Packet;

public class JsonPacket extends Packet {

    public static final short PACKET_ID = -7;
    private static final JsonParser PARSER = new JsonParser();

    private JsonElement payload;

    public JsonPacket() {
        this(null);
    }

    public JsonPacket(JsonElement payload) {
        super(PACKET_ID);

        this.payload = payload;
    }

    @Override
    public void read(ByteBuf buf) {
        payload = PARSER.parse(readString(buf));
    }

    @Override
    public void write(ByteBuf buf) {
        writeString(buf, payload.toString());
    }

    public JsonElement getPayload() {
        return payload;
    }
}
