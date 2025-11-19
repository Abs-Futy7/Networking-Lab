import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            MyTCPServerSocket server = new MyTCPServerSocket(5000);
            server.listen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
