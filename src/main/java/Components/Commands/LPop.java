package Components.Commands;

import Components.Repository.Store;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static Constants.CommandConstants.LPOP;

@Component(LPOP)
public class LPop implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 2 || command.length > 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'lpop' command"));
        } else {
            try {
                String key = command[1];
                int cnt = 1;
                if (command.length == 3) {
                    cnt = Integer.parseInt(command[2]);
                }
                if (cnt <= 0) {
                    writeAndFlush(ctx,
                            respSerializer.serializeError("ERR value is out of range, must be positive"));

                } else {
                    writeAndFlush(ctx, store.lpop(key, cnt));
                }
            } catch (NumberFormatException e) {
                writeAndFlush(ctx,
                        respSerializer.serializeError("ERR value is out of range, must be positive"));
            }
        }
    }
}
