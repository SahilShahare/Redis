package Components.Commands;

import Components.Repository.Store;
import Components.Repository.StreamWaiterRegistry;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static Constants.CommandConstants.BLOCK;
import static Constants.CommandConstants.NULL_ARRAY;
import static Constants.CommandConstants.STREAMS;
import static Constants.CommandConstants.XREAD;

@Component(XREAD)
public class XRead implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    StreamWaiterRegistry streamWaiterRegistry;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        int idx = 1;
        long blockMillis = -1; // -1 = no BLOCK option given (plain, non-blocking read)

        if (idx < command.length && BLOCK.equalsIgnoreCase(command[idx])) {
            if (idx + 1 >= command.length) {
                writeAndFlush(ctx, respSerializer.serializeError("ERR syntax error"));
                return;
            }
            try {
                blockMillis = Long.parseLong(command[idx + 1]);
            } catch (NumberFormatException e) {
                writeAndFlush(ctx,
                        respSerializer.serializeError("ERR timeout is not an integer or out of range"));
                return;
            }
            if (blockMillis < 0) {
                writeAndFlush(ctx, respSerializer.serializeError("ERR timeout is negative"));
                return;
            }
            idx += 2;
        }

        if (idx >= command.length || !STREAMS.equalsIgnoreCase(command[idx])) {
            writeAndFlush(ctx, respSerializer.serializeError("ERR wrong number of arguments for 'xread' command"));
            return;
        }
        idx++; // first key starts here

        int remaining = command.length - idx;
        if (remaining == 0 || remaining % 2 != 0) {
            writeAndFlush(ctx, respSerializer.serializeError(
                    "ERR Unbalanced XREAD list of streams: for each stream key an ID or '$' must be specified."));
            return;
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

        String immediate = store.xread(keys, ids);
        if (blockMillis < 0 || !NULL_ARRAY.equals(immediate)) {
            // Either a plain (non-blocking) call, or data was already there.
            writeAndFlush(ctx, immediate);
            return;
        }

        streamWaiterRegistry.addWaiter(keys, ids, ctx, blockMillis);
    }
}
