package Components.Commands;

import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.LRANGE;

@Component(LRANGE)
public class LRange implements RedisCommand{

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 4) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'lrange' command"));
        } else {
            try {
                String key = command[1];
                int start = Integer.parseInt(command[2]);
                int end = Integer.parseInt(command[3]);
                writeAndFlush(ctx, store.lrange(key, start, end));
            } catch (NumberFormatException e) {
                writeAndFlush(ctx,
                        respSerializer.serializeError("ERR value is not an integer or out of range"));
            }
        }
    }
}
