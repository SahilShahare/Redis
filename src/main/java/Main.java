import redis.server.RedisConfig;
import redis.server.RedisTcpServer;
import config.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        RedisTcpServer server = context.getBean(RedisTcpServer.class);

        RedisConfig redisConfig = context.getBean(RedisConfig.class);

        redisConfig.setPort(6379);
        redisConfig.setRole("master");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--port requires a value");
                    }
                    redisConfig.setPort(Integer.parseInt(args[++i]));
                    break;
                case "--replicaof":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("--replicaof requires a value");
                    }
                    String[] hostAndPort = args[++i].split(" ");
                    if (hostAndPort.length < 2) {
                        throw new IllegalArgumentException("--replicaof requires \"<host> <port>\"");
                    }
                    redisConfig.setRole("slave");
                    redisConfig.setMasterHost(hostAndPort[0]);
                    redisConfig.setMasterPort(Integer.parseInt(hostAndPort[1]));
            }
        }

        server.startServer();
    }
}
