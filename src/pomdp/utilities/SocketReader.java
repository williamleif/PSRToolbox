package pomdp.utilities;

import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;

public class SocketReader extends StringReader {
	
	public SocketReader( Socket s ) throws IOException{
		super( readString( s ) );
	}

	private static String readString( Socket s ) throws IOException {
		String sXML = "";
		do{
			sXML += (char)s.getInputStream().read();
		}while( s.getInputStream().available() > 0 );
		return sXML;
	}
}
