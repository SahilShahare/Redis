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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static constants.CommandConstants.BLOCK;
import static constants.CommandConstants.STREAMS;
import static constants.CommandConstants.XREAD;

@Component(XREAD)
public class XRead implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    StreamWaiterRegistry streamWaiterRegistry;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        int idx = 1;
        long blockMillis = -1; // -1 = no BLOCK option given (plain, non-blocking read)

        if (idx < command.length && BLOCK.equalsIgnoreCase(command[idx])) {
            if (idx + 1 >= command.length) {
                return Optional.of(respSerializer.serializeError("ERR syntax error"));
            }
            try {
                blockMillis = Long.parseLong(command[idx + 1]);
            } catch (NumberFormatException e) {
                return Optional.of(respSerializer.serializeError("ERR timeout is not an integer or out of range"));
            }
            if (blockMillis < 0) {
                return Optional.of(respSerializer.serializeError("ERR timeout is negative"));
            }
            idx += 2;
        }

        if (idx >= command.length || !STREAMS.equalsIgnoreCase(command[idx])) {
            return Optional.of(respSerializer.serializeError("ERR wrong number of arguments for 'xread' command"));
        }
        idx++; // first key starts here

        int remaining = command.length - idx;
        if (remaining == 0 || remaining % 2 != 0) {
            return Optional.of(respSerializer.serializeError(
                    "ERR Unbalanced XREAD list of streams: for each stream key an ID or '$' must be specified."));
        }
        int n = remaining / 2;
        List<String> keys = new ArrayList<>(n);
        List<String> ids = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String key = command[idx + i];
            String rawId = command[idx + n + i];
            String resolvedId = "$".equals(rawId) ? store.lastId(key).toString() : rawId;
            keys.add(key);
            ids.add(resolvedId);
        }

        try {
            List<Pair<String, List<StreamEntry>>> immediate = store.xread(keys, ids);
            if (blockMillis < 0 || !immediate.isEmpty()) {
                // Either a plain (non-blocking) call, or data was already there.
                return Optional.of(respSerializer.serializeStreamReadEntryList(immediate));
            }
        } catch (InvalidTypeException e) {
            return Optional.of(respSerializer.serializeError(e.getMessage()));
        }

        if (isBlocking) {
            streamWaiterRegistry.addWaiter(keys, ids, ctx, blockMillis);
        }
        return Optional.empty();
    }
}
