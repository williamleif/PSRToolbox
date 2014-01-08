package pomdp.algorithms.pointbased;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.CreateBeliefSpaces;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.concurrent.ComputeBellmanError;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.utilities.datastructures.MultiValueMap;

public class PrioritizedValueIteration extends PrioritizedPerseus{

	
	private MultiValueMap<Double,BeliefState>[] m_mSuccessorsStateValues;
	private MultiValueMap<Double,BeliefState>[] m_mBeliefsStateValues;
	private Map<BeliefState,Vector<BeliefState>> m_mPredecessors;
	private Map<BeliefState, Double> m_mBeliefStateValues;
	private MultiValueMap<Double, BeliefState> m_mPriorities;
	private Map<BeliefState, Double> m_mReversedPriorities;
	private boolean m_bFirstErrorComputation;
	private ComputeBellmanError[] m_atThreads;
	
	public PrioritizedValueIteration( POMDP pomdp ){
		super( pomdp );
		m_dMinimalProb = 0.0;
		m_bFirstErrorComputation = true;
		m_atThreads = null;
	}
		
	
	protected void init( Vector<BeliefState> vBeliefPoints ){
		int iState = 0, iBeliefPoint = 0;
		AlphaVector avMax = null;
		Iterator<Entry<Integer, Double>> itNonZero = null;
		Entry<Integer, Double> e = null;
		Vector<BeliefState> vPreds = null;
		double dError = 0.0;
		long lTimeBefore = System.currentTimeMillis();
		
		Logger.getInstance().logFull( "PVI", 0, "init", "Started initializing PVI data structures" );
		
		m_vBeliefPoints = vBeliefPoints;
		
		m_mSuccessorsStateValues = new MultiValueMap[m_cStates];
		m_mBeliefsStateValues = new MultiValueMap[m_cStates];
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			m_mSuccessorsStateValues[iState] = new MultiValueMap<Double, BeliefState>();
			m_mBeliefsStateValues[iState] = new MultiValueMap<Double, BeliefState>();
		}
		
		m_mPredecessors = new TreeMap<BeliefState, Vector<BeliefState>>( BeliefStateComparator.getInstance() );
		m_mBeliefStateValues = new TreeMap<BeliefState, Double>( BeliefStateComparator.getInstance() );
		
		m_mPriorities = new MultiValueMap<Double, BeliefState>();
		m_mReversedPriorities = new TreeMap<BeliefState, Double>( BeliefStateComparator.getInstance() );
		
		for( BeliefState bsCurrent : m_vBeliefPoints ){
			avMax = m_vValueFunction.getMaxAlpha( bsCurrent );
			m_mBeliefStateValues.put( bsCurrent, avMax.dotProduct( bsCurrent ) );
			itNonZero = bsCurrent.getNonZeroEntries().iterator();
			while( itNonZero.hasNext() ){
				e = itNonZero.next();
				m_mBeliefsStateValues[e.getKey()].put( avMax.valueAt( e.getKey() ), bsCurrent );
			}
			for( BeliefState bsSuccessor : bsCurrent.computeSuccessors() ){
				if( !m_mPredecessors.containsKey( bsSuccessor ) ){
					vPreds = new Vector<BeliefState>();
					m_mPredecessors.put( bsSuccessor, vPreds );
					avMax = m_vValueFunction.getMaxAlpha( bsSuccessor );
					m_mBeliefStateValues.put( bsSuccessor, avMax.dotProduct( bsSuccessor ) );
					itNonZero = bsSuccessor.getNonZeroEntries().iterator();
					while( itNonZero.hasNext() ){
						e = itNonZero.next();
						m_mSuccessorsStateValues[e.getKey()].put( avMax.valueAt( e.getKey() ), bsSuccessor );
					}
				}
				else{
					vPreds = m_mPredecessors.get( bsSuccessor );
				}
				vPreds.add( bsCurrent );
			}
			iBeliefPoint++;
			if( iBeliefPoint % 5 == 0 )
				Logger.getInstance().log( "." );
		}
		for( BeliefState bsCurrent : m_vBeliefPoints ){
			dError = computeNewValue( bsCurrent ) - m_mBeliefStateValues.get( bsCurrent );
			m_mPriorities.put( dError, bsCurrent );
			m_mReversedPriorities.put( bsCurrent, dError );
		}
		
		
		Logger.getInstance().logln( "." );
		
