package me.legrange.tcpserver;

/** This interface must be implemented to provide the logic for
  * a simple TCP server. */
public interface Service {
	
	/** Called by the server to start a new conversation, returning the 
         * first data sent to the connection. 
         * @return The text to send to the connection */
	public String open();
	
	/** Called by the server to check to see if the service is still interested in talking.
         * @return  True if the session needs to continue */
	public boolean isRunning();
	
	/** Called by the server to process a line of text received from the network.
         * @param line The text to process
         * @return The response to send to the network */
	public String receive(String line);
        
	
	
}