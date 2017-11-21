package me.bennettca.nio.server;

public interface ServerFutureListener {

    /**
     * Server address was successfully bound to host.
     */
    void bindSucceeded();

    /**
     * Server failed to bind to specified host.
     */
    void bindFailed();
}
