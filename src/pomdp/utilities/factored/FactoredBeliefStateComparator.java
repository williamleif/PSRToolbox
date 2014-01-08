package pomdp.utilities.factored;

import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;

/**
 * Use this class to compare two factored belief state implemeted by ADDs
 * @author shanigu
 *
 */
public class FactoredBeliefStateComparator extends BeliefStateComparator {

	public FactoredBeliefStateComparator( double dEpsilon ) {
		super( dEpsilon );
	}

	public static BeliefStateComparator getInstance( double dEpsilon ){
		if( m_bscComparator == null ){
			m_bscComparator = new FactoredBeliefStateComparator( dEpsilon );
		}
		return m_bscComparator;
	}

	
	public int compare( BeliefState bs1, BeliefState bs2 ){
		if( bs1 == bs2 )
			return 0;
		if( ( bs1 instanceof FactoredBeliefState ) && ( bs2 instanceof FactoredBeliefState ) ){
			FactoredBeliefState fbs1 = (FactoredBeliefState)bs1;
			FactoredBeliefState fbs2 = (FactoredBeliefState)bs2;
			int iResult = fbs1.m_addProbabilities.compareTo( fbs2.m_addProbabilities );
			iResult = fbs1.m_addProbabilities.compareTo( fbs2.m_addProbabilities );
			return iResult;
		}
		else{
			return super.compare( bs1, bs2 ); 
		}
	}

}
