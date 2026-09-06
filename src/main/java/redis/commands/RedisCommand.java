package redis.commands;

import io.netty.channel.ChannelHandlerContext;

import java.util.Optional;

public interface RedisCommand {
    Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking);
}
