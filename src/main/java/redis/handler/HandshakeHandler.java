package redis.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HandshakeHandler extends SimpleChannelInboundHandler<String> {

    private enum Step { AWAIT_PONG, AWAIT_REPLCONF_PORT_OK, AWAIT_REPLCONF_CAPA_OK, AWAIT_FULLRESYNC, DONE }

    private final int listeningPort;
    private Step step = Step.AWAIT_PONG;

    public HandshakeHandler(int listeningPort) {
        this.listeningPort = listeningPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        send(ctx, "PING");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String reply) {
        switch (step) {
            case AWAIT_PONG -> {
                step = Step.AWAIT_REPLCONF_PORT_OK;
                send(ctx, "REPLCONF", "listening-port", String.valueOf(listeningPort));
            }
            case AWAIT_REPLCONF_PORT_OK -> {
                step = Step.AWAIT_REPLCONF_CAPA_OK;
                send(ctx, "REPLCONF", "capa", "psync2");
            }
            case AWAIT_REPLCONF_CAPA_OK -> {
                step = Step.AWAIT_FULLRESYNC;
                send(ctx, "PSYNC", "?", "-1");
            }
            case AWAIT_FULLRESYNC -> {
                // +FULLRESYNC <REPL_ID> 0 - ignored for now, per the
                // spec; the RDB transfer that follows is later work.
                step = Step.DONE;
                log.info("Replication handshake with master complete");
            }
            case DONE -> {
            }
        }
    }

    private void send(ChannelHandlerContext ctx, String... parts) {
        StringBuilder resp = new StringBuilder();
        resp.append("*").append(parts.length).append("\r\n");
        for (String part : parts) {
            resp.append("$").append(part.length()).append("\r\n").append(part).append("\r\n");
        }
        ctx.writeAndFlush(resp.toString());
    }
}
