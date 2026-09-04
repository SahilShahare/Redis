package Components.Commands;

import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.XRANGE;

@Component(XRANGE)
public class XRange implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 4) {
            writeAndFlush(ctx, respSerializer.serializeError("ERR wrong number of arguments for 'xrange' command"));
            return;
        }
        String key = command[1];
        String start = command[2];
        String end = command[3];
        writeAndFlush(ctx, store.xrange(key, start, end));
    }
}
