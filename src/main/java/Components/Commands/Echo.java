package Components.Commands;

import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.ECHO;

@Component(ECHO)
public class Echo implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 2) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'echo' command"));
            return;
        }
        writeAndFlush(ctx, respSerializer.serializeBulkString(command[1]));
    }
}
