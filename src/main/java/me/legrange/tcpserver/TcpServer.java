package me.legrange.tcpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import me.legrange.net.IPAccessList;
import me.legrange.net.NetworkException;

/**
 * This is a simple, multi-threaded TCP server.
 */
public class TcpServer {

    private int port;
    private ServerSocket sock;
    private ServiceFactory fact;
    private boolean running = false;
    private Logger log = Logger.getLogger(TcpServer.class.getName());
    private IPAccessList acl;
    private final ThreadPoolExecutor pool 
            = new  ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    60, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

    /**
     * instantiate a new server that will listen on the given port with the
     * given default access policy.
     */
    public TcpServer(int port, ServiceFactory fact, boolean defaultPolicy) throws NetworkException {
        this(port, fact, new IPAccessList(defaultPolicy));
    }

    /**
     * instantiate a new server that will listen on the given port with the
     * given access policy.
     */
    public TcpServer(int port, ServiceFactory fact, IPAccessList acl) {
        this.port = port;
        this.acl = acl;
        this.fact = fact;
    }

    /**
     * instantiate a new server that will listen on the given port and allow
     * connections from anywhere by default.
     */
    public TcpServer(int port, ServiceFactory fact) throws NetworkException {
        this(port, fact, true);
    }

    /**
     * add an access rule
     */
    public void addAcl(String ip, int mask, boolean allow) throws NetworkException {
        acl.add(ip, mask, allow);
    }
    
    /** Set the logger to use for logging info, debug and error messages 
     * 
     * @param log  The logger
     */
    public void setLog(Logger log) {
        this.log = log;
    }
    
    /** Set the maximum number of concurrent connections. 
     * 
     * @param num The number of connections
     */
    public void setMaxConnections(int num) {
        if (num >= 1)
            pool.setMaximumPoolSize(num +1);
    }

    /**
     * start running
     */
    public void start() throws IOException {
        sock = new ServerSocket(port);
        pool.submit(() -> run());
    }

    /**
     * stop running
     */
    public void stop() {
        running = false;
        try {
            sock.close();
        } catch (IOException e) {
            log.severe(String.format("Error closing socket: %s", e.getMessage()));
        }
    }

    public boolean isAlive() {
        return running;
    }

    /**
     * run method
     */
    private void run() {
        running = true;
        while (running) {
            try {
                Socket s = sock.accept(); // wait for incoming connection
                if (checkAccess(s)) {
                    pool.submit(() -> run(s));
                    log.finest(String.format("Accepting connection %s", describeConnection(s)));
                } else {
                    log.fine(String.format("Rejecting connection %s", describeConnection(s)));
                    s.close();
                }
            } catch (IOException e) {
                log.severe(String.format("Error accepting incoming connection: " + e.getMessage()));
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
            log.severe(String.format("Invalid IP address %s in ACL check.", s.getInetAddress().getHostAddress()));
        }
        return false;
    }

    /**
     * make a nice host:port string
     */
    private String describeConnection(Socket s) {
        return String.format("from %s:%d to %s:%d", s.getInetAddress().getHostAddress(), s.getPort(), s.getLocalAddress().getHostAddress(), s.getLocalPort());
    }

    private void run(Socket socket) {
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
            log.severe(String.format("Error talking to %s: %s", describeConnection(socket), e.getMessage()));
        } finally {
            try {
                socket.close();
                log.finest(String.format("Connection closed %s", describeConnection(socket)));
            } catch (IOException e) {
                log.severe(String.format("Error closing connection %s: %s", describeConnection(socket), e.getMessage()));
            }
        }
    }

}
