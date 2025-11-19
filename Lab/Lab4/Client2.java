import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client2 {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = "http://" + SERVER_IP + ":" + SERVER_PORT;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=== HTTP File Client ===");
            System.out.println("1. Upload File (POST)");
            System.out.println("2. Download File (GET)");
            System.out.println("3. Exit");
            System.out.print("Choose an option (1-3): ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    uploadFile(scanner);
                    break;
                case "2":
                    downloadFile(scanner);
                    break;
                case "3":
                    System.out.println("Exiting...");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    // Upload File using HTTP POST
    private static void uploadFile(Scanner scanner) {
        try {
            // Ask user to input file path to upload
            System.out.print("Enter the file path to upload: ");
            String filePath = scanner.nextLine();
            
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("Error: File not found - " + filePath);
                return;
            }
            
            System.out.println("Uploading file: " + file.getName() + " (Size: " + file.length() + " bytes)");
            
            // Create URL object for upload endpoint
            URL url = new URL(SERVER_URL + "/upload");
            
            // Open HttpURLConnection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set method to POST and enable output
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(file.length()));
            
            // Open FileInputStream for local file and output stream from connection
            try (FileInputStream fileInput = new FileInputStream(file);
                 OutputStream connectionOutput = connection.getOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytesSent = 0;
                
                // Read from file and write to connection output stream in chunks
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    connectionOutput.write(buffer, 0, bytesRead);
                    totalBytesSent += bytesRead;
                    
                    // Show upload progress
                    double progress = (double) totalBytesSent / file.length() * 100;
                    System.out.printf("\rUpload Progress: %.2f%%", progress);
                }
                connectionOutput.flush();
            }
            
            // Read response from server
            int responseCode = connection.getResponseCode();
            System.out.println("\nServer Response Code: " + responseCode);
            
            // Display upload confirmation
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 ? 
                    connection.getInputStream() : connection.getErrorStream()))) {
                
                String line;
                System.out.println("Server Response:");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            connection.disconnect();
            
        } catch (IOException e) {
            System.err.println("Upload error: " + e.getMessage());
        }
    }
    
    // Download File using HTTP GET
    private static void downloadFile(Scanner scanner) {
        try {
            // Ask user to input filename to download
            System.out.print("Enter the filename to download: ");
            String filename = scanner.nextLine();
            
            // Create URL object with filename parameter
            String encodedFilename = URLEncoder.encode(filename, "UTF-8");
            URL url = new URL(SERVER_URL + "/download?filename=" + encodedFilename);
            
            // Open HttpURLConnection and set method to GET
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            int responseCode = connection.getResponseCode();
            System.out.println("Server Response Code: " + responseCode);
            
            if (responseCode == 200) {
                // HTTP 200 OK - file found
                // Get content length for progress tracking
                long fileSize = connection.getContentLengthLong();
                String contentDisposition = connection.getHeaderField("Content-Disposition");
                
                // Extract filename from Content-Disposition header if available
                String downloadFilename = "downloaded_" + filename;
                if (contentDisposition != null && contentDisposition.contains("filename=")) {
                    int start = contentDisposition.indexOf("filename=") + 9;
                    int end = contentDisposition.length();
                    if (contentDisposition.charAt(start) == '"') {
                        start++;
                        end = contentDisposition.indexOf('"', start);
                    }
                    downloadFilename = "downloaded_" + contentDisposition.substring(start, end);
                }
                
                System.out.println("Downloading file as: " + downloadFilename);
                if (fileSize > 0) {
                    System.out.println("File size: " + fileSize + " bytes");
                }
                
                // Open input stream from connection and FileOutputStream to save locally
                try (InputStream connectionInput = connection.getInputStream();
                     FileOutputStream fileOutput = new FileOutputStream(downloadFilename)) {
                    
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    // Read from input and write to output in chunks
                    while ((bytesRead = connectionInput.read(buffer)) != -1) {
                        fileOutput.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        // Show download progress if file size is known
                        if (fileSize > 0) {
                            double progress = (double) totalBytesRead / fileSize * 100;
                            System.out.printf("\rDownload Progress: %.2f%%", progress);
                        }
                    }
                    
                    System.out.println("\nFile downloaded successfully as: " + downloadFilename);
                    System.out.println("Total bytes downloaded: " + totalBytesRead);
                }
                
            } else if (responseCode == 404) {
                // HTTP 404 - File not found
                System.out.println("Error: File not found on server - " + filename);
                
                // Read error message from server
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Server Message: " + line);
                    }
                }
                
            } else {
                // Other HTTP error codes
                System.out.println("Error: HTTP " + responseCode + " - " + connection.getResponseMessage());
                
                // Read error message from server
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        connection.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Server Message: " + line);
                    }
                }
            }
            
            connection.disconnect();
            
        } catch (IOException e) {
            System.err.println("Download error: " + e.getMessage());
        }
    }
}
