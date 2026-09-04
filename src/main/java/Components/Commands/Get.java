package Components.Commands;

import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.GET;

@Component(GET)
public class Get implements RedisCommand {

    @Autowired
    Store store;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length > 2) {
            writeAndFlush(ctx, respSerializer.serializeError("ERR wrong number of arguments for 'get' command"));
            return;
        }
        String key = command[1];
        writeAndFlush(ctx, store.get(key));
    }
}
