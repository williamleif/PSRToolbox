package pomdp.utilities.concurrent;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;

public class ComputeFarthestSuccessors  extends Task implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Vector<BeliefState> m_vAllBeliefs;
	private Vector<BeliefState> m_vTaskBeliefs;
	private Vector<BeliefState> m_vResultBeliefs;
	
	public ComputeFarthestSuccessors( Vector<BeliefState> vBeliefs ){
		m_vAllBeliefs = vBeliefs;
		m_vTaskBeliefs = new Vector<BeliefState>();
		m_vResultBeliefs = new Vector<BeliefState>();
	}
	
	public void addBelief( BeliefState bs ){
		m_vTaskBeliefs.add( bs );
	}
	
	public Vector<BeliefState> getSuccessors(){
		return m_vResultBeliefs;
	}
	
	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execute() {
		BeliefState bsSuccessor = null;
		for( BeliefState bs : m_vTaskBeliefs ){
			bsSuccessor = m_pPOMDP.getBeliefStateFactory().computeFarthestSuccessor( m_vAllBeliefs, bs );
			if( bsSuccessor != null )
				m_vResultBeliefs.add( bsSuccessor );
		}		
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		for( BeliefState bs : m_vTaskBeliefs ){
			m_vResultBeliefs.add( m_pPOMDP.getBeliefStateFactory().computeFarthestSuccessor( m_vAllBeliefs, bs ) );
		}
	}

}
