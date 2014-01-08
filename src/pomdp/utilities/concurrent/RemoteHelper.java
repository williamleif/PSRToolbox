package pomdp.utilities.concurrent;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import pomdp.environments.POMDP;
import pomdp.utilities.Logger;
import pomdp.utilities.SocketReader;


public class RemoteHelper {
	
	public static String readFromSocket( Socket socket ) throws IOException{
		String sXML = "";
		do{
			sXML += (char)socket.getInputStream().read();
		}while( socket.getInputStream().available() > 0 );
		return sXML;
	}
	
	public static void main(String argv[]) {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		Socket socket = null;
		POMDP pomdp = null;
		Task t = null;
		try {
			        // open a socket connection
			socket = new Socket( "172.31.42.197", 3000 );
			// open I/O streams for objects
			oos = new ObjectOutputStream( socket.getOutputStream() );
			ois = new ObjectInputStream( socket.getInputStream() );
			// read an object from the server
			pomdp = (POMDP) ois.readObject();
			pomdp.initBeliefStateFactory();
			Logger.getInstance().logln( "Accepted a POMDP model: " + pomdp.getName() );
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			do{
				//String sXML = readFromSocket( socket );
				//StringReader sr = new StringReader( sXML );
				SocketReader sr = new SocketReader( socket );
				InputSource is = new InputSource( sr ); 
				Document docTask = builder.parse( is );
				t = Task.restoreTask( docTask, pomdp );

				Logger.getInstance().logln( "Accepted task " + t.getId() + " of type " + t.getClass() );
				t.execute();
				Logger.getInstance().logln( "Done executing" );

				docTask = builder.newDocument();
				Element eTask = t.getDOM( docTask );
				DOMSource source = new DOMSource( eTask );
				StreamResult result = new StreamResult( socket.getOutputStream() );
				transformer.transform( source, result );

				/*
				//oos = new ObjectOutputStream( socket.getOutputStream() );
				//ois = new ObjectInputStream( socket.getInputStream() );
				t = (Task)ois.readObject();
				Logger.getInstance().logln( "Accepted task " + t.getId() + " of type " + t.getClass() );
				t.setPOMDP( pomdp );
				t.execute();
				Logger.getInstance().logln( "Done executing" );
				oos.writeObject( t );
				oos.flush();
				*/
			}while( !( t instanceof TerminateTask ) );
			oos.close();
			ois.close();
		} 
		catch(Exception e) {
			Logger.getInstance().logln( e );
			e.printStackTrace();
		}
	}
}
