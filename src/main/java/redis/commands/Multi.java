package redis.commands;

import redis.store.TransactionStore;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.Optional;

import static constants.CommandConstants.MULTI;
import static constants.CommandConstants.OK;

@Component(MULTI)
public class Multi implements RedisCommand {

    @Autowired
    TransactionStore transactionStore;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (!transactionStore.start(ctx)) {
            return Optional.of(respSerializer.serializeError("ERR MULTI calls can not be nested"));
        }
        return Optional.of(OK);
    }
}
