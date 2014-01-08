package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.utilities.datastructures.Heap;
import pomdp.utilities.datastructures.PriorityQueue;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;
public abstract class IterativeValueIteration extends ValueIteration {

	protected double m_dExplorationRate;
	protected int m_cUnchangedIterations;
	protected int m_cMaxUnchangedIterations = 50;
	protected int m_cIterations;
	protected LinearValueFunctionApproximation m_vNextAlphaVectors;
	protected PriorityQueue m_pqBeliefStates;
	protected boolean m_bAlwaysBackup;
	protected Vector m_vObservedBeliefPoints;
	protected Vector m_vAllObservedBeliefPoints;
	protected LinearValueFunctionApproximation m_vQMDPFunction;
	protected LinearValueFunctionApproximation m_vOptimizedValueFunction;
	protected Vector m_vBeliefPoints;
	protected int m_cSteps;
	
	public IterativeValueIteration( POMDP pomdp, double dEpsilon, 
									LinearValueFunctionApproximation vOptimizedValueFunction, Vector vBeliefPoints, boolean bPersistQValues ){
		super( pomdp );
		m_cUnchangedIterations = 0;
		m_dEpsilon = dEpsilon;
		m_vNextAlphaVectors = new LinearValueFunctionApproximation( m_dEpsilon, false );
		m_cIterations = 0;
		m_pqBeliefStates = new Heap();
		m_bAlwaysBackup = true;
		m_dExplorationRate = 0.5;
		m_vObservedBeliefPoints = new Vector();
		m_vAllObservedBeliefPoints = new Vector();
		if( bPersistQValues )
			m_vfMDP.persistQValues( true );
		m_vfMDP.valueIteration( 1000, 0.001 );
		m_vQMDPFunction = m_vfMDP.getValueFunction();
		m_vOptimizedValueFunction = vOptimizedValueFunction;
		m_vBeliefPoints = vBeliefPoints;
		m_cSteps = 0;
	}
	
	protected double maxDistance( LinearValueFunctionApproximation vFirstValueFunction, 
									LinearValueFunctionApproximation vSecondValueFunction,
									Vector vBeliefStates ){
		Iterator it = vBeliefStates.iterator();
		BeliefState bs = null, bsMax = null;
		double dMaxDist = 0.0, dV1 = 0.0, dV2 = 0.0, dDelta = 0.0, dAvg = 0.0;
				
		while( it.hasNext() ){
			bs = (BeliefState) it.next();
			dV1 = vFirstValueFunction.valueAt( bs );
			dV2 = vSecondValueFunction.valueAt( bs );
			dDelta = diff( dV1, dV2 );
			if( dDelta > dMaxDist ){
				dMaxDist = dDelta;
				bsMax = bs;
			}
			dAvg += dDelta;
		}
		
		Logger.getInstance().logln( "maxDist: max = " + round( dMaxDist, 3 ) + " BS = " + bsMax + " avg = " + round( dAvg / vBeliefStates.size(), 3 ) );
		return dMaxDist;
	}
	
	protected boolean sameActions( LinearValueFunctionApproximation vFirstValueFunction, 
									LinearValueFunctionApproximation vSecondValueFunction,
									Vector vBeliefStates ){
		Iterator it = vBeliefStates.iterator();
		BeliefState bs = null, bsMax = null;
		int iAction1 = 0, iAction2 = 0;
		boolean bResult = true;
		
		while( it.hasNext() ){
			bs = (BeliefState) it.next();
			iAction1 = vFirstValueFunction.getBestAction( bs );
			iAction2 = vSecondValueFunction.getBestAction( bs );
			if( iAction1 != iAction2 ){
				Logger.getInstance().logln( "sameActions: does not agree on bs = " + bs + " a_perseus = " + iAction1 + " a_online = " + iAction2 );
				bResult = false;
			}
		}
		return bResult;
	}
	
	protected boolean done(){
		return sameActions( m_vValueFunction, m_vOptimizedValueFunction, m_vBeliefPoints );
	}
	
	protected void prioritizedSweepingImprove( BeliefState bsLatest, int cUpdates ){
		int iUpdate = 0;
		BeliefState bsPrioritizesBeliefState = null;
		double dDelta = prioritizedSweepingImprove( bsLatest, m_bAlwaysBackup );
		if( dDelta > m_dEpsilon ){
			for( iUpdate = 0 ; iUpdate < cUpdates ; iUpdate++ ){
				bsPrioritizesBeliefState = (BeliefState)m_pqBeliefStates.extractMax();
				if( bsPrioritizesBeliefState != null )
					prioritizedSweepingImprove( bsPrioritizesBeliefState, true );
			}
		}
	}
	
	protected double prioritizedSweepingImprove( BeliefState bsCurrent, boolean bForceBackup ){
		double dDelta = improve( bsCurrent, bForceBackup );
		if( dDelta > m_dEpsilon ){
			Iterator itPreds = bsCurrent.getPredecessors().iterator();
			BeliefState bsPred = null;
			while( itPreds.hasNext() ){
				bsPred = (BeliefState) itPreds.next();
				m_pqBeliefStates.insert( bsPred ); //will insert only if it is not in
				if( bsPred.getPriority() < dDelta ){
					bsPred.increasePriority( m_dGamma * dDelta );
				}
			}
		}
		return dDelta;
	}
	
