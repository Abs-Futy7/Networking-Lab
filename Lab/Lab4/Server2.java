import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server2 {
    private static final int PORT = 8080;
    private static HttpServer server;
    
    public static void main(String[] args) {
        try {
            // Step 1: Initialize the Server
            // Set port and create HttpServer instance
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            System.out.println("HTTP File Server started on port " + PORT);
            
            // Define two contexts: /download and /upload
            server.createContext("/download", new DownloadHandler());
            server.createContext("/upload", new UploadHandler());
            
            // Set thread pool executor to handle multiple clients
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            // Start the server
            server.start();
            System.out.println("Server is running...");
            System.out.println("Download files: http://localhost:" + PORT + "/download?filename=<filename>");
            System.out.println("Upload files: POST to http://localhost:" + PORT + "/upload");
            
        } catch (IOException e) {
            System.err.println("Server initialization error: " + e.getMessage());
        }
    }
    
    // Handler for GET requests - File Download
    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Step 2: Handle GET Requests
            String method = exchange.getRequestMethod();
            
            // Check if method is GET
            if (!method.equals("GET")) {
                // Respond with HTTP 405 (Method Not Allowed)
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Extract filename from query parameters
            String query = exchange.getRequestURI().getQuery();
            String filename = null;
            
            if (query != null && query.startsWith("filename=")) {
                filename = query.substring("filename=".length());
                // URL decode the filename
                filename = URLDecoder.decode(filename, "UTF-8");
            }
            
            if (filename == null || filename.isEmpty()) {
                sendResponse(exchange, 400, "Bad Request: filename parameter required");
                return;
            }
            
            System.out.println("Download requested for file: " + filename);
            
            // Check if file exists on server disk
            File file = new File(filename);
            
            if (!file.exists() || !file.isFile()) {
                // File not found - respond with HTTP 404
                sendResponse(exchange, 404, "File Not Found");
                System.out.println("File not found: " + filename);
                return;
            }
            
            // File found - set response headers
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/octet-stream");
            headers.set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            
            // Send HTTP 200 OK with file size as content length
            long fileSize = file.length();
            exchange.sendResponseHeaders(200, fileSize);
            
            System.out.println("Sending file: " + filename + " (Size: " + fileSize + " bytes)");
            
            // Open FileInputStream and write file bytes to response output stream
            try (FileInputStream fileInput = new FileInputStream(file);
                 OutputStream responseOutput = exchange.getResponseBody()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                
                // Write file bytes in chunks
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    responseOutput.write(buffer, 0, bytesRead);
                }
                responseOutput.flush();
            }
            
            System.out.println("File sent successfully: " + filename);
        }
    }
    
    // Handler for POST requests - File Upload
    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Step 3: Handle POST Requests
            String method = exchange.getRequestMethod();
            
            // Check if method is POST
            if (!method.equals("POST")) {
                // Respond with HTTP 405 (Method Not Allowed)
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Create new file with timestamp naming convention
            long timestamp = System.currentTimeMillis();
            String uploadFileName = "upload_" + timestamp + ".dat";
            File uploadFile = new File(uploadFileName);
            
            System.out.println("Receiving file upload as: " + uploadFileName);
            
            // Open InputStream from HttpExchange and FileOutputStream for new file
            try (InputStream requestInput = exchange.getRequestBody();
                 FileOutputStream fileOutput = new FileOutputStream(uploadFile)) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalBytes = 0;
                
                // Read data in chunks from input and write to file output
                while ((bytesRead = requestInput.read(buffer)) != -1) {
                    fileOutput.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                fileOutput.flush();
                
                System.out.println("File uploaded successfully: " + uploadFileName + 
                                 " (Size: " + totalBytes + " bytes)");
                
                // Send HTTP 200 OK with confirmation message
                String response = "File uploaded successfully as: " + uploadFileName + 
                                " (Size: " + totalBytes + " bytes)";
                sendResponse(exchange, 200, response);
                
            } catch (IOException e) {
                System.err.println("Error during file upload: " + e.getMessage());
                sendResponse(exchange, 500, "Internal Server Error during upload");
            }
        }
    }
    
    // Utility method to send HTTP responses
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) 
            throws IOException {
        byte[] responseBytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(responseBytes);
            output.flush();
        }
    }
}
