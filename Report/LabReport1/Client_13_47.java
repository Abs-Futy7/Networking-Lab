import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client_13_47 {
    public static void main(String[] args) throws Exception {

        System.out.print("\033[H\033[2J");
        System.out.flush();
        Scanner sc = new Scanner(System.in);
        
        String name = "";
        
        System.out.print("Enter Name:");
        name = sc.nextLine();
        
        Socket s = new Socket("localhost", 3000);
        DataOutputStream out = new DataOutputStream(s.getOutputStream());
        DataInputStream in = new DataInputStream(s.getInputStream());

        System.out.println("Joined Server: "+ name);
        System.out.print("\033[H\033[2J");
        System.out.flush();
        System.out.println("Welcome " + name + "!");
        System.out.println("Type your messages below. Type 'quit' to exit.");
        out.writeUTF(name);
        AtomicBoolean running = new AtomicBoolean(true);
        new Thread(() -> {
            try {
                while (running.get()) {
                    String message = sc.nextLine();
                    if (message.equals("quit")) {
                        running.set(false);
                        out.writeUTF(message);
                        break;
                    }
                    if (!message.trim().isEmpty()) {
                        System.out.println("You: " + message);
                        out.writeUTF(message);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error sending message");
            }
        }).start();

        new Thread(() -> {
            try {
                while (running.get()) {
                    String message = in.readUTF();
                    System.out.println(message);
                }
            } catch (Exception e) {
                if (running.get()) {
                    System.out.println("Connection to server lost");
                }
            }
        }).start();
    }
}