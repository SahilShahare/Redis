package Components.Commands;

import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.TYPE;

@Component(TYPE)
public class Type implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 2) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'type' command"));
        } else {
            String key = command[1];
            writeAndFlush(ctx, store.type(key));
        }
    }
}
