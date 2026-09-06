package redis.commands;

import redis.store.TransactionStore;
import redis.store.WatchStore;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static constants.CommandConstants.DISCARD;
import static constants.CommandConstants.OK;

@Component(DISCARD)
public class Discard implements RedisCommand {

    @Autowired
    TransactionStore transactionStore;

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    WatchStore watchStore;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (!transactionStore.discard(ctx)) {
            return Optional.of(respSerializer.serializeError("ERR DISCARD without MULTI"));
        }
        watchStore.unwatch(ctx);
        return Optional.of(OK);
    }
}
