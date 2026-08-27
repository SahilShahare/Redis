package Components;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    Socket socket;
    InputStream inputStream;
    OutputStream outputStream;

    public Client(Socket socket, InputStream inputStream, OutputStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }
}
