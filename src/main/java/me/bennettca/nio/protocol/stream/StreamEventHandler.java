package me.bennettca.nio.protocol.stream;

public interface StreamEventHandler {

    void streamStarted(ChunkedStream stream);

    void streamProgressed(ChunkedStream stream);

    void streamEnded(ChunkedStream stream);
}
