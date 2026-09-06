package redis.commands;

import redis.server.RedisConfig;
import redis.handler.RespSerializer;
import io.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

import static constants.CommandConstants.BULK_NULL;
import static constants.CommandConstants.INFO;

@Component(INFO)
public class Info implements RedisCommand {

    @Autowired
    RedisConfig redisConfig;

    @Autowired
    RespSerializer respSerializer;

    @Override
    public Optional<String> execute(ChannelHandlerContext ctx, String[] command, boolean isBlocking) {
        int replication = Arrays.stream(command).toList().indexOf("replication");
        if (replication > -1) {
            String role = "role:" + redisConfig.getRole();
            String masterReplId = "master_replid:" + redisConfig.getMasterReplId();
            String masterReplOffset = "master_repl_offset:" + redisConfig.getMasterReplOffset();

            String[] info = new String[]{role, masterReplId, masterReplOffset};

            String replicationData = String.join("\r\n", info);

            return Optional.of(respSerializer.serializeBulkString(replicationData));
        }

        return Optional.of(BULK_NULL);
    }
}
