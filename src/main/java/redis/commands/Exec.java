package redis.commands;

import redis.store.TransactionStore;
import redis.store.WatchStore;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static constants.CommandConstants.EXEC;
import static constants.CommandConstants.NULL_ARRAY;

@Component(EXEC)
public class Exec implements RedisCommand {

    @Autowired
    private Map<String, RedisCommand> commandMap;

    @Autowired
    TransactionStore transactionStore;

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    WatchStore watchStore;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        List<String[]> queued = transactionStore.end(ctx);
        if (queued == null) {
            return Optional.of(respSerializer.serializeError("ERR EXEC without MULTI"));
        }

        boolean dirty = watchStore.isDirty(ctx);
        watchStore.unwatch(ctx);

        if (dirty) {
            return Optional.of(NULL_ARRAY);
        }

        StringBuilder res = new StringBuilder("*").append(queued.size()).append("\r\n");
        for (String[] cmd : queued) {
            String result;
            try {
                result = execute(ctx, cmd);
            } catch (Exception e) {

                result = respSerializer.serializeError("ERR " + e.getMessage());
            }
            res.append(result);
        }
        return Optional.of(res.toString());
    }

    private String execute(ChannelHandlerContext ctx, String[] command) {
        RedisCommand redisCommand = commandMap.get(command[0].toUpperCase());
        if (redisCommand != null) {
            Optional<String> result = redisCommand.execute(ctx, command, false);
            return result.orElse(NULL_ARRAY);
        } else {
            StringBuilder res = new StringBuilder("ERR unknown command ");
            res.append("'").append(command[0]).append("'").append(", with args beginning with:");
            for (int i = 1; i < command.length; i++) {
                res.append(" ").append("'").append(command[i]).append("'");
            }
            return respSerializer.serializeError(res.toString());
        }
    }

}
