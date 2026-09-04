package Components.Commands;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public interface RedisCommand {
    void execute(ChannelHandlerContext ctx, String[] command);

    default void writeAndFlush(ChannelHandlerContext ctx, String s) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(s, StandardCharsets.UTF_8));
    }
}
