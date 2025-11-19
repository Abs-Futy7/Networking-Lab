
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

public class Server_13_47 {
    public static Vector<ClientHandlerThread> clientThreads = new Vector<>();
    public static int selectedIndex = 0;

    public static void main(String[] args) throws Exception {

        ServerSocket ss = new ServerSocket(3000);
        System.out.println("New Server Created");
        Scanner sc = new Scanner(System.in);
        new Thread(() -> {
            try {
                while (true) {
                    Socket s = ss.accept();

                    DataInputStream in = new DataInputStream(s.getInputStream());
                    DataOutputStream out = new DataOutputStream(s.getOutputStream());

                    ClientHandlerThread thread = new ClientHandlerThread(s, in, out, clientThreads.size());
                    clientThreads.add(thread);

                    thread.start();
                }
            } catch (Exception e) {
               System.out.println("Error in accepting client" + e);
            }

        }).start();

        new Thread(() -> {
            try {
                while (true) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();

                    System.out.println("0. Exit");
                    String read = sc.nextLine();
                    if (read.equals("0")) {
                        System.out.println("Quitting...");
                        System.exit(0);
                        break;
                    }

                }
            } catch (Exception e) {
                System.out.println("Error in i/o thread");
            }

        }).start();

    }
}

class ClientHandlerThread extends Thread {
    public Socket s;
    DataInputStream in;
    DataOutputStream out;
    String name;
    Vector<ClientHandlerThread> clientList;

    public ClientHandlerThread(Socket s, DataInputStream in, DataOutputStream out, int index) {
        this.s = s;
        this.in = in;
        this.out = out;
        this.clientList = Server_13_47.clientThreads;
    }

    @Override
    public void run() {
        try {
           
            name = in.readUTF();
            System.out.println(name + " joined the server!");
            
            while (true) {
                String message = in.readUTF();
                
                if (message.equals("quit")) {
                    System.out.println(name + " left the server!");
                    break;
                }
                
                broadcastMessage(name + ": " + message, this);
            }
            
        } catch (Exception e) {
            System.out.println(name + " disconnected unexpectedly");
        } finally {
            clientList.remove(this);
            try {
                s.close();
            } catch (Exception e) {
                System.out.println("Error closing socket");
            }
        }
    }
  
    private void broadcastMessage(String message, ClientHandlerThread sender) {
        for (ClientHandlerThread client : clientList) {
            if (client != sender) {
                try {
                    client.out.writeUTF(message);
                } catch (Exception e) {
                    System.out.println("Failed to send message to " + client.name);
                }
            }
        }
    }

}