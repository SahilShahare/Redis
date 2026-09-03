package Components.Server;

import Components.Service.CommandHandler;
import Components.Service.RespDecoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class MasterTcpServer {

    @Autowired
    RedisConfig redisConfig;

    @Autowired
    CommandHandler commandHandler;


    public void startServer() throws InterruptedException {

        //Boss group: only ever calls accept() on the listening socket
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        //Worker group: this is where channelRead0() actually runs for every client.
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);

        int port = redisConfig.getPort();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new RespDecoder()).addLast(commandHandler);
                }
            });

            ChannelFuture future = bootstrap.bind(port).sync();
            log.info(String.format("Listening on port %d (single-threaded worker event loop)", port));
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}
