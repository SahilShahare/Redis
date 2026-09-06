package redis.store;

import io.netty.channel.ChannelHandlerContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionStore {

    private final Map<ChannelHandlerContext, List<String[]>> transactions = new HashMap<>();

    public boolean start(ChannelHandlerContext ctx) {
        if (transactions.containsKey(ctx)) {
            return false;
        }
        transactions.put(ctx, new ArrayList<>());
        return true;
    }

    public boolean isInTransaction(ChannelHandlerContext ctx) {
        return transactions.containsKey(ctx);
    }

    public void queue(ChannelHandlerContext ctx, String[] command) {
        transactions.get(ctx).add(command);
    }

    public List<String[]> end(ChannelHandlerContext ctx) {
        return transactions.remove(ctx);
    }

    public boolean discard(ChannelHandlerContext ctx) {
        return transactions.remove(ctx) != null;
    }


    public void removeConnection(ChannelHandlerContext ctx) {
        transactions.remove(ctx);
    }
}