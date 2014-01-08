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
import java.io.IOException;
 
public class EndOfFileException extends IOException {
	public EndOfFileException(){
		super();
	}

	public EndOfFileException( String sData ){
		super( sData );
	}
}
