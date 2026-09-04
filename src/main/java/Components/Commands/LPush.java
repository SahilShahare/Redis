package Components.Commands;

import Components.Repository.BlockingWaiterRegistry;
import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Constants.CommandConstants.LPUSH;

@Component(LPUSH)
public class LPush implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'rpush' command"));
        } else {
            String key = command[1];
            List<Object> values = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(command, 2, command.length)));
            writeAndFlush(ctx, store.lpush(key, values));
            serveWaiters(key);
        }
    }

    private void serveWaiters(String key) {
        while (blockingWaiterRegistry.hasWaiters(key)) {
            Object popped = store.pollListLeft(key);
            if (popped == null) {
                break; // nothing left to hand out
            }
            BlockingWaiterRegistry.BlockedClient waiter = blockingWaiterRegistry.pollWaiter(key);
            if (waiter == null) {
                break;
            }
            writeAndFlush(waiter.ctx, respSerializer.serializeBlpopReply(key, popped));
        }
    }
}
