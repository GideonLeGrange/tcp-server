package me.legrange.tcpserver;

import java.net.InetAddress;
import me.legrange.net.IPAccessList;

/**
 *
 * @author gideon
 */
public class TcpServerBuilder {
    
    private final int port; 
    private final ServiceFactory fact;
    private IPAccessList acl = new IPAccessList(false);
    private InetAddress address;

    public TcpServerBuilder(int port, ServiceFactory fact) {
        this.port = port;
        this.fact = fact;
    }
    
    public TcpServerBuilder withAcl(IPAccessList acl) {
        this.acl = acl;
        return this;
    }
    
    public TcpServerBuilder withServerAddress(InetAddress address) {
        this.address = address;
        return this;
    }
    
    
}
