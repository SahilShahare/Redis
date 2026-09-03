import Components.Server.RedisConfig;
import Components.Server.MasterTcpServer;
import Config.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


public class Main {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        MasterTcpServer master = context.getBean(MasterTcpServer.class);

        RedisConfig redisConfig = context.getBean(RedisConfig.class);

        redisConfig.setPort(6379);
        redisConfig.setRole("master");
        for(int i=0;i<args.length; i++){
            switch(args[i]){
                case "--port":
                    int port = Integer.parseInt(args[i+1]);
                    redisConfig.setPort(port);
                    break;
                case "--replicaof":
                    redisConfig.setRole("slave");
                    String masterHost = args[i+1].split(" ")[0];
                    int masterPort = Integer.parseInt(args[i+1].split(" ")[1]);
                    redisConfig.setMasterHost(masterHost);
                    redisConfig.setMasterPort(masterPort);
            }
        }

        if(redisConfig.getRole().equals("master")) {
            master.startServer();
        }
    }
}