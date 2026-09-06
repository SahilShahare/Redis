package redis.store;

import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WatchStore {

    @Autowired
    Store store;

    private final Map<ChannelHandlerContext, Map<String, Long>> watches = new HashMap<>();

    public void watch(ChannelHandlerContext ctx, List<String> keys) {
        Map<String, Long> snapshot = watches.computeIfAbsent(ctx, k -> new HashMap<>());
        for (String key : keys) {
            snapshot.put(key, store.getVersion(key));
        }
    }

    public boolean isDirty(ChannelHandlerContext ctx) {
        Map<String, Long> snapshot = watches.get(ctx);
        if (snapshot == null) {
            return false;
        }
        for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
            if (!entry.getValue().equals(store.getVersion(entry.getKey()))) {
                return true;
            }
        }
        return false;
    }

    public void unwatch(ChannelHandlerContext ctx) {
        watches.remove(ctx);
    }
}