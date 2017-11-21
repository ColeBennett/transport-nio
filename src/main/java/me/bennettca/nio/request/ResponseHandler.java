package me.bennettca.nio.request;

public interface ResponseHandler {

    void responseReceived(Response response);

    default void requestFailed() {

    }
}
