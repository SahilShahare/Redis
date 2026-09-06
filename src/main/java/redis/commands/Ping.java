package redis.commands;

import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.PING;
import static constants.CommandConstants.PONG;

@Component(PING)
public class Ping implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {

        if (command.length == 1) {
            return Optional.of(PONG);
        } else if (command.length == 2) {
            return Optional.of(respSerializer.serializeBulkString(command[1]));
        } else {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'ping' command"));
        }
    }
}
