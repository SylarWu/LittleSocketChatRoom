import Local.Server;
import User.Client;

import java.io.IOException;

public class ServerTest {

    public static void main(String args[]) throws IOException {
        Server server = Server.getInstarnce();
        server.OnService();
    }
}
