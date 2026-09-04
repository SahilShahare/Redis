package Components.Infrastructure;

public record StreamId(long ms, long seq) implements Comparable<StreamId> {

    public static final StreamId ZERO = new StreamId(0, 0);

    @Override
    public int compareTo(StreamId other) {
        int c = Long.compare(this.ms, other.ms);
        return c != 0 ? c : Long.compare(this.seq, other.seq);
    }

    @Override
    public String toString() {
        return ms + "-" + seq;
    }

    public static StreamId parseExplicit(String raw) {
        String[] parts = raw.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("ERR Invalid stream ID specified as stream command argument");
        }
        try {
            long ms = Long.parseLong(parts[0]);
            long seq = Long.parseLong(parts[1]);
            return new StreamId(ms, seq);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERR Invalid stream ID specified as stream command argument");
        }
    }
}