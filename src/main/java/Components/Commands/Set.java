package Components.Commands;

import Components.Repository.Store;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.PX;
import static Constants.CommandConstants.SET;

@Component(SET)
public class Set implements RedisCommand {

    @Autowired
    Store store;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        try {
            String key = command[1];
            String value = command[2];

            int pxFlag = Arrays.stream(command).map(String::toUpperCase).toList().indexOf(PX);

            if (pxFlag > -1) {
                int delta = Integer.parseInt(command[pxFlag + 1]);
                writeAndFlush(ctx, store.set(key, value, delta));
            } else {
                writeAndFlush(ctx, store.set(key, value));
            }

        } catch (Exception e) {
            writeAndFlush(ctx, BULK_NULL);
        }
    }
}
