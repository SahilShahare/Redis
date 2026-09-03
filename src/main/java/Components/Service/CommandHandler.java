package Components.Service;

import Components.Repository.BlockingWaiterRegistry;
import Components.Repository.ListStore;
import Components.Repository.Store;
import Components.Server.RedisConfig;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Constants.CommandConstants.BLPOP;
import static Constants.CommandConstants.BULK_NULL;
import static Constants.CommandConstants.ECHO;
import static Constants.CommandConstants.GET;
import static Constants.CommandConstants.INFO;
import static Constants.CommandConstants.LLEN;
import static Constants.CommandConstants.LPOP;
import static Constants.CommandConstants.LPUSH;
import static Constants.CommandConstants.LRANGE;
import static Constants.CommandConstants.PING;
import static Constants.CommandConstants.PONG;
import static Constants.CommandConstants.PX;
import static Constants.CommandConstants.RPUSH;
import static Constants.CommandConstants.SET;

@Component
@Slf4j
@ChannelHandler.Sharable
public class CommandHandler extends SimpleChannelInboundHandler<List<String>> {

    @Autowired
    public RespSerializer respSerializer;

    @Autowired
    public Store store;

    @Autowired
    public ListStore listStore;

    @Autowired
    public RedisConfig redisConfig;

    @Autowired
    BlockingWaiterRegistry blockingWaiterRegistry;

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
        switch (command[0].toUpperCase()) {
            case BLPOP -> blpop(ctx, command);
            case ECHO -> echo(ctx, command);
            case GET -> get(ctx, command);
            case INFO -> info(ctx, command);
            case LLEN -> llen(ctx, command);
            case LPOP -> lpop(ctx, command);
            case LPUSH -> lpush(ctx, command);
            case LRANGE -> lrange(ctx, command);
            case PING -> ping(ctx, command);
            case RPUSH -> rpush(ctx, command);
            case SET -> set(ctx, command);
            default -> unknown(ctx, command);
        }
    }

    private void blpop(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'blpop' command"));
            return;
        }

        String key = command[1];
        double timeoutSeconds;
        try {
            timeoutSeconds = Double.parseDouble(command[2]);
        } catch (NumberFormatException e) {
            writeAndFlush(ctx, respSerializer.serializeBulkString("ERR timeout is not a float or out of range"));
            return;
        }
        if (timeoutSeconds < 0) {
            writeAndFlush(ctx, respSerializer.serializeBulkString("ERR timeout is negative"));
            return;
        }

        Object popped = listStore.pollLeft(key);
        if (popped != null) {
            writeAndFlush(ctx, respSerializer.serializeBlpopReply(key, popped));
        } else {
            blockingWaiterRegistry.addWaiter(key, ctx, timeoutSeconds);
        }
    }

    private void echo(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 2) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'echo' command"));
            return;
        }
        writeAndFlush(ctx, respSerializer.serializeBulkString(command[1]));
    }

    private void ping(ChannelHandlerContext ctx, String[] command) {
        if (command.length == 1) {
            writeAndFlush(ctx, PONG);
        } else if (command.length == 2) {
            writeAndFlush(ctx, respSerializer.serializeBulkString(command[1]));
        } else {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'ping' command"));
        }
    }

    private void get(ChannelHandlerContext ctx, String[] command) {

        try {
            String key = command[1];
            writeAndFlush(ctx, store.get(key));
        } catch (Exception e) {
            writeAndFlush(ctx, BULK_NULL);
        }
    }

    private void info(ChannelHandlerContext ctx, String[] command) {
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

    private void llen(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 2) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'llen' command"));
        } else {
            String key = command[1];
            writeAndFlush(ctx, listStore.getSize(key));
        }
    }

    private void lpop(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 2 || command.length > 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'lpop' command"));
        } else {
            try {
                String key = command[1];
                int cnt = 1;
                if (command.length == 3) {
                    cnt = Integer.parseInt(command[2]);
                }
                if (cnt <= 0) {
                    writeAndFlush(ctx,
                            respSerializer.serializeBulkString("ERR value is out of range, must be positive"));

                } else {
                    writeAndFlush(ctx, listStore.removeLeft(key, cnt));
                }
            } catch (NumberFormatException e) {
                writeAndFlush(ctx,
                        respSerializer.serializeBulkString("ERR value is out of range, must be positive"));
            }
        }
    }

    private void lpush(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'rpush' command"));
        } else {
            String key = command[1];
            List<Object> values = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(command, 2, command.length)));
            writeAndFlush(ctx, listStore.putLeft(key, values));
            serveWaiters(key);
        }
    }

    private void lrange(ChannelHandlerContext ctx, String[] command) {
        if (command.length != 4) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'lrange' command"));
        } else {
            try {
                String key = command[1];
                int start = Integer.parseInt(command[2]);
                int end = Integer.parseInt(command[3]);
                writeAndFlush(ctx, listStore.get(key, start, end));
            } catch (NumberFormatException e) {
                writeAndFlush(ctx,
                        respSerializer.serializeBulkString("ERR value is not an integer or out of range"));
            }
        }
    }

    private void rpush(ChannelHandlerContext ctx, String[] command) {
        if (command.length < 3) {
            writeAndFlush(ctx,
                    respSerializer.serializeBulkString("ERR wrong number of arguments for 'rpush' command"));
        } else {
            String key = command[1];
            List<Object> values = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(command, 2, command.length)));
            writeAndFlush(ctx, listStore.putRight(key, values));
            serveWaiters(key);
        }
    }

    private void set(ChannelHandlerContext ctx, String[] command) {
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

    private void unknown(ChannelHandlerContext ctx, String[] command) {
        StringBuilder res = new StringBuilder("ERR unknown command ");
        res.append("'").append(command[0]).append("'").append(", with args beginning with:");
        for (int i = 1; i < command.length; i++) {
            res.append(" ").append("'").append(command[i]).append("'");
        }
        writeAndFlush(ctx, respSerializer.serializeBulkString(res.toString()));
    }

    private void writeAndFlush(ChannelHandlerContext ctx, String s) {
        ctx.writeAndFlush(Unpooled.copiedBuffer(s, StandardCharsets.UTF_8));
    }

    private void serveWaiters(String key) {
        while (blockingWaiterRegistry.hasWaiters(key)) {
            Object obj = listStore.getLeft(key);
            if (obj == null) {
                break; // nothing left to hand out
            }
            BlockingWaiterRegistry.BlockedClient waiter = blockingWaiterRegistry.pollWaiter(key);
            if (waiter == null) {
                break;
            }
            if (waiter.completed.compareAndSet(false, true)) {
                if (waiter.timeoutTask != null) waiter.timeoutTask.cancel(false);
                listStore.pollLeft(key);
                writeAndFlush(waiter.ctx, respSerializer.serializeBlpopReply(key, obj));
            }
        }
    }

}
