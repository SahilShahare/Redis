package Components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Component
public class TcpServer {

    @Autowired
    private RespSerializer respSerializer;

    @Autowired
    private CommandHandler commandHandler;

    public void startServer(int port) {
        ServerSocket serverSocket = null;
        Socket clientSocket = null;

        try{
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            while(true){
                clientSocket = serverSocket.accept();
                Socket finalSocket = clientSocket;
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                final Client client = new Client(finalSocket, inputStream, outputStream);
                CompletableFuture.runAsync(()-> {
                    try{
                        handle(client);
                    } catch(IOException e) {
                        System.out.println("IOException: " + e.getMessage());
                    }
                });
            }

        } catch(IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try{
                if(clientSocket != null){
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());;
            }
        }
    }

    private void handle(final Client client) throws IOException{
        while(client.socket.isConnected()) {
            byte[] buffer = new byte[client.socket.getReceiveBufferSize()];
            int bytesRead = client.inputStream.read(buffer);
            if(bytesRead > 0){
                List<String[]> res = respSerializer.deserialize(buffer);
                for(String[] s : res) {
                    handleCommand(s, client);
                }
            }
        }
    }

    private void handleCommand(String[] command, Client client) throws IOException {
        String res = "";
        switch (command[0].toUpperCase()) {
            case "PING":
                res = commandHandler.ping(command);
                break;
            case "ECHO":
                res = commandHandler.echo(command);
                break;
            case "SET":
                res = commandHandler.set(command);
                break;
            case "GET":
                res = commandHandler.get(command);
                break;
        }
        if(res !=null && !res.isEmpty()){
            client.outputStream.write(res.getBytes(StandardCharsets.UTF_8));
        }
    }
}
