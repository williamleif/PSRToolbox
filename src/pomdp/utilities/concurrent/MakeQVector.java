package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.utilities.AlphaVector;
import pomdp.valuefunction.MDPValueFunction;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class MakeQVector extends Task {

	private MDPValueFunction m_mMDP;
	private int m_iAction;
	private AlphaVector m_avResult;
	
	public MakeQVector( MDPValueFunction mdp, int iAction ){
		m_mMDP = mdp;
		m_iAction = iAction;
		m_avResult = null;
	}
	
	public void execute() {
		AlphaVector av = m_mMDP.newAlphaVector();
		double dQValue = 0.0;
		
		for( int iState : m_mMDP.getValidStates() ){
			dQValue = m_mMDP.getQValue( iState, m_iAction ) ;
			if( dQValue != 0.0 )
				av.setValue( iState, dQValue );
		}
		av.finalizeValues();
		m_avResult = av;
	}
	public AlphaVector getResult(){
		return m_avResult;
	}
	public void copyResults( Task tProcessed ){
		m_avResult = ((MakeQVector)tProcessed).getResult();		
	}
	
	public Element getDOM(Document doc) throws Exception {
		throw new NotImplementedException();
	}
}
