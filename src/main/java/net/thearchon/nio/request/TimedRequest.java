package net.thearchon.nio.request;

public class TimedRequest implements Request {

    private final ResponseHandler handler;
    private final long start;

    public TimedRequest(ResponseHandler handler) {
        this.handler = handler;
        start = System.currentTimeMillis();
    }

    @Override
    public ResponseHandler getHandler() {
        return handler;
    }

    public long getStartTime() {
        return start;
    }
}
