package Components.Infrastructure;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
@Slf4j
public class Client {
    public Socket socket;
    public InputStream inputStream;
    public OutputStream outputStream;

    public Client(Socket socket, InputStream inputStream, OutputStream outputStream) {
        this.socket = socket;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void close() {
        try{
            if(socket != null) {
                socket.close();
            }
            if(inputStream != null) {
                inputStream.close();
            }
            if(outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
