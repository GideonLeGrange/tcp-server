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

// 2012-10-08 - Refactored from old code. 
/** This is a simple, multi-threaded TCP server. */
public class TCPServer implements Runnable {

    /** instantiate a new server that will listen on the given port with the give default access policy. */
    public TCPServer(int port, boolean defaultPolicy, ServiceFactory factory) {
        this(port, new IPAccessList(defaultPolicy), factory);
    }

    /** instantiate a new server that will listen on the given port with the given access policy. */
    public TCPServer(int port, IPAccessList acl, ServiceFactory factory) {
        this.port = port;
        this.factory = factory;
        this.acl = acl;
    }

    /** instantiate a new server that will listen on the given port and allow connections from anywhere by default. */
    public TCPServer(int port, ServiceFactory factory) {
        this(port, true, factory);
    }

    /** add an access rule */
    public void addAcl(String ip, String mask, boolean allow) throws NetworkException {
        acl.add(ip, mask, allow);
    }

    /** add an access rule */
    public void addAcl(String ip, int mask, boolean allow) throws NetworkException {
        acl.add(ip, mask, allow);
    }

    /** start running */
    public void start() throws IOException {
        sock = new ServerSocket(port);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("TCP Server Socket Thread");
        thread.start();
    }

    /** stop running */
    public void stop() throws IOException {
        running = false;
        sock.close();
    }

    public boolean isAlive() {
        return running && thread.isAlive();
    }

    /** run method */
    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                Socket s = sock.accept(); // wait for incoming connection
                if (checkAccess(s)) {
                    Thread t = new Thread(new Runner(s));
                    t.setDaemon(true);
                    if (verbose) {
                        log.info(String.format("Accepting connection %s", describeConnection(s)));
                    }
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

    /** check if the remote IP address has access to this service */
    private boolean checkAccess(Socket s) {
        try {
            return acl.checkAccess(s.getInetAddress().getHostAddress());
        } catch (NetworkException e) {
            log.error(String.format("Invalid IP address %s in ACL check.", s.getInetAddress().getHostAddress()));
        }
        return false;
    }

    /** make a nice host:port string */
    private String describeConnection(Socket s) {
        return String.format("from %s:%d to %s:%d", s.getInetAddress().getHostAddress(), s.getPort(), s.getLocalAddress().getHostAddress(), s.getLocalPort());
    }
    private int port;
    private ServerSocket sock;
    private Thread thread;
    private boolean running = false;
    private boolean verbose = true;
    private ServiceFactory factory;
    private Logger log = Logger.getLogger(TCPServer.class);
    private IPAccessList acl;

    /** t that handles a single connection */
    private class Runner implements Runnable {

        private Runner(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Service state = factory.getInstance();
                String reply = state.start();
                if (reply != null) {
                    out.println(reply);
                }
                String line = null;
                while (state.isRunning() && ((line = in.readLine()) != null)) {
                    reply = state.receive(line);
                    if (reply != null) {
                        out.println(reply);
                    }
                }
            } catch (IOException e) {
                log.error(String.format("Error talking to %s: %s", describeConnection(socket), e.getMessage()));
            } finally {
                try {
                    socket.close();
                    log.info(String.format("Connection closed from %s to %s", describeConnection(socket)));
                } catch (IOException e) {
                    log.error(String.format("Error closing connection %s: %s", describeConnection(socket), e.getMessage()));
                }
            }
        }
        private Socket socket;
    }
}