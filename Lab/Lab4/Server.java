import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 12345;
    
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Step 1: Create ServerSocket (handshaking socket)
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for client connections...");
            
            while (true) {
                // Step 2: Create plain Socket (communication socket) that accepts client requests
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                
                // Step 3: Create new instance of ClientHandler thread
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                
                // Step 4: Start the thread
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}

// Custom Thread class for handling client connections
class ClientHandler extends Thread {
    private final Socket clientSocket;
    
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }
    
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
             DataOutputStream dataOutput = new DataOutputStream(clientSocket.getOutputStream())) {
            
            // Step 6: Send prompt to client
            writer.write("Enter the file name you want to download:");
            writer.newLine();
            writer.flush();
            
            // Step 7: Read file name from client
            String fileName = reader.readLine();
            System.out.println("Client requested file: " + fileName);
            
            // Step 8: Check if file exists on server
            File file = new File(fileName);
            
            if (file.exists() && file.isFile()) {
                // Step 9: File found
                // Step 9a: Send confirmation to client
                writer.write("FILE_FOUND");
                writer.newLine();
                writer.flush();
                
                // Step 9b: Send file size to client
                long fileSize = file.length();
                dataOutput.writeLong(fileSize);
                dataOutput.flush();
                System.out.println("Sending file: " + fileName + " (Size: " + fileSize + " bytes)");
                
                // Step 9c & 9d: Open FileInputStream and send file content in chunks
                try (FileInputStream fileInput = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    
                    // Read file content in chunks and send to client
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        // Send the bytes read from file to client through network stream
                        dataOutput.write(buffer, 0, bytesRead);
                    }
                    // Ensure all buffered data is sent immediately to client
                    dataOutput.flush();
                }
                
                System.out.println("File sent successfully: " + fileName);
            } else {
                // Step 10: File not found
                writer.write("NOT_FOUND");
                writer.newLine();
                writer.flush();
                System.out.println("File not found: " + fileName);
            }
            
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            // Step 11: Close client socket connection
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                System.out.println("Client connection closed.");
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
