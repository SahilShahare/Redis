package redis.commands;

import redis.store.WatchStore;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.OK;
import static constants.CommandConstants.UNWATCH;

@Component(UNWATCH)
public class Unwatch implements RedisCommand {

    @Autowired
    WatchStore watchStore;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        watchStore.unwatch(ctx);
        return Optional.of(OK);
    }
}