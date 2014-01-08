package pomdp.utilities.distance;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import pomdp.utilities.BeliefState;

public abstract class LDistance implements DistanceMetric {
	protected TreeMap m_tmCache;
	
	public LDistance(){
		m_tmCache = null;//new TreeMap();
	}

	protected double getInitialDistance(){
		return 0.0;
	}
	
//	protected double getCachedDistance( BeliefState bs1, BeliefState bs2 ){
//		try{
//			TreeMap tmSingle = (TreeMap) m_tmCache.get( new Integer( bs1.getId() ) );
//			if( tmSingle != null ){
//				Double dValue = (Double) tmSingle.get( new Integer( bs2.getId() ) );
//				if( dValue != null )
//					return dValue.doubleValue();
//			}
//			return -1.0;
//		}
//		catch( NullPointerException e ){
//			Logger.getInstance().logln( e );
//			return getCachedDistance( bs1, bs2 );
//		}
//	}
	
//	protected void cacheDistance( BeliefState bs1, BeliefState bs2, double dDistance ){
//		TreeMap tmSingle1 = (TreeMap) m_tmCache.get( new Integer( bs1.getId() ) );
//		TreeMap tmSingle2 = (TreeMap) m_tmCache.get( new Integer( bs2.getId() ) );
//		if( tmSingle1 == null ){
//			tmSingle1 = new TreeMap();
//			m_tmCache.put( new Integer( bs1.getId() ), tmSingle1 );
//		}
//		if( tmSingle2 == null ){
//			tmSingle2 = new TreeMap();
//			m_tmCache.put( new Integer( bs2.getId() ), tmSingle2 );
//		}
//		tmSingle1.put( new Integer( bs2.getId() ), new Double( dDistance ) );
//		tmSingle2.put( new Integer( bs1.getId() ), new Double( dDistance ) );
//	}
	
	public double distance( BeliefState bs1, BeliefState bs2 ){
		double dDistance = -1.0;//getCachedDistance( bs1, bs2 );
		if( dDistance != -1.0 )
			return dDistance;
		
		if( bs1 == bs2 )
			dDistance = 0.0;
		else{
			Iterator itFirstNonZero = bs1.getNonZeroEntries().iterator();
			Iterator itSecondNonZero = bs2.getNonZeroEntries().iterator();
			dDistance = 0.0;
			Map.Entry e = null;
			int iState1 = -1, iState2 = -1, iRetVal = 0;
			double dValue1 = 0.0, dValue2 = 0.0;
			Belief b = null;
			
			dDistance = getInitialDistance();
			
//			while( ( iState1 < Integer.MAX_VALUE ) || ( iState2 < Integer.MAX_VALUE ) ){
//				if( iState1 == iState2 ){					
//					dDistance = applyDistanceMetric( dDistance, dValue1, dValue2 );
//					
//
//					iState1 = getBeliefState(itFirstNonZero);
//					dValue1 = getBeliefValue(itFirstNonZero);
//
//					iState2 = getBeliefState(itSecondNonZero);
//					dValue2 = getBeliefValue(itSecondNonZero);
//
//				}
//				else if( iState1 < iState2 ){
//					dDistance = applyDistanceMetric( dDistance, dValue1, 0 );
//			
//					iState1 = getBeliefState(itFirstNonZero);
//					dValue1 = getBeliefValue(itFirstNonZero);
//					
//				}
//				else if( iState2 < iState1 ){
//					dDistance = applyDistanceMetric( dDistance, dValue2, 0 );
//
//					iState2 = getBeliefState(itSecondNonZero);
//					dValue2 = getBeliefValue(itSecondNonZero);
//				}			
//			}
			
			
			while( ( iState1 < Integer.MAX_VALUE ) || ( iState2 < Integer.MAX_VALUE ) ){
				if( iState1 == iState2 ){					
					dDistance = applyDistanceMetric( dDistance, dValue1, dValue2 );
					
					b = new Belief( itFirstNonZero );
					iState1 = b.iState;
					dValue1 = b.dValue;
					b = new Belief( itSecondNonZero );
					iState2 = b.iState;
					dValue2 = b.dValue;
				}
				else if( iState1 < iState2 ){
					dDistance = applyDistanceMetric( dDistance, dValue1, 0 );

					b = new Belief( itFirstNonZero );
					iState1 = b.iState;
					dValue1 = b.dValue;				
				}
				else if( iState2 < iState1 ){
					dDistance = applyDistanceMetric( dDistance, dValue2, 0 );

					b = new Belief( itSecondNonZero );
					iState2 = b.iState;
					dValue2 = b.dValue;	
				}			
			}
			
		}
		
		
		dDistance = applyFinal( dDistance );
		
		//cacheDistance( bs1, bs2, dDistance );
		
		return dDistance;
	}

	protected abstract double applyDistanceMetric( double dAccumulated, double dValue1, double dValue2 );

	protected abstract double applyFinal( double dAccumulated );
	
	
	
	private int getBeliefState(Iterator it)
	{
		if( it.hasNext() ){
			Entry e = (Entry)it.next();
			return ((Integer) e.getKey()).intValue();
		}
		else return Integer.MAX_VALUE;
	}
	
	private double getBeliefValue(Iterator it)
	{
		if( it.hasNext() ){
			Entry e = (Entry)it.next();
			return ((Double) e.getValue()).doubleValue();
		}
		else return -1.0;
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
