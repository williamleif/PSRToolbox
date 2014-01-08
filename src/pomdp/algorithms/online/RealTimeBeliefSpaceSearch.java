package pomdp.algorithms.online;

import java.util.TreeMap;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.ForwardSearchValueIteration;
import pomdp.algorithms.pointbased.HeuristicSearchValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RealTimeBeliefSpaceSearch extends ValueIteration {

	protected TreeMap<BeliefState,Number[]> m_htBeliefStateTable;
	protected LinearValueFunctionApproximation m_vHeuristicValueFunction;
	protected int m_iMaxDepth;
	protected double m_dMaxValue;
	
	public RealTimeBeliefSpaceSearch( POMDP pomdp, int iMaxDepth ){
		super( pomdp );
		m_iMaxDepth = iMaxDepth;
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false ); 
		m_htBeliefStateTable = new TreeMap<BeliefState, Number[]>();
		
		ForwardSearchValueIteration fsvi = new ForwardSearchValueIteration( pomdp );
		fsvi.valueIteration( 25 );
		m_vHeuristicValueFunction = fsvi.getValueFunction();
	}

	public RealTimeBeliefSpaceSearch( POMDP pomdp ){
		this( pomdp, 3 );
	}

	
	protected int[] sortActions( BeliefState bs ){
		int[] aiActions = new int[m_cActions];
		double[] adActionValues = new double[m_cActions];
		int iAction = 0, iActionIdx = 0, iMaxAction = 0;
		double dMaxValue = 0.0;
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			adActionValues[iAction] = m_vfMDP.getQValue( bs, iAction );
		}

		for( iActionIdx = 0 ; iActionIdx < m_cActions ; iActionIdx++ ){
			dMaxValue = MIN_INF;
			iMaxAction = -1;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( adActionValues[iAction] > dMaxValue ){
					dMaxValue = adActionValues[iAction];
					iMaxAction = iAction;
				}
			}
			aiActions[iActionIdx] = iMaxAction;
			adActionValues[iMaxAction] = MIN_INF;
		}
		
		return aiActions;
	}
	
	public double searchValue( BeliefState bs, int iDepth, double dAccumulatedReward ){
		//Pair<Double, Integer> pResult = new Pair<Double, Integer>( 0.0, -1 );
		int iFinalAction = -1;
		double dFinalValue = 0.0;
		double dQMDPValue = 0.0, dHValue = 0.0, dImmediateReward = 0.0;
		double dQValue = 0.0, dPr = 0.0, dVNext = 0.0, dMaxQValue = MIN_INF, dValue = 0.0;
		BeliefState bsNext = null;
		int iObservation = 0, iAction = 0, iActionIdx = 0, iMaxAction = -1;
		boolean bPruned = false;
		double dGammaFactor = Math.pow( m_dGamma, m_iMaxDepth - iDepth );
		int[] aiSortedActions = null;
		
		if( iDepth == 0 ){
			AlphaVector vMaxAlpha = m_vHeuristicValueFunction.getMaxAlpha( bs );
			dValue = m_vHeuristicValueFunction.valueAt( bs );
			dFinalValue = dValue;
			iFinalAction = vMaxAlpha.getAction();
			if( ( dGammaFactor * dValue + dAccumulatedReward ) > m_dMaxValue ){
				m_dMaxValue = dGammaFactor * dValue + dAccumulatedReward;
			}
		}
		else{			
			dQMDPValue = m_vfMDP.getValue( bs );
			dHValue = dAccumulatedReward + dGammaFactor * dQMDPValue;
			if( dHValue < m_dMaxValue ){
				dFinalValue = dQMDPValue;
				iFinalAction = m_vfMDP.getAction( bs );
				bPruned = true;
			}
			else{
				//search recursively
				aiSortedActions = sortActions( bs );
				for( iActionIdx = 0 ; iActionIdx < m_cActions ; iActionIdx++ ){
					iAction = aiSortedActions[iActionIdx];
					dImmediateReward = m_pPOMDP.immediateReward( bs, iAction );
					dQValue = dImmediateReward;
					for( iObservation = 0 ;iObservation < m_cObservations ; iObservation++ ){
						dPr = bs.probabilityOGivenA( iAction, iObservation );
						if( dPr != 0.0 ){
							bsNext = bs.nextBeliefState( iAction, iObservation );
							if( bsNext != null ){
								dVNext = searchValue( bsNext, iDepth - 1, dAccumulatedReward + dImmediateReward );
								dQValue += m_dGamma * dVNext * dPr;
							}
						}
					}
					if( dQValue > dMaxQValue ){
						dMaxQValue = dQValue;
						iMaxAction = iAction;
					}
				}
				dFinalValue = dMaxQValue;
				iFinalAction = iMaxAction;
			}
		}

		int iOffset = 0;
		String sOutput = "";
		for( iOffset = 0 ; iOffset < m_iMaxDepth - iDepth ; iOffset++ ){
			sOutput += "\t";
		}
		sOutput += bs + " = " + round( dFinalValue, 2 ) + ", max = " + round( m_dMaxValue, 3 );
		if( bPruned )
			sOutput += " pruned";
		Logger.getInstance().logln( sOutput );

		storeBeliefPointValue( bs, dFinalValue, iFinalAction, m_iMaxDepth - iDepth );
		return dFinalValue;
	}
	
	protected Number[] getStoredBeliefPointEntry( BeliefState bs ){
		return m_htBeliefStateTable.get( bs );
	}
	
	protected void storeBeliefPointValue( BeliefState bs, double dValue, int iAction ){
		Number[] pEntry = new Number[]{ dValue, iAction, m_iMaxDepth };
		m_htBeliefStateTable.put( bs, pEntry );
	}

	protected void storeBeliefPointValue( BeliefState bs, double dValue, int iAction, int iDepth ){
		Number[] pFullEntry = m_htBeliefStateTable.get( bs );
		if( pFullEntry == null ){
			pFullEntry = new Number[]{ dValue, iAction, iDepth };
			m_htBeliefStateTable.put( bs, pFullEntry );
		}
		else{
			int iPreviousDepth = pFullEntry[2].intValue();
			if( iDepth <= iPreviousDepth ){
				pFullEntry[0] = dValue;
				pFullEntry[1] = iAction;
				pFullEntry[2] = iDepth;
			}
		}
	
	}
	
	public int getAction( BeliefState bs ){
		m_dMaxValue = MIN_INF;
		Number[] pBestAction = null;
		int iAction = 0;
		if( m_bExploring ){
			searchValue( bs, m_iMaxDepth, 0.0 );			
		}
		
		pBestAction = getStoredBeliefPointEntry( bs ); 

		if( pBestAction != null ){
			iAction = pBestAction[1].intValue();
		}
		else{
			iAction = m_vfMDP.getAction( bs );
		}
		return iAction;
	}

	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon,
			double targetValue, int maxRunningTime, int numEvaluations) {
		throw new NotImplementedException();		
	}


}


