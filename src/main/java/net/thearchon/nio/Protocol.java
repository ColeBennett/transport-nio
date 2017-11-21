package net.thearchon.nio;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public enum Protocol {

    /**
     * General/System
     */
    CONNECT(0),

    NETWORK_COUNT_UPDATE(1),
    NETWORK_SLOT_UPDATE(2),
    LOBBY_SLOT_UPDATE(3),
    MOTD_UPDATE(4),
    SERVER_COUNT_LIST(5),
    SERVER_MONITOR(6),

    FORWARD(7),
    SHUTDOWN(8),

    UPDATE_CONFIG(9),
    UPDATE_JAR(10),

    LOG_PURCHASE(11),
    REQUEST_CONNECT(12),
    CONFIG(13),

    QUEUE_POSITION_UPDATE(14),
    QUEUE_SIZE_UPDATE(15),

    /**
     * BungeeCord
     */
    MESSAGE(20),
    BROADCAST(21),
    KICK_PLAYER(22),
    SEND_PLAYER(23),
    EXEC_COMMAND(24),

    LOGIN_REQUEST(25),
    ENABLE_CHAT(26),
    DISABLE_CHAT(27),
    REGISTER_PROXY_COMMANDS(28),
    UNREGISTER_PROXY_COMMANDS(29),

    PLAYER_CONNECT(30),
    PLAYER_DISCONNECT(31),
    PLAYER_SERVER_SWITCH(32),
    PLAYER_CHAT(33),
    PLAYER_COMMAND(34),

    /**
     * Server
     */
    RANK_UPDATE(50),
    GET_PLAYER_INFO(51),

    SERVER_LIST(52),
    ADD_SERVER(53),
    REMOVE_SERVER(54),
    SERVER_STATE_UPDATE(55),
    SERVER_PLAYER_COUNT_UPDATE(56),
    SERVER_SLOT_UPDATE(57),
    LOBBY_COUNT_LIST(58),

    /**
     * Game
     */
    GAME_REQUEST_JOIN(100),
    GAME_DATA(101),
    GAME_SERVER_LIST(102),
    GAME_ADD_SERVER(103),
    GAME_REMOVE_SERVER(104),
    GAME_PLAYER_COUNT_UPDATE(105),
    GAME_SPECTATOR_COUNT_UPDATE(106),
    GAME_SLOT_UPDATE(107),
    GAME_STARTING_COUNT_UPDATE(108),
    GAME_STATE_UPDATE(109),
    GAME_MAP_NAME_UPDATE(110),
    GAME_GET_MAP_LIST(111),
    GAME_MAP_LIST(112),
    GAME_GET_MAP_FILE(113),
    GAME_MAP(114),
    GAME_WIN(115),
    
    ADD_COINS(90),
    REMOVE_COINS(91),
    COIN_UPDATE(92),
    COIN_MULTIPLIER_UPDATE(93),

    /**
     * Arcade
     */
    GIVE_WHEEL_SPINS(94),
    GIVE_TOKENS(95),

    /**
     * Misc
     */

    VIOLATION(96),
    REPORT(97),

    /**
     * Server Pool System
     */
    POOL_SERVER_LIST(200),
    POOL_SERVER_DATA(201),
    POOL_ADD_SERVER(202),
    POOL_REMOVE_SERVER(203),

    ;

    private static final Map<Short, Protocol> VALUES;

    static {
        Protocol[] headers = values();
        VALUES = new HashMap<>(headers.length);
        for (Protocol header : headers) {
            VALUES.put(header.id, header);
        }
    }

    private final short id;

    Protocol(int id) {
        this.id = (short) id;
    }

    public short getId() {
        return id;
    }

    public BufferedPacket buffer() {
        return writeHeader(new BufferedPacket(1));
    }

    public BufferedPacket buffer(int initialCapacity) {
        return writeHeader(new BufferedPacket(initialCapacity + 1));
    }

    public <T> BufferedPacket construct(Collection<T> values) {
        return buffer(values.size()).writeAll(values);
    }

    public <K, V> BufferedPacket construct(Map<K, V> map) {
        return buffer(map.size() * 2).writeMap(map);
    }

    public BufferedPacket construct(Object... values) {
        return buffer(values.length).writeAll(values);
    }

    public BufferedPacket writeHeader(BufferedPacket buf) {
        return buf.writeShort(id);
    }

    public static Protocol valueOf(BufferedPacket buf) {
        return valueOf(buf.readShort());
    }

    public static Protocol valueOf(short id) {
        return VALUES.get(id);
    }
}
