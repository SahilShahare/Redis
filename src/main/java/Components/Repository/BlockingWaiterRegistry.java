package Components.Repository;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static Constants.CommandConstants.NULL_ARRAY;

@Component
public class BlockingWaiterRegistry {

    private final Map<String, Deque<BlockedClient>> waiters = new HashMap<>();

    // Registers ctx as waiting for an element to appear on key.
    public void addWaiter(String key, ChannelHandlerContext ctx, Double timeoutSeconds) {
        Deque<BlockedClient> queue = waiters.computeIfAbsent(key, k -> new ArrayDeque<>());
        BlockedClient client = new BlockedClient(ctx);
        queue.addLast(client);
        if (timeoutSeconds > 0.0) {
            long delayMicros = Math.round(timeoutSeconds * 1_000_000);
            client.timeoutTask = ctx.executor().schedule(() -> {
                if (queue.remove(client)) {
                    if (queue.isEmpty()) {
                        waiters.remove(key);
                    }
                    ctx.writeAndFlush(Unpooled.copiedBuffer(NULL_ARRAY, StandardCharsets.UTF_8));
                }
            }, delayMicros, TimeUnit.MICROSECONDS);
        }
    }

    public boolean hasWaiters(String key) {
        Deque<BlockedClient> q = waiters.get(key);
        return q != null && !q.isEmpty();
    }

    // Removes and returns the longest-waiting client for key, or null if none.
    public BlockedClient pollWaiter(String key) {
        Deque<BlockedClient> q = waiters.get(key);
        if (q == null || q.isEmpty()) {
            return null;
        }
        BlockedClient client = q.pollFirst();
        if (q.isEmpty()) {
            waiters.remove(key);
        }
        if (client.timeoutTask != null) {
            client.timeoutTask.cancel(false);
        }
        return client;
    }

    public void removeWaiter(ChannelHandlerContext ctx) {
        waiters.values().forEach(q -> q.removeIf(client -> {
            boolean match = client.ctx == ctx;
            if (match && client.timeoutTask != null) {
                client.timeoutTask.cancel(false);
            }
            return match;
        }));
        waiters.values().removeIf(Deque::isEmpty);
    }

    public static class BlockedClient {
        public final ChannelHandlerContext ctx;
        public ScheduledFuture<?> timeoutTask;

        BlockedClient(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
    }
}
