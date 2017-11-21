package net.thearchon.nio.request;

public class DefaultRequest implements Request {

    private final ResponseHandler handler;

    public DefaultRequest(ResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public ResponseHandler getHandler() {
        return handler;
    }
}
