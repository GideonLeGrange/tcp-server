package me.legrange.tcpserver;

/** This interface must be implemented to provide a factory for creating new Logic instances. */
public interface ServiceFactory {
	
	/** return a new logic instance */
	public Service getInstance();
	
}