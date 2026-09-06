package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.XRANGE;

@Component(XRANGE)
public class XRange implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length != 4) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'xrange' command"));
        }
        String key = command[1];
        String start = command[2];
        String end = command[3];
        try {
            return Optional.of(respSerializer.serializeStreamEntries(store.xrange(key, start, end)));
        } catch (InvalidTypeException e) {
            return Optional.of(respSerializer.serializeError(e.getMessage()));
        }
    }
}
