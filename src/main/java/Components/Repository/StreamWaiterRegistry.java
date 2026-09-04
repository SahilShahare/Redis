package Components.Repository;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static Constants.CommandConstants.NULL_ARRAY;

@Component
public class StreamWaiterRegistry {

    private final Map<String, Deque<BlockedXRead>> waiters = new HashMap<>();

    public void addWaiter(List<String> keys, List<String> ids, ChannelHandlerContext ctx, long timeoutMillis) {
        BlockedXRead client = new BlockedXRead(ctx, keys, ids);
        for (String key : keys) {
            waiters.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(client);
        }

        if (timeoutMillis > 0) {
            client.timeoutTask = ctx.executor().schedule(() -> {
                if (removeFromAllKeys(client)) {
                    ctx.writeAndFlush(Unpooled.copiedBuffer(NULL_ARRAY, StandardCharsets.UTF_8));
                }
            }, timeoutMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void notifyPush(String key, BiFunction<List<String>, List<String>, String> reader) {
        Deque<BlockedXRead> q = waiters.get(key);
        if (q == null || q.isEmpty()) {
            return;
        }

        for (BlockedXRead client : new ArrayList<>(q)) {
            String result = reader.apply(client.keys, client.ids);
            if (!NULL_ARRAY.equals(result)) {
                if (client.timeoutTask != null) {
                    client.timeoutTask.cancel(false);
                }
                removeFromAllKeys(client);
                client.ctx.writeAndFlush(Unpooled.copiedBuffer(result, StandardCharsets.UTF_8));
            }
        }
    }

    public void removeWaiter(ChannelHandlerContext ctx) {
        for (Deque<BlockedXRead> q : waiters.values()) {
            q.removeIf(client -> {
                boolean match = client.ctx == ctx;
                if (match && client.timeoutTask != null) {
                    client.timeoutTask.cancel(false);
                }
                return match;
            });
        }
        waiters.values().removeIf(Deque::isEmpty);
    }

    private boolean removeFromAllKeys(BlockedXRead client) {
        boolean found = false;
        for (String key : client.keys) {
            Deque<BlockedXRead> q = waiters.get(key);
            if (q != null && q.remove(client)) {
                found = true;
                if (q.isEmpty()) {
                    waiters.remove(key);
                }
            }
        }
        return found;
    }

    private static class BlockedXRead {
        final ChannelHandlerContext ctx;
        final List<String> keys;
        final List<String> ids;
        ScheduledFuture<?> timeoutTask;

        BlockedXRead(ChannelHandlerContext ctx, List<String> keys, List<String> ids) {
            this.ctx = ctx;
            this.keys = keys;
            this.ids = ids;
        }
    }
}