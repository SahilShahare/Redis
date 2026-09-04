package Components.Repository;

import Components.Infrastructure.Type;
import Components.Infrastructure.Stream;

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
