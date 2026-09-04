package Components.Service;

import Components.Commands.RedisCommand;
import Components.Repository.BlockingWaiterRegistry;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<List<String>> {

    private final Map<String, RedisCommand> commandMap;
    @Autowired
    public RespSerializer respSerializer;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    public CommandHandler(Map<String, RedisCommand> commandMap) {
        this.commandMap = commandMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<String> cmd) throws Exception {
        if (!cmd.isEmpty()) {
            execute(ctx, cmd.toArray(new String[0]));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // Client disconnected - if it was blocked on a BLPOP, stop tracking it
        // so we don't write to a closed channel later or leak it forever.
        blockingWaiterRegistry.removeWaiter(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        writeAndFlush(ctx, "-ERR " + cause.getMessage() + "\r\n");
    }

    private void execute(ChannelHandlerContext ctx, String[] command) {
        RedisCommand redisCommand = commandMap.get(command[0].toUpperCase());
        if (redisCommand != null) {
            redisCommand.execute(ctx, command);
        } else {
            unknown(ctx, command);
        }
    }

    private void unknown(ChannelHandlerContext ctx, String[] command) {
        StringBuilder res = new StringBuilder("ERR unknown command ");
        res.append("'").append(command[0]).append("'").append(", with args beginning with:");
        for (int i = 1; i < command.length; i++) {
            res.append(" ").append("'").append(command[i]).append("'");
        }
        writeAndFlush(ctx, respSerializer.serializeError(res.toString()));
    }

    private void writeAndFlush(ChannelHandlerContext ctx, String s) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(s, StandardCharsets.UTF_8));
    }

}
