package pomdp.utilities;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class BeliefStateComparator implements Comparator<BeliefState> /*, Serializable*/ {
	protected double m_dEpsilon;
	protected static BeliefStateComparator m_bscComparator = null;
	
	public BeliefStateComparator( double dEpsilon ){
		m_dEpsilon = dEpsilon;
	}
	
	public static BeliefStateComparator getInstance( double dEpsilon ){
		if( m_bscComparator == null ){
			m_bscComparator = new BeliefStateComparator( dEpsilon );
		}
		return m_bscComparator;
	}
		
	public static BeliefStateComparator getInstance(){
		if( m_bscComparator == null ){
			return null;
		}
		return m_bscComparator;
	}
		
	//Cannot be used for fully ordering belief points if epsilon > 0. 
	//The reason is that it is possible that belief a > b, but there can exist a belief c that is between a and b but is equal to them both.
	//For example a_i = epsilon, b_i = 2*epsilon, c_i = 1.5*epsilon.
	public int compare( BeliefState bs1, BeliefState bs2 ){
					
		//Non deterministic belief states
		Iterator<Entry<Integer, Double>> itFirstNonZero = bs1.getNonZeroEntries().iterator();
		Iterator<Entry<Integer, Double>> itSecondNonZero = bs2.getNonZeroEntries().iterator();
		int iState1 = -1, iState2 = -1;
		double dValue1 = 0.0, dValue2 = 0.0;
		Belief b = null;
				
		while( ( iState1 < Integer.MAX_VALUE ) || ( iState2 < Integer.MAX_VALUE ) ){
			if( iState1 == iState2 ){
				if( Math.abs( dValue1 - dValue2 ) > m_dEpsilon ){
					if( dValue1 > dValue2 )
						return 1;
					else
						return -1;
				}
				b = new Belief( itFirstNonZero );
				iState1 = b.iState;
				dValue1 = b.dValue;
				b = new Belief( itSecondNonZero );
				iState2 = b.iState;
				dValue2 = b.dValue;
			}
			else if( iState1 < iState2 ){
				if( dValue1 > m_dEpsilon )
					return 1;
				b = new Belief( itFirstNonZero );
				iState1 = b.iState;
				dValue1 = b.dValue;
			}
			else if( iState2 < iState1 ){
				if( dValue2 > m_dEpsilon )
					return -1;
				b = new Belief( itSecondNonZero );
				iState2 = b.iState;
				dValue2 = b.dValue;
			}			
		}
		
		return 0;
	}

	public class Belief{
		public int iState;
		public double dValue;
		
		public Belief( Iterator it ){
			if( it.hasNext() ){
				Entry e = (Entry)it.next();
				iState = ((Integer) e.getKey()).intValue();
				dValue = ((Double) e.getValue()).doubleValue();
			}
			else{
				iState = Integer.MAX_VALUE;
				dValue = -1.0;
			}
		}
	}
}
