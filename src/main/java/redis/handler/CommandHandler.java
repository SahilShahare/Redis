package redis.handler;

import redis.commands.RedisCommand;
import redis.blocking.BlockingWaiterRegistry;
import redis.blocking.StreamWaiterRegistry;
import redis.store.TransactionStore;
import redis.store.WatchStore;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static constants.CommandConstants.QUEUED;
import static constants.CommandConstants.TRANSACTION_CONTROL_COMMANDS;

@Component
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<List<String>> {

    @Autowired
    public RespSerializer respSerializer;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

    @Autowired
    StreamWaiterRegistry streamWaiterRegistry;

    @Autowired
    TransactionStore transactionStore;

    @Autowired
    WatchStore watchStore;

    @Autowired
    private Map<String, RedisCommand> commandMap;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, List<String> cmd) throws Exception {
        if (!cmd.isEmpty()) {
            execute(ctx, cmd.toArray(new String[0]));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        blockingWaiterRegistry.removeWaiter(ctx);
        streamWaiterRegistry.removeWaiter(ctx);
        transactionStore.removeConnection(ctx);
        watchStore.unwatch(ctx);
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        writeAndFlush(ctx, "-ERR " + cause.getMessage() + "\r\n");
    }

    private void execute(ChannelHandlerContext ctx, String[] command) {
        String name = command[0].toUpperCase();
        RedisCommand redisCommand = commandMap.get(name);
        if (redisCommand == null) {
            unknown(ctx, command);
            return;
        }
        if (transactionStore.isInTransaction(ctx) && !TRANSACTION_CONTROL_COMMANDS.contains(name)) {
            transactionStore.queue(ctx, command);
            writeAndFlush(ctx, QUEUED);
            return;
        }
        Optional<String> result = redisCommand.execute(ctx, command, true);
        result.ifPresent(s -> writeAndFlush(ctx, s));
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