	protected double improve( BeliefState bsCurrent, boolean bForceBackup ){
		double dCurrentValue = valueAt( bsCurrent );
		double dNextValue = m_vNextAlphaVectors.valueAt( bsCurrent );
		AlphaVector avNext = null;
		double dRetVal = 0.0;
		
		addObservedBeliefState( bsCurrent );
		
		if( bForceBackup || ( dNextValue < dCurrentValue ) ){
			avNext = backup( bsCurrent );
			dNextValue = avNext.dotProduct( bsCurrent );
			if( dNextValue < dCurrentValue ){
				avNext = getMaxAlpha( bsCurrent );
				dNextValue = dCurrentValue;
				m_vNextAlphaVectors.add( avNext );
			}
			else if( dNextValue > dCurrentValue + m_dEpsilon ){
				m_vNextAlphaVectors.add( avNext );
				m_cUnchangedIterations = 0;
			}
			else
				m_cUnchangedIterations++;
			
			dRetVal = dNextValue - dCurrentValue;
		}
		else{
			m_cUnchangedIterations++;
		}
		if( ( m_vObservedBeliefPoints.size() > 10 ) &&
				( m_vNextAlphaVectors.size() > 0 ) ){
			if( false && m_vValueFunction.size() > 1 && m_vNextAlphaVectors.size() == 1 ){
				Logger.getInstance().logln( m_vValueFunction );
				Logger.getInstance().logln( m_vNextAlphaVectors );
			}
			
			m_vValueFunction = m_vNextAlphaVectors;
			m_vNextAlphaVectors = new LinearValueFunctionApproximation( m_dEpsilon, false );
			m_cUnchangedIterations = 0;
			m_vObservedBeliefPoints.clear();
			m_cIterations++;
			if( m_dExplorationRate > 0.1 )
				m_dExplorationRate *= 0.9;
		}
		return dRetVal;
	}
	
	public int getBestAction( BeliefState bs ){
		AlphaVector avMaxPreviousAlpha = getMaxAlpha( bs );
		AlphaVector avMaxNextAlpha = m_vNextAlphaVectors.getMaxAlpha( bs );
		double dPreviousValue = avMaxPreviousAlpha.dotProduct( bs );
		double dNextValue = MIN_INF;
		if( avMaxNextAlpha != null )
			dNextValue = avMaxNextAlpha.dotProduct( bs );
		if( dPreviousValue > dNextValue ){
			return avMaxPreviousAlpha.getAction();
		}
		else{
			return avMaxNextAlpha.getAction();
		}
	}
	
	public int getAction( BeliefState bsCurrent ){
		double dRand = m_rndGenerator.nextDouble();
		int iAction = 0;
		if( m_bExploring ){
			prioritizedSweepingImprove( bsCurrent, 10 );
			if( dRand > m_dExplorationRate ){
				iAction = getBestAction( bsCurrent );
			}
			else{
				iAction = m_vQMDPFunction.getBestAction( bsCurrent );
			}
			if( m_dExplorationRate > 0.1 ){
				m_dExplorationRate *= 0.9995;
			}
			m_cSteps++;
		}
		else{
			iAction = getBestAction( bsCurrent );
		}
		return iAction;
	}
	
	protected void addObservedBeliefState( BeliefState bs ){
		addOnce( bs, m_vObservedBeliefPoints );
	}
	
	protected void addOnce( BeliefState bs, Vector vBeliefStates ){
		int iBelief = 0, cBeliefs = vBeliefStates.size();
		for( iBelief = 0 ; iBelief < cBeliefs ; iBelief++ ){
			if( vBeliefStates.get( iBelief ) == bs ){
				return;
			}
		}
		vBeliefStates.add( bs );
	}
	
	
	//V_n+1(b)=max_a{r_a b + \gamma \sum_o[pr(o|a,b) V(\tau(a,b,o)]}
	protected double computeBackupValue( BeliefState bs, boolean bDebug ){
		double dQValue = 0.0, dR = 0.0, dPr = 0.0, dVNext = 0.0, dMaxQValue = MIN_INF;
		BeliefState bsNext = null;
		int iObservation = 0, iAction = 0, iMaxAction = -1;
		AlphaVector avAlpha = null;
		
		if( bDebug )
			Logger.getInstance().logln( "computeBackupValue1" );
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dR = m_pPOMDP.immediateReward( bs, iAction );
			dQValue = dR;
			for( iObservation = 0 ;iObservation < m_cObservations ; iObservation++ ){
				dPr = bs.probabilityOGivenA( iAction, iObservation );
				if( dPr > 0 ){
					bsNext = bs.nextBeliefState( iAction, iObservation );
					avAlpha = getMaxAlpha( bsNext );
					dVNext = avAlpha.dotProduct( bsNext );
					dQValue += m_dGamma * dVNext * dPr;			
				}
				if( bDebug )
					Logger.getInstance().logln( "V( " + iAction + ", " + iObservation + " ) = " + dVNext * dPr );
			}
			
			if( bDebug )
				Logger.getInstance().logln( "V( " + iAction + " ) = " + dQValue );
			
			if( dQValue > dMaxQValue ){
				dMaxQValue = dQValue;
				iMaxAction = iAction;
			}
		}
		return dMaxQValue;
	}
		
	protected void decayValueFunction( double dDelta ){
		Iterator it = m_vValueFunction.iterator();
		AlphaVector av = null;
		while( it.hasNext() ){
			av = (AlphaVector)it.next();
			av.decay( dDelta );
		}
	}
	
	protected void ageValueFunction(){
		Iterator it = m_vValueFunction.iterator();
		AlphaVector av = null;
		while( it.hasNext() ){
			av = (AlphaVector)it.next();
			av.age();
		}
	}
}
