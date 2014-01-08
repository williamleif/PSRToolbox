/*
 * Created on May 3, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pomdp.utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LineReader{
	//FileInputStream m_fosInput;
	BufferedReader m_fosInput;
	public LineReader( String sFileName ) throws IOException{
		//m_fosInput = new FileInputStream( sFileName );
		m_fosInput = new BufferedReader( new FileReader( sFileName ) );
	}
	/*
	public String readLine() throws IOException, EndOfFileException{
		String sLine = "";
		char c = 0;
		boolean bDone = false;
		
		if( endOfFile() )
			throw new EndOfFileException();
		
		while( !bDone ){
			c = (char)m_fosInput.read();
			if( c != '\n' ){
				sLine += c;
				bDone = endOfFile();
			}
			else
				bDone = true;
		}
		return sLine;
	}
	*/
	public String readLine() throws IOException, EndOfFileException{
		String sLine = m_fosInput.readLine();
		//Logger.getInstance().logln( sLine );
		return sLine;
	}
	
	public boolean endOfFile() throws IOException{
		//return m_fosInput.available() == 0;
		return !m_fosInput.ready();
	}
}
