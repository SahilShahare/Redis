package redis.commands;

import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.TYPE;

@Component(TYPE)
public class Type implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length != 2) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'type' command"));
        } else {
            String key = command[1];
            return Optional.of(respSerializer.serializeSimpleString(store.type(key)));
        }
    }
}
