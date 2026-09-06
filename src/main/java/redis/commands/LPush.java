package redis.commands;

import redis.blocking.BlockingWaiterRegistry;
import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static constants.CommandConstants.LPUSH;

@Component(LPUSH)
public class LPush implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length < 3) {
            return Optional.of(
                    respSerializer.serializeError("ERR wrong number of arguments for 'lpush' command"));
        } else {
            String key = command[1];
            List<Object> values = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(command, 2, command.length)));
            try {
                int result = store.lpush(key, values);
                serveWaiters(key);
                return Optional.of(respSerializer.serializeInteger(result));
            } catch (InvalidTypeException e) {
                return Optional.of(respSerializer.serializeError(e.getMessage()));
            }

        }
    }

    private void serveWaiters(String key) {
        while (blockingWaiterRegistry.hasWaiters(key)) {
            Object popped = store.pollListLeft(key);
            if (popped == null) {
                break; // nothing left to hand out
            }
            BlockingWaiterRegistry.BlockedClient waiter = blockingWaiterRegistry.pollWaiter(key);
            if (waiter == null) {
                break;
            }
            writeAndFlush(waiter.ctx, respSerializer.serializeBlpopReply(key, popped));
        }
    }

    private void writeAndFlush(ChannelHandlerContext ctx, String s) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(s, StandardCharsets.UTF_8));
    }
}
