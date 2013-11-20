package me.legrange.tcpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import me.legrange.net.IPAccessList;
import me.legrange.net.NetworkException;
import org.apache.log4j.Logger;

/**
 * This is a simple, multi-threaded TCP server.
 */
public class TCPServer implements Runnable {

    /**
     * instantiate a new server that will listen on the given port with the given
     * default access policy.
     */
    public TCPServer(int port, ServiceFactory fact, boolean defaultPolicy)  throws NetworkException {
        this(port, fact, new IPAccessList(defaultPolicy));
    }

    /**
     * instantiate a new server that will listen on the given port with the
     * given access policy.
     */
    public TCPServer(int port, ServiceFactory fact, IPAccessList acl) {
        this.port = port;
        this.acl = acl;
        this.fact = fact;
    }

    /**
     * instantiate a new server that will listen on the given port and allow
     * connections from anywhere by default.
     */
    public TCPServer(int port, ServiceFactory fact) throws NetworkException {
        this(port, fact, true);
    }

    /**
     * add an access rule
     */
    public void addAcl(String ip, int mask, boolean allow) throws NetworkException {
        acl.add(ip, mask, allow);
    }

    /**
     * start running
     */
    public void start() throws IOException {
        sock = new ServerSocket(port);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("TCP Server Socket Thread");
        thread.start();
    }

    /**
     * stop running
     */
    public void stop() {
        running = false;
        try {
            sock.close();
        } catch (IOException e) {
            log.error(String.format("Error closing socket: %s", e.getMessage()),e );
        }
    }

    public boolean isAlive() {
        return running && thread.isAlive();
    }

    /**
     * run method
     */
    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                Socket s = sock.accept(); // wait for incoming connection
                if (checkAccess(s)) {
                    Thread t = new Thread(new Runner(s));
                    t.setDaemon(true);
                    if (verbose) 
                        log.info(String.format("Accepting connection %s", describeConnection(s)));
                    t.start();
                } else {
                    log.info(String.format("Rejecting connection %s", describeConnection(s)));
                    s.close();
                }
            } catch (IOException e) {
                log.error(String.format("Error accepting incoming connection: " + e.getMessage()));
            }
        }
    }
    
    /**
     * check if the remote IP address has access to this service
     */
    private boolean checkAccess(Socket s) {
        try {
            return acl.checkAccess(s.getInetAddress());
        } catch (NetworkException e) {
            log.error(String.format("Invalid IP address %s in ACL check.", s.getInetAddress().getHostAddress()));
        }
        return false;
    }

    /**
     * make a nice host:port string
     */
    private String describeConnection(Socket s) {
        return String.format("from %s:%d to %s:%d", s.getInetAddress().getHostAddress(), s.getPort(), s.getLocalAddress().getHostAddress(), s.getLocalPort());
    }
    private int port;
    private ServerSocket sock;
    private ServiceFactory fact;
    private Thread thread;
    private boolean running = false;
    private boolean verbose = true;
    private Logger log = Logger.getLogger(TCPServer.class);
    private IPAccessList acl;

    /**
     * t that handles a single connection
     */
    private class Runner implements Runnable {

        private Runner(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Service service = fact.getService(socket);
                String reply = service.open();
                if (reply != null) {
                    out.println(reply);
                }
                String line = null;
                while (service.isRunning() && ((line = in.readLine()) != null)) {
                    reply = service.receive(line);
                    if (reply != null) {
                        out.println(reply);
                    }
                }
            } catch (IOException e) {
                log.error(String.format("Error talking to %s: %s", describeConnection(socket), e.getMessage()));
            } finally {
                try {
                    socket.close();
                    if (verbose)
                        log.info(String.format("Connection closed %s", describeConnection(socket)));
                } catch (IOException e) {
                    log.error(String.format("Error closing connection %s: %s", describeConnection(socket), e.getMessage()));
                }
            }
        }
        private Socket socket;
    }
}