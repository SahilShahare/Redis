package Components.Infrastructure;

import Components.Repository.ListValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Stream {
    final List<StreamEntry> entries;
    StreamId lastId;

    public Stream() {
        entries = new ArrayList<>();
        lastId = StreamId.ZERO;
    }

    // First index whose entry's id is >= target
    private int lowerBound(StreamId target) {
        int lo = 0, hi = entries.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (entries.get(mid).id.compareTo(target) < 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    // First index whose entry's id is > target
    private int upperBound(StreamId target) {
        int lo = 0, hi = entries.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (entries.get(mid).id.compareTo(target) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public String xadd(String idSpec, LinkedHashMap<String, String> fields) throws IllegalArgumentException {

        StreamId id;
        id = resolveId(idSpec);

        if (id.compareTo(StreamId.ZERO) <= 0) {
            throw new IllegalArgumentException("ERR The ID specified in XADD must be greater than 0-0");
        }
        if (id.compareTo(lastId) <= 0) {
            throw new IllegalArgumentException(
                    "ERR The ID specified in XADD is equal or smaller than the target stream top item");
        }

        entries.add(new StreamEntry(id, fields));
        lastId = id;
        return id.toString();
    }

    public List<StreamEntry> xrange(String startSpec, String endSpec) throws IllegalArgumentException {
        StreamId start = parseBound(startSpec, true);
        StreamId end = parseBound(endSpec, false);

        int from = lowerBound(start);  // first index with id >= start
        int to = upperBound(end);       // one past the last index with id <= end
        return (from < to) ? entries.subList(from, to) : List.of();
    }

    public List<StreamEntry> xread(String id) throws IllegalArgumentException {
        StreamId afterId = StreamId.parseExplicit(id);
        int from = upperBound(afterId); // first index strictly greater than afterId
        return entries.subList(from, entries.size());
    }

    public StreamId getLastId() {
        return this.lastId;
    }

    private StreamId resolveId(String idSpec) {
        if ("*".equals(idSpec)) {
            long ms = System.currentTimeMillis();
            long seq = (ms == this.lastId.ms()) ? this.lastId.seq() + 1 : 0;
            return new StreamId(ms, seq);
        }

        if (idSpec.endsWith("-*")) {
            long ms;
            try {
                ms = Long.parseLong(idSpec.substring(0, idSpec.length() - 2));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("ERR Invalid stream ID specified as stream command argument");
            }
            long seq;
            if (ms == this.lastId.ms()) {
                seq = this.lastId.seq() + 1;
            } else {
                seq = (ms == 0) ? 1 : 0;
            }
            return new StreamId(ms, seq);
        }

        return StreamId.parseExplicit(idSpec);
    }

    private StreamId parseBound(String raw, boolean isStart) {
        if ("-".equals(raw)) {
            return StreamId.ZERO; // inclusive lower bound: every real entry is > 0-0
        }
        if ("+".equals(raw)) {
            return new StreamId(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        if (raw.contains("-")) {
            return StreamId.parseExplicit(raw);
        }
        long ms;
        try {
            ms = Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ERR Invalid stream ID specified as stream command argument");
        }
        return new StreamId(ms, isStart ? 0 : Long.MAX_VALUE);
    }

}
