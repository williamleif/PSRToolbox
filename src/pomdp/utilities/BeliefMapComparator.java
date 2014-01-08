package pomdp.utilities;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class BeliefMapComparator implements Comparator {
	protected static BeliefMapComparator m_bmcComparator = null;
	
	private BeliefMapComparator(){
	}
	
	public static BeliefMapComparator getInstance(){
		if( m_bmcComparator == null ){
			m_bmcComparator = new BeliefMapComparator();
		}
		return m_bmcComparator;
	}
		
	protected double diff( double d1, double d2 ){
		double dResult = d1 - d2;
		if( dResult < 0.0 )
			dResult *= -1;
		return dResult;
	}
	
	public int compare( Map mEntries1, Map mEntries2 ){
		if( mEntries1.size() != mEntries2.size() )
			return mEntries1.size() - mEntries2.size();
		
		//Same sized belief states
		Iterator itFirstNonZero = mEntries1.entrySet().iterator();
		Iterator itSecondNonZero = mEntries2.entrySet().iterator();
		int iState1 = -1, iState2 = -1;
		double dValue1 = 0.0, dValue2 = 0.0;
		Belief b = null;
				
		while( ( iState1 < Integer.MAX_VALUE ) || ( iState2 < Integer.MAX_VALUE ) ){
			if( iState1 == iState2 ){
				if( diff( dValue1, dValue2 ) > 0.00000001 ){
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
				return 1;
			}
			else if( iState2 < iState1 ){
				return -1;
			}			
		}
		
		return 0;
	}
	
	public int compare( Object o1, Object o2 ){
		if( o1 == o2 )
			return 0;
		if( ( o1 instanceof Map ) && ( o2 instanceof Map ) ){
			Map mEntries1 = (Map)o1;
			Map mEntries2 = (Map)o2;
			int iResult = compare( mEntries1, mEntries2 );
			//Logger.getInstance().logln( "Comparing to " + bs2 + " result = " + iResult );
			return iResult;
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
