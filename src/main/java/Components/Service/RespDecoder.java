package Components.Service;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespDecoder extends ByteToMessageDecoder {

    private static final RuntimeException NEED_MORE_DATA = new RuntimeException("incomplete") {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    };

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        try {
            List<List<String>> commands = readTopLevel(in);
            out.addAll(commands);
        } catch (RuntimeException e) {
            if (e == NEED_MORE_DATA) {
                in.resetReaderIndex(); // rewind; wait for the rest, Netty will call decode() again
            } else {
                throw e; // genuine protocol error - propagates to exceptionCaught
            }
        }
    }

    private List<List<String>> readTopLevel(ByteBuf in) {
        expect(in, '*');
        int count = (int) readLong(in);
        if (peek(in) == '*') {
            // Multiple Commands: Ex. *2\r\n*3\r\n$3\r\nset\r\n$3\r\nkey\r\n$5\r\nvalue\r\n*2\r\n$3\r\nget\r\n$3\r\nkey\r\n
            List<List<String>> commands = new ArrayList<>(count);
            for (int c = 0; c < count; c++) {
                commands.add(readNestedCommand(in));
            }
            return commands;
        } else {
            // Single Commands: *3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n
            return List.of(readFlatCommand(in, count));
        }
    }

    private List<String> readNestedCommand(ByteBuf in) {
        expect(in, '*');
        int argc = (int) readLong(in);
        return readFlatCommand(in, argc);
    }

    private List<String> readFlatCommand(ByteBuf in, int argc) {
        List<String> args = new ArrayList<>(argc);
        for (int i = 0; i < argc; i++) {
            args.add(readBulkString(in));
        }
        return args;
    }

    private String readBulkString(ByteBuf in) {
        expect(in, '$');
        int len = (int) readLong(in);
        if (in.readableBytes() < len + 2) throw NEED_MORE_DATA; // payload + trailing \r\n
        byte[] data = new byte[len];
        in.readBytes(data);
        readByte(in); // \r
        readByte(in); // \n
        return new String(data, StandardCharsets.UTF_8);
    }

    private void expect(ByteBuf in, char marker) {
        byte b = readByte(in);
        if (b != marker) {
            throw new IllegalStateException("Protocol error: expected '" + marker + "' but got '" + (char) b + "'");
        }
    }

    /** Looks at the next byte without consuming it. */
    private char peek(ByteBuf in) {
        if (!in.isReadable()) throw NEED_MORE_DATA;
        return (char) in.getByte(in.readerIndex());
    }

    private byte readByte(ByteBuf in) {
        if (!in.isReadable()) throw NEED_MORE_DATA;
        return in.readByte();
    }

    private long readLong(ByteBuf in) {
        return Long.parseLong(readLine(in));
    }

    private String readLine(ByteBuf in) {
        int lf = in.indexOf(in.readerIndex(), in.writerIndex(), (byte) '\n');
        if (lf < 0) throw NEED_MORE_DATA;
        int len = lf - in.readerIndex();
        String line = in.toString(in.readerIndex(), len, StandardCharsets.UTF_8);
        in.readerIndex(lf + 1);
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }
}
