package pomdp.algorithms.pointbased.priorities;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.Logger;
import pomdp.utilities.datastructures.Function;
import pomdp.utilities.datastructures.PriorityQueue;

public class StateBasedPriorityUpdate implements PriorityUpdatePolicy {
	protected Map m_mPredcessors;
	protected Function m_fTransition;
	protected Vector m_vBeliefStates;
	protected int m_cActions;
	protected int m_cStates;
	
	
	public StateBasedPriorityUpdate( Vector vBeliefStates, Function fTransition,
			int cActions, int cStates ){
		m_vBeliefStates = vBeliefStates;
		m_fTransition = fTransition;
		m_cActions = cActions;
		m_cStates = cStates;
		initPredcessors();
	}
	
	protected double computeWeight( BeliefState bs1, BeliefState bs2 ){
		Iterator itStates1 = null, itStates2 = null;
		int iState1 = 0, iState2 = 0, iAction = 0;
		double dWeight = 0.0, dMaxWeight = Double.MAX_VALUE * -1.0;
		double dBelief1 = 0.0, dBelief2 = 0.0, dTr = 0.0, dSum = 0.0;
		Map.Entry e = null;
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dWeight = 0.0;
			itStates1 = bs1.getNonZeroEntries().iterator();
			while( itStates1.hasNext() ){
				e = (Entry) itStates1.next();
				iState1 = ((Number) e.getKey()).intValue();
				dBelief1 = ((Number) e.getValue()).doubleValue();
				dSum = 0.0;
				itStates2 = m_fTransition.getNonZeroEntries( iState1, iAction );
				while( itStates2.hasNext() ){
					e = (Entry) itStates2.next();
					iState2 = ((Number) e.getKey()).intValue();
					dTr = ((Number) e.getValue()).doubleValue();
					dBelief2 = bs2.valueAt( iState2 );
					dSum += dTr * dBelief2;
				}
			}
			dWeight += dSum * dBelief1;
			if( dWeight > dMaxWeight ){
				dMaxWeight = dWeight;
			}
		}
		
		return dMaxWeight;
	}
	
	protected void initPredcessors(){
		int cBeliefStates = m_vBeliefStates.size(), iBeliefState = 0;
		Iterator itBeliefStates1 = null, itBeliefStates2 = null;
		BeliefState bs1 = null, bs2 = null;
		double dWeight = 0.0;
		Map mPredcessors = null;
		
		m_mPredcessors = new TreeMap( BeliefStateComparator.getInstance() );
		
		Logger.getInstance().logln( "Initializing predecessors" );
		
		itBeliefStates1 = m_vBeliefStates.iterator();
		while( itBeliefStates1.hasNext() ){
			bs1 = (BeliefState) itBeliefStates1.next();
			mPredcessors = new TreeMap( BeliefStateComparator.getInstance() );
			itBeliefStates2 = m_vBeliefStates.iterator();
			while( itBeliefStates2.hasNext() ){
				bs2 = (BeliefState) itBeliefStates2.next();
				dWeight = computeWeight( bs2, bs1 );
				if( dWeight > 0.00001 ){
					mPredcessors.put( bs2, new Double( dWeight ) );
				}
			}
			m_mPredcessors.put( bs1, mPredcessors );
			iBeliefState++;
			
			if( iBeliefState % 100 == 0 ){
				Logger.getInstance().logln( "Done " + iBeliefState + " belief states " );
			}
		}	
	}
	
	protected double getWeight( BeliefState bs1, BeliefState bs2 ){
		Map mPredcessors = (Map) m_mPredcessors.get( bs1 );
		Double dWeight = (Double) mPredcessors.get( bs2 );
		if( dWeight == null )
			return 0.0;
		return dWeight.doubleValue();
	}
	
	public void updatePriorities( BeliefState bsCurrent, 
			double dDelta, double dEpsilon, 
			Vector vBeliefStates, PriorityQueue pq, double dGamma ){
		Iterator itPreds = ((Map) m_mPredcessors.get( bsCurrent )).entrySet().iterator();
		Map.Entry e = null;
		BeliefState bsPred = null;
		double dWeight = 0.0;
		
		while( itPreds.hasNext() ){
			e = (Entry) itPreds.next();
			bsPred = (BeliefState) e.getKey();
			dWeight = ((Number) e.getValue()).doubleValue();
			if( dWeight * dDelta > dEpsilon ){
				bsPred.increasePriority( dWeight * dDelta );
				pq.insert( bsPred );
			}
		}
	}

}
