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
        BlockedClient blockedClient = new BlockedClient(ctx);
        if (timeoutSeconds > 0.0) {
            blockedClient.timeoutTask = ctx.executor().schedule(() -> {
                if (blockedClient.completed.compareAndSet(false, true)) {
                    ctx.writeAndFlush(Unpooled.copiedBuffer(NULL_ARRAY, StandardCharsets.UTF_8));
                }
            }, Math.round(timeoutSeconds * 1e6), TimeUnit.MICROSECONDS);
        }
        waiters.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(blockedClient);
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
        return client;
    }

    public void removeWaiter(ChannelHandlerContext ctx) {
        waiters.values().forEach(q -> q.removeIf(client -> client.ctx == ctx));
        waiters.values().removeIf(Deque::isEmpty);
    }

    public static class BlockedClient {
        public final ChannelHandlerContext ctx;
        public ScheduledFuture<?> timeoutTask;
        public volatile AtomicBoolean completed = new AtomicBoolean(false);

        BlockedClient(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }
    }
}
