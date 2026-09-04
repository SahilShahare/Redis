package Components.Commands;

import Components.Repository.Store;
import Components.Repository.StreamWaiterRegistry;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

import static Constants.CommandConstants.XADD;

@Component(XADD)
public class XAdd implements RedisCommand {

    @Autowired
    RespSerializer respSerializer;

    @Autowired
    Store store;

    @Autowired
    StreamWaiterRegistry streamWaiterRegistry;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 5 || (command.length - 3) % 2 == 1) {
            writeAndFlush(ctx,
                    respSerializer.serializeError("ERR wrong number of arguments for 'xadd' command"));
        } else {
            String key = command[1];
            String idSpec = command[2];
            LinkedHashMap<String, String> fields = new LinkedHashMap<>();
            for (int i = 3; i < command.length; i += 2) {
                fields.put(command[i], command[i + 1]);
            }
            String result = store.xadd(key, idSpec, fields);
            if (result.startsWith("$")) {
                // Success (a RESP bulk string always starts with '$')
                streamWaiterRegistry.notifyPush(key, store::xread);
            }

            writeAndFlush(ctx, result);
        }
    }
}
