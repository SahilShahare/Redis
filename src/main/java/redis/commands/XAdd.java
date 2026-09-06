package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.infrastructure.Pair;
import redis.infrastructure.StreamEntry;
import redis.store.Store;
import redis.blocking.StreamWaiterRegistry;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static constants.CommandConstants.XADD;

@Component(XADD)
public class XAdd implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    StreamWaiterRegistry streamWaiterRegistry;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length < 5 || (command.length - 3) % 2 == 1) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'xadd' command"));
        } else {
            String key = command[1];
            String idSpec = command[2];
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            for (int i = 3; i < command.length; i += 2) {
                fields.put(command[i], command[i + 1]);
            }
            try {
                String result = store.xadd(key, idSpec, fields);
                streamWaiterRegistry.notifyPush(key, this::read);
                return Optional.of(respSerializer.serializeBulkString(result));
            } catch (IllegalArgumentException | InvalidTypeException e) {
                return Optional.of(respSerializer.serializeError(e.getMessage()));
            }
        }
    }

    private String read(List<String> keys, List<String> ids) {
        try {
            List<Pair<String, List<StreamEntry>>> result = store.xread(keys, ids);
            return respSerializer.serializeStreamReadEntryList(result);
        } catch (InvalidTypeException e) {
            return respSerializer.serializeError(e.getMessage());
        }
    }
}
