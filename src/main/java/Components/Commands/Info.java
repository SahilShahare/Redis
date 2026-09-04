package Components.Commands;

import Components.Server.RedisConfig;
import Components.Service.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.INFO;

@Component(INFO)
public class Info implements RedisCommand {

    @Autowired
    RedisConfig redisConfig;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public void execute(ChannelHandlerContext ctx, String[] command) {
        int replication = Arrays.stream(command).toList().indexOf("replication");
        if (replication > -1) {
            String role = "role:" + redisConfig.getRole();
            String masterReplId = "master_replid:" + redisConfig.getMasterReplId();
            String masterReplOffset = "master_repl_offset:" + redisConfig.getMasterReplOffset();

            String[] info = new String[]{role, masterReplId, masterReplOffset};

            String replicationData = String.join("\r\n", info);

            writeAndFlush(ctx, respSerializer.serializeBulkString(replicationData));
            return;
        }

        writeAndFlush(ctx, BULK_NULL);
    }
}
