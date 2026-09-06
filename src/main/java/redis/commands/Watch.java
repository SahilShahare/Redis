package redis.commands;

import redis.store.TransactionStore;
import redis.store.WatchStore;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static constants.CommandConstants.OK;
import static constants.CommandConstants.WATCH;

@Component(WATCH)
public class Watch implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    TransactionStore transactionStore;

    @Autowired
    WatchStore watchStore;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (transactionStore.isInTransaction(ctx)) {
            return Optional.of(respSerializer.serializeError("ERR WATCH inside MULTI is not allowed"));
        }
        if (command.length < 2) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'watch' command"));
        }
        watchStore.watch(ctx, Arrays.asList(command).subList(1, command.length));
        return Optional.of(OK);
    }
}