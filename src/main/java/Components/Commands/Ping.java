package Components.Commands;

import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.PING;
import static Constants.CommandConstants.PONG;

@Component(PING)
public class Ping implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length == 1) {
            writeAndFlush(ctx, PONG);
        } else if (command.length == 2) {
            writeAndFlush(ctx, respSerializer.serializeBulkString(command[1]));
        } else {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'ping' command"));
        }
    }
}
