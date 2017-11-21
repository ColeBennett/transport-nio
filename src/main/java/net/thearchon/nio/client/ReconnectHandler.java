package net.thearchon.nio.client;

public interface ReconnectHandler {

    /**
     * Client successfully reconnected to remote host
     * after being disconnected for any reason.
     */
    void clientReconnected();
}
