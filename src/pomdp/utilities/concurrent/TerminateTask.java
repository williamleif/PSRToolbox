package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class TerminateTask extends Task {

	
	public void execute() {
		// TODO Auto-generated method stub

	}

	
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub
		
	}

	
	public Element getDOM( Document doc ) throws Exception {
		return doc.createElement( "TerminateTask" );
	}

}
