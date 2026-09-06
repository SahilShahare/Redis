package redis.store;

import redis.infrastructure.DoubleEndedList;
import redis.infrastructure.Type;

public class ListValue implements Value{

    public DoubleEndedList<Object> list;

    public ListValue() {
        this.list =  new DoubleEndedList<>();
    }

    @Override
    public Type getType() {
        return Type.LIST;
    }
}
