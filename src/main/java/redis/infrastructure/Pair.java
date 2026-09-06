package redis.infrastructure;

public record Pair<K, V>(K key, V value) {}