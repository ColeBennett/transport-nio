package net.thearchon.nio.request;

import io.netty.channel.EventLoop;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class RequestPool {

    private final AtomicLong nextId = new AtomicLong();
    private final Map<Long, Request> requests = new ConcurrentHashMap<>();

    protected abstract void schedule(Runnable task);

    protected void requestFailed(Request request) {
        request.getHandler().requestFailed();
    }

    public long track(Request request, EventLoop eventLoop) {
        final long id = nextId.getAndIncrement();
        requests.put(id, request);
        schedule(() -> {
            Request request1 = take(id);
            if (request1 != null) {
                requestFailed(request1);
            }
        });
        return id;
    }

    public Request take(long id) {
        return requests.remove(id);
    }

    public void clear() {
        Iterator<Entry<Long, Request>> itr = requests.entrySet().iterator();
        while (itr.hasNext()) {
            Entry<Long, Request> entry = itr.next();
            entry.getValue().getHandler().requestFailed();
            itr.remove();
        }
    }
}
