
package examples;

import java.io.IOException;
import java.net.Socket;
import me.legrange.net.NetworkException;
import me.legrange.tcpserver.Service;
import me.legrange.tcpserver.ServiceFactory;
import me.legrange.tcpserver.TcpServer;

/**
 *
 * @author gideon
 */
public class EchoExample {
    
    public static void main(String...args) throws NetworkException, InterruptedException, IOException {
        TcpServer s = new TcpServer(2000, new ServiceFactory() {
            @Override
            public Service getService(final Socket sock) {
                
                return new Service() {
                    private boolean running = true;
                    
                    @Override
                    public String open() {
                        return  "Hello user from " + sock.getRemoteSocketAddress() + "!\n" + 
                                "Type something. Type quit to stop";
                    }

                    @Override
                    public boolean isRunning() {
                        return running;
                    }

                    @Override
                    public String receive(String line) {
                        line = line.trim().toLowerCase();
                        switch (line) {
                            case "quit" : 
                                running = false;
                                return "OK";
                            case "stop" : 
                                System.exit(0);
                                return "Gurgle";
                            default : 
                                return "You said: '" + line + "'";
                        }
                        
                    }
                };
            }
        }, true);
        s.start();
        while (true) {
            Thread.sleep(1000);
        }
    }
    
}
