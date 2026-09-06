package redis.commands;

import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.ECHO;

@Component(ECHO)
public class Echo implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {

        if (command.length != 2) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'echo' command"));
        }
        return Optional.of(respSerializer.serializeBulkString(command[1]));
    }
}
