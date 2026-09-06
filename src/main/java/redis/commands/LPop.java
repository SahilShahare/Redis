package redis.commands;

import redis.infrastructure.InvalidTypeException;
import redis.store.Store;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static constants.CommandConstants.BULK_NULL;
import static constants.CommandConstants.LPOP;
import static constants.CommandConstants.NULL_ARRAY;

@Component(LPOP)
public class LPop implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        if (command.length < 2 || command.length > 3) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'lpop' command"));
        } else {
            try {
                String key = command[1];
                int cnt = 1;
                if (command.length == 3) {
                    cnt = Integer.parseInt(command[2]);
                }
                if (cnt <= 0) {
                    return Optional.of(respSerializer.serializeError("ERR value is out of range, must be positive"));
                } else if (cnt == 1) {
                    Optional<Object> result = store.lpop(key);
                    return result.map(o -> respSerializer.serializeObject(o))
                            .or(() -> Optional.of(BULK_NULL));
                } else {
                    Optional<List<Object>> result = store.lpop(key, cnt);
                    return result.map(o -> respSerializer.serializeList(o))
                            .or(() -> Optional.of(NULL_ARRAY));
                }
            } catch (NumberFormatException e) {
                return Optional.of(respSerializer.serializeError("ERR value is out of range, must be positive"));
            } catch (InvalidTypeException e) {
                return Optional.of(respSerializer.serializeError(e.getMessage()));
            }
        }
    }
}
