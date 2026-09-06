package redis.commands;

import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.server.RedisConfig;

import java.util.Optional;

import static constants.CommandConstants.PSYNC;

@Component(PSYNC)
public class Psync implements RedisCommand {

    @Autowired
    RedisConfig redisConfig;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        String reply = "+FULLRESYNC " + redisConfig.getMasterReplId() + " " + redisConfig.getMasterReplOffset() + "\r\n";
        return Optional.of(reply);
    }
}
