package Components.Server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisConfig {

    @Getter
    @Setter
    private String role;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private String masterHost;

    @Getter
    @Setter
    private int masterPort;

    private String masterReplId;

    private Long masterReplOffset;

    public String getMasterReplId() {
        if(this.masterReplId == null) {
            this.masterReplId = UUID.randomUUID().toString().replace("-", "")+
                    UUID.randomUUID().toString().replace("-", "").substring(0,8);
        }
        return this.masterReplId;
    }

    public Long getMasterReplOffset() {
        if(this.masterReplOffset == null) {
            this.masterReplOffset = 0L;
        }
        return this.masterReplOffset;
    }

}
