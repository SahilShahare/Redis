package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.LLEN;

@Component(LLEN)
public class LLen implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {

        if (command.length != 2) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'llen' command"));
        } else {
            String key = command[1];
            try {
                return Optional.of(respSerializer.serializeInteger(store.llen(key)));
            } catch (InvalidTypeException e) {
                return Optional.of(respSerializer.serializeError(e.getMessage()));
            }
        }
    }
}
