package me.legrange.tcpserver;

import java.net.Socket;

/**
 * Factory class that supplies a service object on request. 
 * Users of the library needs to implement this. 
 * @author gideon
 */
public interface ServiceFactory {
    
    public Service getService(Socket sock);
    
}
