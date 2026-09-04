package Components.Repository;

import Components.Infrastructure.DoubleEndedList;
import Components.Infrastructure.Type;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static Constants.CommandConstants.BULK_NULL;

@Slf4j
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
