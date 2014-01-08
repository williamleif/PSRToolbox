package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EmptyTask extends Task {

	@Override
	public void copyResults(Task processed) {
	}

	@Override
	public void execute() {
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		return null;
	}

}
