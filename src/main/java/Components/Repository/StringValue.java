package Components.Repository;

import Components.Infrastructure.Type;

import java.time.LocalDateTime;

public class StringValue implements Value {
    public String val;
    public LocalDateTime created;
    public LocalDateTime expiry;

    public StringValue(String val, LocalDateTime created, LocalDateTime expiry) {
        this.val = val;
        this.created = created;
        this.expiry = expiry;
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }
}
