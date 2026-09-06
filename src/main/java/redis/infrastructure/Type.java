package redis.infrastructure;

public enum Type {
    STRING("string"),
    LIST("list"),
    STREAM("stream");

    private final String objectType;

    Type(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectType() {
        return this.objectType;
    }

}
