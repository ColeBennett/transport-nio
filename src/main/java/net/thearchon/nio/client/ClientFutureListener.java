package net.thearchon.nio.client;

public interface ClientFutureListener {

    /**
     * Clienct successfully connected to remote host.
     */
    void clientConnected();

    /**
     * Client failed to connect to remote host or failed
     * to bind to specified local address.
     */
    void connectFailed();
}
