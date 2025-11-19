import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             DataInputStream dataInput = new DataInputStream(socket.getInputStream())) {
            
            // Step 1: Socket connection established
            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);
            
            // Step 2: Receive server prompt asking for file name
            String serverPrompt = reader.readLine();
            System.out.println("Server: " + serverPrompt);
            
            // Step 3: Input desired file name and send it to server
            System.out.print("Enter filename: ");
            String fileName = scanner.nextLine();
            
            // Send the requested filename to the server
            writer.write(fileName);
            // Add newline character to indicate end of filename
            writer.newLine();
            // Force send the data immediately to the server
            writer.flush();
            
            // Read server response
            String response = reader.readLine();
            System.out.println("Server response: " + response);
            
            if (response.equals("FILE_FOUND")) {
                // Step 4: File found on server
                // Step 4a: Read file size
                long fileSize = dataInput.readLong();
                System.out.println("File size: " + fileSize + " bytes");
                
                // Step 4b: Prepare to save file locally as downloaded_<filename>
                String downloadFileName = "downloaded_" + fileName;
                File downloadFile = new File(downloadFileName);
                
                try (FileOutputStream fileOutput = new FileOutputStream(downloadFile)) {
                    // Step 4c: Read file content from socket and save locally
                    byte[] buffer = new byte[4096];
                    long totalBytesRead = 0;
                    int bytesRead;
                    
                    System.out.println("Downloading file...");
                    
                    // Download file in chunks until all bytes are received
                    while (totalBytesRead < fileSize) {
                        // Read data from server into buffer, limiting read size to remaining bytes
                        bytesRead = dataInput.read(buffer, 0, 
                            (int) Math.min(buffer.length, fileSize - totalBytesRead));
                        
                        // Check if end of stream is reached unexpectedly
                        if (bytesRead == -1) {
                            break;
                        }
                        
                        // Write the received bytes to the local file
                        fileOutput.write(buffer, 0, bytesRead);
                        // Update total bytes downloaded counter
                        totalBytesRead += bytesRead;
                        
                        // Show download progress as percentage
                        double progress = (double) totalBytesRead / fileSize * 100;
                        System.out.printf("\rProgress: %.2f%%", progress);
                    }
                    
                    System.out.println("\nFile downloaded successfully as: " + downloadFileName);
                }
            } else if (response.equals("NOT_FOUND")) {
                // Step 5: File not found
                System.out.println("Error: The file '" + fileName + "' doesn't exist on the server.");
            } else {
                System.out.println("Unknown response from server: " + response);
            }
            
            // Step 6: Connections will be automatically closed by try-with-resources
            System.out.println("Connection closed. Exiting.");
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}
