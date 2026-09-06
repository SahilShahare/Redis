package redis.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.handler.HandshakeHandler;
import redis.server.RedisConfig;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class MasterConnectionClient {

    @Autowired
    RedisConfig redisConfig;

    public void connect(EventLoopGroup workerGroup) {
        String host = redisConfig.getMasterHost();
        int port = redisConfig.getMasterPort();
        int listeningPort = redisConfig.getPort();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new LineBasedFrameDecoder(512))
                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                .addLast(new HandshakeHandler(listeningPort));
                    }
                });

        try {
            bootstrap.connect(host, port).sync();
            log.info("Connecting to master at {}:{}", host, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while connecting to master at {}:{}", host, port, e);
        }
    }

}