package me.legrange.tcpserver;

import java.net.Socket;

/**
 * Factory class called by the server to create a new service to 
 * manage a single session. Users of the library needs to implement this. 
 * @author gideon
 */
public interface ServiceFactory {
    
    /** Return a new service to manage the session for the supplied socket.
     * 
     * @param sock The socket for the new connection
     * @return The service to service the socket.
     */
    public Service getService(Socket sock);
    
}
