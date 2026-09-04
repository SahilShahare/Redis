package Components.Commands;

import Components.Repository.BlockingWaiterRegistry;
import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.BLPOP;

@Component(BLPOP)
public class BLPop implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'blpop' command"));
            return;
        }

        String key = command[1];
        double timeoutSeconds;
        try {
            timeoutSeconds = Double.parseDouble(command[2]);
        } catch (NumberFormatException e) {
            writeAndFlush(ctx, respSerializer.serializeError("ERR timeout is not a float or out of range"));
            return;
        }
        if (timeoutSeconds < 0) {
            writeAndFlush(ctx, respSerializer.serializeError("ERR timeout is negative"));
            return;
        }

        Object popped = store.pollListLeft(key);
        if (popped != null) {
            writeAndFlush(ctx, respSerializer.serializeBlpopReply(key, popped));
        } else {
            blockingWaiterRegistry.addWaiter(key, ctx, timeoutSeconds);
        }
    }
}
