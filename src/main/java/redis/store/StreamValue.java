package redis.store;

import redis.infrastructure.Type;
import redis.infrastructure.Stream;

public class StreamValue implements Value{

    Stream stream;

    public StreamValue() {
        this.stream = new Stream();
    }

    @Override
    public Type getType() {
        return Type.STREAM;
    }
}
