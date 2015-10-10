import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServerThread extends Thread {
    public ChatServerThread() throws IOException {
        _socket = new DatagramSocket(2000);
        _clients = new ArrayList<Client>();
    }

    public void run() {
        try {

            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            Client client = new Client();
            _usernames = new ArrayList<String>();

            while (true) {
                _socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());

                // check for client
                client = findClient(packet);
                if (client._port == -1) {
                    client = registerClient(packet);
                }

                // can't implement this feature because clients are sending and
                // receiving on two different ports
//				client._ttl = 20;
//				// decrement ttl for all packets
//				for (Client cl : _clients) {
//					cl._ttl -= 1;
//					if (cl._ttl <= 0) {
//						_clients.remove(cl);
//					}
//				}

                // packet routing logic
                // if packet starts with %%%, then it is a program command
                if (str.startsWith("%%%")) {
                    if (str.startsWith("%%%verify_connection")) {
                        dispatchMessage(client, "%%%verify_connection");
                    }
                    else if (str.startsWith("%%%name")) {
                        if(checkName(str.substring(7))) {
                            dispatchMessage(client, "%%%avail");
                        }
                        else {
                            dispatchMessage(client, "%%%taken");
                        }
                    }
                }
                else {  // else is a normal message, broadcast to all clients
                    for (Client cl : _clients) {
                        dispatchMessage(cl, str);
                    }
                }
            }
        }
        catch (IOException e) {
            System.out.println("IOException on server; program terminating.");
            _socket.close();
        }
    }

    private static boolean checkName(String user) {
        for (String name : _usernames) {
            if (user.equals(name)) { return false; }
        }
        _usernames.add(user);
        return true;
    }

    private static Client registerClient(DatagramPacket packet) {
        Client client = new Client();
        client._address = packet.getAddress();
        client._port = packet.getPort();
        _clients.add(client);
        return client;
    }

    private static Client findClient(InetAddress address, int port) {
        for (int i = 0 ; i < _clients.size() ; ++i) {
            if (_clients.get(i)._address == address && _clients.get(i)._port == port) {
                return _clients.get(i);
            }
        }
        Client client = new Client();
        client._port = -1;
        return client;
    }

    private static Client findClient(DatagramPacket packet) {
        return findClient(packet.getAddress(), packet.getPort());
    }

    private static void dispatchMessage(Client client, String message) throws IOException {
        _socket.send(new DatagramPacket(message.getBytes(), message.getBytes().length, client._address, client._port));
    }

    private static List<Client> _clients;
    private static List<String> _usernames;
    private static DatagramSocket _socket;

    private static class Client {
        public InetAddress _address;
        public int _port = -1;
//		public int _ttl = 20;
    }
}
