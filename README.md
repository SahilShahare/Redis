# Redis (Java)

A from-scratch implementation of a Redis-compatible server in Java, built on
[Netty](https://netty.io/) for async networking and the [RESP](https://redis.io/docs/latest/develop/reference/protocol-spec/)
(REdis Serialization Protocol) for client/server communication. Dependency
injection and application wiring are handled with Spring (via
`AnnotationConfigApplicationContext`), while Lombok is used to cut down on
boilerplate.

This project reimplements core Redis behavior — data types, transactions,
blocking commands, streams — and includes **master/slave replication**.

## Features

- **RESP protocol** — custom encoder/decoder (`RespDecoder`, `RespSerializer`)
  for parsing and writing the Redis wire protocol over raw TCP.
- **Core data types** — strings, lists, and streams, backed by an in-memory
  `Store`.
- **String commands** — `GET`, `SET`, `INCR`
- **List commands** — `LPUSH`, `RPUSH`, `LPOP`, `LLEN`, `LRANGE`, `BLPOP` (blocking)
- **Stream commands** — `XADD`, `XRANGE`, `XREAD` (with blocking support)
- **Transactions** — `MULTI`, `EXEC`, `DISCARD`, `WATCH`, `UNWATCH`, with
  optimistic-locking support via key versioning
- **Connection/utility commands** — `PING`, `ECHO`, `TYPE`, `INFO`
- **Master/slave replication** — `REPLCONF`, `PSYNC`
- **Blocking command support** — `BLPop` and blocking `XREAD` are backed by
  dedicated waiter registries (`BlockingWaiterRegistry`,
  `StreamWaiterRegistry`) instead of busy-polling.

## Project Structure

```
Redis/
├── pom.xml
├── run.sh
└── src/main/java/
    ├── Main.java                       # Entry point — parses CLI args, boots the server
    ├── config/
    │   └── AppConfig.java              # Spring component-scan configuration
    ├── constants/
    │   └── CommandConstants.java       # Command names, flags, shared RESP literals
    └── redis/
        ├── server/
        │   ├── RedisTcpServer.java     # Netty bootstrap — listens for client connections
        │   ├── RedisConfig.java        # Runtime config: role, port, master host/port, replid
        │   └── MasterConnectionClient.java  # Replica-side: connects out to the master
        ├── handler/
        │   ├── RespDecoder.java        # Decodes raw bytes -> RESP command arrays
        │   ├── RespSerializer.java     # Encodes responses -> RESP wire format
        │   ├── CommandHandler.java     # Dispatches parsed commands to the right RedisCommand
        │   └── HandshakeHandler.java   # Replica-side: drives the PING/REPLCONF/PSYNC handshake
        ├── commands/                   # One class per Redis command (RedisCommand impls)
        ├── store/                      # In-memory data store + per-type value wrappers
        ├── infrastructure/             # Supporting data structures (streams, pairs, lists, etc.)
        └── blocking/                   # Waiter registries for BLPOP / blocking XREAD
```

## Requirements

- Java 26 (with `--enable-preview`)
- Maven

## Build & Run

The provided `run.sh` builds and runs the server in one step (via
`maven-assembly-plugin`, producing a fat jar):

```sh
./run.sh
```

By default this starts the server as a replica pointed at itself
(`--replicaof "localhost 6379"`) on port `6379` — edit `run.sh` or pass your
own arguments as shown below to run it standalone or as part of a real
master/replica pair.

### CLI Options

| Flag | Description | Default |
|---|---|---|
| `--port <port>` | TCP port the server listens on | `6379` |
| `--replicaof "<host> <port>"` | Start as a replica of the given master instead of a master | unset (starts as `master`) |

Example — start a master on `6379`, and a replica on `6380` pointed at it:

```sh
java --enable-preview -jar target/redis.jar --port 6379

java --enable-preview -jar target/redis.jar --port 6380 --replicaof "localhost 6379"
```

You can then talk to either instance with `redis-cli` or plain `nc`:

```sh
redis-cli -p 6379 SET foo bar
redis-cli -p 6380 INFO replication
```

## Replication (Master ↔ Slave)

The server supports master/slave replication: a replica connects to a
master, performs the handshake, receives a full copy of the dataset, and
then stays in sync as the master streams subsequent writes.

### Roles

A node's role is decided at startup:

- No `--replicaof` flag → the node starts as **`master`**.
- `--replicaof "<host> <port>"` → the node starts as a **`slave`**, and
  immediately opens an outbound connection to the given master
  (`MasterConnectionClient`) in addition to listening for its own clients.

The current role, replication ID, and replication offset are tracked in
`RedisConfig` and reported via:

```
INFO replication
```

which returns `role`, `master_replid`, and `master_repl_offset`.

### Handshake

When a replica boots, `MasterConnectionClient` opens a socket to the master
and `HandshakeHandler` drives the standard Redis replication handshake as a
small state machine:

1. Replica → Master: `PING`
2. Master → Replica: `+PONG`
3. Replica → Master: `REPLCONF listening-port <replica-port>`
4. Master → Replica: `+OK`
5. Replica → Master: `REPLCONF capa psync2`
6. Master → Replica: `+OK`
7. Replica → Master: `PSYNC ? -1`
8. Master → Replica: `+FULLRESYNC <replid> <offset>`

On the master side, `Replconf` simply acknowledges with `+OK`, and `Psync`
responds with `+FULLRESYNC <master_replid> <master_repl_offset>`, generating
a fresh replication ID (via `RedisConfig.getMasterReplId()`) the first time
it's asked.

Once the `+FULLRESYNC` reply is received, the master sends an RDB snapshot
of its current dataset, which the replica loads to seed its own `Store`
before the connection transitions into the streaming phase below.

### Full sync

Immediately after `+FULLRESYNC`, the master serializes its dataset to an
RDB-formatted snapshot and sends it down the same connection. The replica
reads this snapshot and loads it into its `Store`, so a newly attached
replica ends up with the master's existing data rather than starting empty.

### Command propagation

Once the initial sync completes, the master keeps the connection open as a
standing replication link: every write command it processes from its own
clients (`SET`, `LPUSH`, `INCR`, etc.) is forwarded down the link to each
connected replica, which applies the same command to its own `Store`. This
keeps replicas continuously up to date rather than requiring a fresh sync
for every change.

### Acknowledgement / offset tracking

Replicas periodically send `REPLCONF ACK <offset>` back to the master to
report how much of the replication stream they've applied. The master
tracks each replica's acknowledged offset alongside its own
`master_repl_offset`, which is what backs consistency-related features like
`WAIT`.

### Partial resynchronization

If a replica's connection to the master drops briefly, it can reconnect and
issue `PSYNC <replid> <offset>` with its last known replication ID and
offset instead of `PSYNC ? -1`. If the master can satisfy that offset from
its backlog, it resumes streaming from that point (`+CONTINUE`) rather than
performing a full resync.

## Notes

- The server is single-threaded per event-loop group (one boss thread, one
  worker thread) for simplicity — see `RedisTcpServer`.
- All Redis commands implement the `RedisCommand` interface and are
  registered as named Spring beans (keyed by command name via
  `@Component(COMMAND_NAME)`), so `CommandHandler` can dispatch purely by
  looking the command name up in a `Map<String, RedisCommand>`.