		long lTimeAfter = System.currentTimeMillis();
		
		Logger.getInstance().logFull( "PVI", 0, "init", "Done initializing PVI data structures, time " + ( lTimeAfter - lTimeBefore ) );
    }
	
	public double computeNewValue( BeliefState bs ){
		int iAction = 0, iObservation = 0;
		double dActionValue = 0.0, dMaxValue = Double.NEGATIVE_INFINITY, dPr = 0.0, dSuccessorValue = 0.0;
		BeliefState bsSuccessor = null;
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dActionValue = m_pPOMDP.immediateReward( bs );
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				dPr = bs.probabilityOGivenA( iAction, iObservation );
				if( dPr > 0.0 ){
					bsSuccessor = bs.nextBeliefState( iAction, iObservation );
					if( !m_mBeliefStateValues.containsKey( bsSuccessor ) )
						Logger.getInstance().logln( "*" );
					dSuccessorValue = m_mBeliefStateValues.get( bsSuccessor );
					dActionValue += dSuccessorValue * dPr;
				}
			}
			if( dActionValue > dMaxValue )
				dMaxValue = dActionValue;
		}
		
		return dMaxValue;
	}
	
	private void updateDataStructures( BeliefState bs, AlphaVector avNew, boolean bSuccessor ){
		int iState = 0;
		double dValue = 0.0, dPreviousValue = 0.0;
		Iterator<Entry<Integer, Double>> itNonZero = bs.getNonZeroEntries().iterator();
		AlphaVector avPreviousMax = bs.getMaxAlpha();
		MultiValueMap<Double,BeliefState>[] mStateValues = m_mBeliefsStateValues;
		if( bSuccessor )
			mStateValues = m_mSuccessorsStateValues;
		while( itNonZero.hasNext() ){
			iState = itNonZero.next().getKey();
			dValue = avNew.valueAt( iState );
			dPreviousValue = avPreviousMax.valueAt( iState );
			mStateValues[iState].removeEntry( dPreviousValue, bs );
			mStateValues[iState].put( dValue, bs );
		}
	}
	
	private Vector<BeliefState> getUpdatedBeliefStates( AlphaVector avNew ){
		int iState = 0;
		double dValue = 0.0, dOldValue = 0.0, dNewValue = 0.0;
		Vector<BeliefState> vUpdated= new Vector<BeliefState>(), vUpdatedSuccessors = new Vector<BeliefState>();
		Vector<BeliefState> vRequireErrorUpdate = new Vector<BeliefState>();
		Map<Double, BeliefState> mSmallerValues = null;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue = avNew.valueAt( iState );
			mSmallerValues = m_mBeliefsStateValues[iState].headMap( dValue );
			for( BeliefState bs : mSmallerValues.values() ){
				if( !vUpdated.contains( bs ) ){
					dOldValue = m_mBeliefStateValues.get( bs );
					dNewValue = avNew.dotProduct( bs );
					if( dNewValue > dOldValue ){
						m_mBeliefStateValues.put( bs, dNewValue );
						updateDataStructures( bs, avNew, false );
						if( !vRequireErrorUpdate.contains( bs ) )
							vRequireErrorUpdate.add( bs );
					}
					vUpdated.add( bs );
				}
			}		
			mSmallerValues = m_mSuccessorsStateValues[iState].headMap( dValue );
			for( BeliefState bsSuccessor : mSmallerValues.values() ){
				if( !vUpdatedSuccessors.contains( bsSuccessor ) ){
					dOldValue = m_mBeliefStateValues.get( bsSuccessor );
					dNewValue = avNew.dotProduct( bsSuccessor );
					if( dNewValue > dOldValue ){
						m_mBeliefStateValues.put( bsSuccessor, dNewValue );
						updateDataStructures( bsSuccessor, avNew, true );
						for( BeliefState bsPred : m_mPredecessors.get( bsSuccessor ) ){
							if( !vRequireErrorUpdate.contains( bsPred ) )
								vRequireErrorUpdate.add( bsPred );
						}
					}
					vUpdatedSuccessors.add( bsSuccessor );
				}
			}		
		}
		return vRequireErrorUpdate;
	}
	
	private int updatePriorities( AlphaVector avNew ){
		Vector<BeliefState> vRequireUpdate = getUpdatedBeliefStates( avNew );
		double dError = 0.0, dPreviousError = 0.0;
		int cChanged = 0;
		
		for( BeliefState bsCurrent : vRequireUpdate ){
			dPreviousError = m_mReversedPriorities.get( bsCurrent );
			dError = computeNewValue( bsCurrent ) - m_mBeliefStateValues.get( bsCurrent );
			if( dError != dPreviousError ){
				m_mPriorities.removeEntry( dPreviousError, bsCurrent );
				m_mPriorities.put( dError, bsCurrent );
				m_mReversedPriorities.put( bsCurrent, dError );
				cChanged++;
			}
		}
		return cChanged;
	}
	
	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		long cDotProducts = AlphaVector.dotProductCount();
		int iIteration = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		boolean bDone = false;
		Pair pComputedADRs = new Pair();
		double dMaxDelta = 0.0;
		Vector<BeliefState> vBeliefPoints = CreateBeliefSpaces.createHeuristicSpace( m_pPOMDP, m_rndGenerator.nextInt(), 200 );

		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		dEpsilon = 0.0001;

		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		
		Logger.getInstance().logln( "Starting " + getName() + " with " + vBeliefPoints.size() + " belief points" );
		
		m_vBeliefPoints = vBeliefPoints;
		m_dMinimalProb = 0.0;//0.25;

		if( ExecutionProperties.useHighLevelMultiThread() ){
			int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
			m_atThreads = new ComputeBellmanError[cThreads];
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				m_atThreads[iThread] = new ComputeBellmanError( m_pPOMDP, m_vValueFunction );
			}
			
			iThread = 0;
			for( BeliefState bs : m_vBeliefPoints ){
				m_atThreads[iThread].addBelief( bs );
				iThread = ( iThread + 1 ) % cThreads; 
			}
			
		}
		
		
		
		for( iIteration = 0 ; ( iIteration < cMaxSteps ) && !bDone ; iIteration++ ){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			dMaxDelta = executeIteration( 10, dEpsilon );
			if( dMaxDelta < dEpsilon ){
				bDone = true;
			}
			lCurrentTime = System.currentTimeMillis();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;
			rtRuntime.gc();
			
			if( ( ( lCPUTimeTotal  / 1000000000 ) >= 2 ) ){
				bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs ) || bDone;

				Logger.getInstance().logln( "PVI: Iteration " + iIteration + 
						" |V| = " + m_vValueFunction.size() + 
						" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
						" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
						" Time " + ( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" #backups " + m_cBackups + 
						" V changes " + m_vValueFunction.getChangesCount() +
						" max delta " + round( dMaxDelta, 3 ) +
						" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
						" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 );
			}
			else{
				Logger.getInstance().logln( "PVI: Iteration " + iIteration + 
						" |Vn| = " + m_vValueFunction.size() +
						" time " + 	( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" #backups " + m_cBackups + 
						" V changes " + m_vValueFunction.getChangesCount() +
						" max delta " + round( dMaxDelta, 3 ) +
						"" );
			
			}

		}

		m_cDotProducts = AlphaVector.dotProductCount() - cDotProducts;
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;
		
		Logger.getInstance().logln( "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + 
				" |BS| = " + vBeliefPoints.size() +
				" |V| = " + m_vValueFunction.size() + 
				" backups = " + m_cBackups + 
				" GComputations = " + AlphaVector.getGComputationsCount() +
				" Dot products = " + m_cDotProducts );
	}	


	public String getName(){
		return "PVI";
	}

	protected double executeIteration( int cIterations, double dEpsilon ){
		BeliefState bsCurrent = null;
		AlphaVector avNew = null;
		int iIteration = 0, cChanged = 0;	
		boolean bDone = false;
		double dMaxError = 0.0;
		double dError = 0.0, dValue = 0.0;
		
		m_bFirstErrorComputation = true;
		for( iIteration = 0 ; iIteration < cIterations && !bDone  ; iIteration++ ){
			if( ExecutionProperties.useHighLevelMultiThread() )
				bsCurrent = chooseMultiThreaded( dEpsilon );
			else{
				bsCurrent = choose( 25, dEpsilon );
			}
			m_bFirstErrorComputation = false;
			if( bsCurrent != null ){				
				dError = computeBellmanError( bsCurrent );

				avNew = backup( bsCurrent );

				if( dError > dMaxError )
					dMaxError = dError;
				m_vValueFunction.addPrunePointwiseDominated( avNew );
			}
			else{
				if( m_dMinimalProb > 0.001 )
					m_dMinimalProb /= 2;
				else
					bDone = true;
			}
		}
		
		Logger.getInstance().logln( "Done iteration, max error = " + round( dMaxError, 3 ) + 
				" last error = " + round( dError, 3 ) + 
				" #backups " + iIteration +
				" #computations " + m_cComputations +
				" min prob " + m_dMinimalProb );
		
		return dMaxError;
	}

	private BeliefState choose() {
		return m_mPriorities.lastValue();
	}


	protected double singleValueFunctionIteration( int iIteration, double dEpsilon ){
		return executeIteration( iIteration, dEpsilon );
	}
	
	protected void finalizeIteration(){
	}
	
	protected BeliefState choose( int cPoints, double dEpsilon ){
		Iterator itPoints = null;
		BeliefState bsCurrent = null, bsMax = null;
		double dError = 0.0, dMaxError = dEpsilon;
		double dProb = 0.0, dValue = 0.0;
		Vector vTmpBeliefPoints = new Vector( m_vBeliefPoints );
		
		long lBefore = System.currentTimeMillis();
		
		if( ExecutionProperties.getDebug() ){
			itPoints = m_vBeliefPoints.iterator();
			while( itPoints.hasNext() ){
				bsCurrent = (BeliefState) itPoints.next();
				dValue = valueAt( bsCurrent );
				dError = computeBellmanError( bsCurrent );
				Logger.getInstance().logln( "bs" + bsCurrent.getId() + 
						", e = " + round( dError, 5 ) + ", v = " + round( dValue, 3 ) + 
						", max alpha " + m_vValueFunction.getMaxAlpha( bsCurrent ) );
				
			}	
			System.exit( 0 );
		}
			
		
		while( ( bsMax == null ) && ( vTmpBeliefPoints.size() > 0 ) ){
			if( cPoints == -1 )
				dProb = 1.0;
			else
				dProb = ( 1.0 * cPoints ) / m_vBeliefPoints.size();
			itPoints = vTmpBeliefPoints.iterator();
			while( itPoints.hasNext() ){
				bsCurrent = (BeliefState) itPoints.next();	
				if( m_rndGenerator.nextDouble( 1.0 ) <= dProb ){			
					itPoints.remove();
					dError = computeBellmanError( bsCurrent );
					if( dError > dMaxError ){
						dMaxError = dError;
						bsMax = bsCurrent;
					}
				}
			}
		}
		
		if( ExecutionProperties.getDebug()  )
			Logger.getInstance().log( "PerseusValueIteration", 0, "singleValueFunctionIteration", "err " + round( dMaxError, 3 ) + ", bs" + bsCurrent.getId() + ", max " + bsMax );

		//Logger.getInstance().logln( "Returning " + bsMax + " with error " + dMaxError + " value " + m_vValueFunction.valueAt( bsMax ) );
		long lAfter = System.currentTimeMillis();
		//Logger.getInstance().logln( "Finished choose in " + ( lAfter - lBefore ) / 1000.0 );
		return bsMax;
	}

	protected BeliefState chooseMultiThreaded( double dEpsilon ){
		BeliefState bsMax = null;
		double dError = 0.0, dMaxError = dEpsilon;
		int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			m_atThreads[iThread].clearResults();
			ThreadPool.getInstance().addTask( m_atThreads[iThread] );
		}
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().waitForTask( m_atThreads[iThread] );
			dError = m_atThreads[iThread].getMaxError();
			if( dError > dMaxError ){
				dMaxError = dError;
				bsMax = m_atThreads[iThread].getMaxErrorBelief();
			}
		}
		
		return bsMax;
	}

}
