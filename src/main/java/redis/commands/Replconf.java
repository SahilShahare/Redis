package redis.commands;

import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.OK;
import static constants.CommandConstants.REPLCONF;

@Component(REPLCONF)
public class Replconf implements RedisCommand {
    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        return Optional.of(OK);
    }
}
