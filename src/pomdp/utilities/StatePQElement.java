/*
 * Created on 01/05/2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package pomdp.utilities;

import pomdp.utilities.datastructures.PriorityQueueElement;

public class StatePQElement extends PriorityQueueElement {
	protected int m_iState;
	public StatePQElement( int iState ){
		m_iState = iState;
	}
	public int getState(){
		return m_iState;  
	}
}
