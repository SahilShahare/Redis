package Constants;

public class CommandConstants {
    private CommandConstants() {};

    //Commands
    public static final String BLPOP = "BLPOP";
    public static final String ECHO = "ECHO";
    public static final String GET = "GET";
    public static final String INFO = "INFO";
    public static final String LLEN = "LLEN";
    public static final String LPOP = "LPOP";
    public static final String LPUSH = "LPUSH";
    public static final String LRANGE = "LRANGE";
    public static  final String PING = "PING";
    public static final String RPUSH = "RPUSH";
    public static final String SET = "SET";
    public static final String TYPE = "TYPE";
    public static final String XADD = "XADD";
    public static final String XRANGE = "XRANGE";
    public static final String XREAD = "XREAD";

    //Flags
    public static final String BLOCK = "BLOCK";
    public static final String PX = "PX";
    public static final String STREAMS = "STREAMS";

    //Common Strings
    public static final String BULK_NULL = "$-1\r\n";
    public static final String NULL_ARRAY = "*-1\r\n";
    public static final String PONG = "+PONG\r\n";
    public static final String OK = "+OK\r\n";

}
