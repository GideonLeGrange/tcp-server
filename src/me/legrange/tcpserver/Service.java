package me.legrange.tcpserver;

/** This interface must be implemented to provide the logic for
  * a simple TCP server. */
public interface Service {
	
	/** start a new conversation */
	public String start();
	
	/** check to see if the service is still interested in talking. */
	public boolean isRunning();
	
	/** process a line received */
	public String receive(String line);
        
	
	
}