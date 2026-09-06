package redis.commands;

import redis.blocking.BlockingWaiterRegistry;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.BLPOP;

@Component(BLPOP)
public class BLPop implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {

        if (command.length < 3) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'blpop' command"));
        }

        String key = command[1];
        double timeoutSeconds;
        try {
            timeoutSeconds = Double.parseDouble(command[2]);
        } catch (NumberFormatException e) {
            return Optional.of(respSerializer.serializeError("ERR timeout is not a float or out of range"));
        }
        if (timeoutSeconds < 0) {
            return Optional.of(respSerializer.serializeError("ERR timeout is negative"));
        }

        Object popped = store.pollListLeft(key);
        if (popped != null) {
            return Optional.of(respSerializer.serializeBlpopReply(key, popped));
        }
        if (isBlocking) {
            blockingWaiterRegistry.addWaiter(key, ctx, timeoutSeconds);
        }
        return Optional.empty();
    }
}
