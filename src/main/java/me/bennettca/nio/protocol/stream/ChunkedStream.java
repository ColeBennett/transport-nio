package me.bennettca.nio.protocol.stream;

public class ChunkedStream {

    private long progress;
    private long total;
    private long chunksReceived;
    private long start;
    private long end;

    ChunkedStream(long total) {
        this.total = total;
    }

    public long getProgress() {
        return progress;
    }

    public long getTotal() {
        return total;
    }

    public long getAvailable() {
        return total - progress;
    }

    public long getChunksReceived() {
        return chunksReceived;
    }

    public long getStartTime() {
        return start;
    }

    public long getEndTime() {
        return end;
    }

    public long getTimeCompleted() {
        if (start == 0 || end == 0) {
            throw new IllegalStateException("Stream still in progress");
        }
        return end - start;
    }

    public boolean isComplete() {
        return progress == total;
    }

    void bytesTransferred(long transferred) {
        progress += transferred;
        chunksReceived++;

        if (chunksReceived == 1) {
            start = System.currentTimeMillis();
        } else if (isComplete()) {
            end = System.currentTimeMillis();
        }
    }

    public double calculateProgress() {
        return ((double) progress / (double) total) * 100;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("ChunkedStream(");
        if (isComplete()) {
            buf.append("complete, transferred: ");
            buf.append(progress);
            buf.append(", chunks: ");
            buf.append(chunksReceived);
            buf.append(", time completed: ");
            buf.append(getTimeCompleted() / 1000);
            buf.append(" sec");
        } else {
            buf.append("in progress, transferred: ");
            buf.append(progress);
            buf.append(", total: ");
            buf.append(total);
            buf.append(", chunks: ");
            buf.append(chunksReceived);
        }
        buf.append(')');
        return buf.toString();
    }
}
