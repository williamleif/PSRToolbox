package pomdp.algorithms.pointbased;

import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.valuefunction.JigSawValueFunction;
import pomdp.valuefunction.MDPValueFunction;

public class HeuristicSearchValueIteration extends ValueIteration {
	protected JigSawValueFunction m_vfUpperBound;
	protected int m_cApplyHComputations;
	protected int m_cNewPointComputations;
	protected int m_cVisitedBeliefStates;
	protected double m_dMaxWidthForIteration;
	private static double m_dExplorationFactor;
	
	public HeuristicSearchValueIteration( POMDP pomdp, double dExplorationFactor, boolean bUseFIB  ){
		super( pomdp );
		
		
		if( !m_vfMDP.persistQValues() ){
			m_vfMDP = new MDPValueFunction( pomdp, 0.0 );
			m_vfMDP.persistQValues( true );
			m_vfMDP.valueIteration( 100, m_dEpsilon );
		}
		m_vfUpperBound = new JigSawValueFunction( pomdp, m_vfMDP, bUseFIB );
		m_cNewPointComputations = 0;
		m_cApplyHComputations = 0;
		m_cVisitedBeliefStates = 0;
		m_dMaxWidthForIteration = 0.0;
		m_dExplorationFactor = dExplorationFactor;
	}
	public HeuristicSearchValueIteration( POMDP pomdp, boolean bUseFIB ){
		this( pomdp, 0.0, bUseFIB );
	}
	
	protected void applyH( BeliefState bs ){
		long lTimeBefore = 0, lTimeAfter = 0;
		
		if( ExecutionProperties.getReportOperationTime() )
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
	
		m_vfUpperBound.updateValue( bs );
		
		if( ExecutionProperties.getReportOperationTime() ){
			lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			
			m_cTimeInHV += ( lTimeAfter - lTimeBefore ) / 1000000;
		}
	}

	public int getAction( BeliefState bsCurrent ){
		AlphaVector avMaxAlpha = m_vValueFunction.getMaxAlpha( bsCurrent );
		return avMaxAlpha.getAction();
	}
	
	protected String toString( double[][] adArray ){
		int i = 0, j = 0;
		String sRes = "";
		for( i = 0 ; i < adArray.length ; i++ ){
			for( j = 0 ; j < adArray[i].length ; j++ ){
				sRes += adArray[i][j] + " ";
			}
			sRes += "\n";
		}
		return sRes;
	}

	protected double excess( BeliefState bsCurrent, double dEpsilon, double dDiscount ){
		return width( bsCurrent ) - ( dEpsilon / dDiscount );
	}
	
	protected double width( BeliefState bsCurrent ){
		double dUpperValue = 0.0, dLowerValue = 0.0, dWidth = 0.0;
		dUpperValue = m_vfUpperBound.valueAt( bsCurrent );
		dLowerValue = valueAt( bsCurrent );
		dWidth = dUpperValue - dLowerValue;	
		
		return dWidth;
	}
		
	public String getName(){
		return "HSVI";
	}
	
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations)
	{
		BeliefState bsInitial = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState();
		double dInitialWidth = width( bsInitial );
		int iIteration = 0, iMaxDepth = 0;
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		boolean bDone = false;
		Pair<Double, Double> pComputedADRs = new Pair<Double, Double>();
		Vector<BeliefState> vObservedBeliefStates = new Vector<BeliefState>();
		int cUpperBoundPoints = 0, cNoChange = 0;
		String sMsg = "";
		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		
		int cValueFunctionChanges = 0;
		
		Logger.getInstance().logln( "Begin " + getName() + ", Initial width = " + dInitialWidth );
		
		for( iIteration = 0 ; ( iIteration < cIterations ) && !bDone && !m_bTerminate ; iIteration++ ){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			
			m_dMaxWidthForIteration = 0.0;
			iMaxDepth = explore( bsInitial, dEpsilon, 0, 1.0, vObservedBeliefStates );
			if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
				m_vfUpperBound.pruneUpperBound();
				cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
			}						
			m_cVisitedBeliefStates += iMaxDepth;
			dInitialWidth = width( bsInitial );			
			
			lCurrentTime = System.currentTimeMillis();
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;
			

			
			if( ( iIteration >= 5 ) && ( ( lCPUTimeTotal  / 1000000000 ) >= 0 ) && ( iIteration % 5 == 0 ) && m_vValueFunction.getChangesCount() > cValueFunctionChanges ){
								
				bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );

				cValueFunctionChanges = m_vValueFunction.getChangesCount();
				
				rtRuntime.gc();
				
				sMsg = getName() + ": Iteration " + iIteration + 
									" initial width " + round( dInitialWidth, 3 ) +
									" V(b) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
									" V^(b) " + round( m_vfUpperBound.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
									//" max width bs = " + bsMaxWidth + 
									//" max width " + round( width( bsMaxWidth ), 3 ) +
									" max depth " + iMaxDepth +
									" max width " + round( m_dMaxWidthForIteration, 3 ) +
									" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
									" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
									" Time " + ( lCurrentTime - lStartTime ) / 1000 +
									" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
									" CPU total " + lCPUTimeTotal  / 1000000000 +
									" |V| " + m_vValueFunction.size() +
									" |V^| " + m_vfUpperBound.getUpperBoundPointCount() +
									" V changes " + m_vValueFunction.getChangesCount() +
									" #ObservedBS = " + vObservedBeliefStates.size() +
									" #BS " + m_pPOMDP.getBeliefStateFactory().getBeliefUpdatesCount() +
									" #backups " + m_cBackups +
									" #V^(b) = " + m_cNewPointComputations +
									" max depth " + iMaxDepth +
									" free memory " + rtRuntime.freeMemory() / 1000000 +
									" total memory " + rtRuntime.totalMemory() / 1000000 +
									" max memory " + rtRuntime.maxMemory() / 1000000;
			}
			else{
				sMsg = getName() + ": Iteration " + iIteration + 
						" initial width " + round( dInitialWidth, 3 ) +
						" V(b) " + round( m_vValueFunction.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
						" V^(b) " + round( m_vfUpperBound.valueAt( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() ), 4 ) +
						" max depth " + iMaxDepth +
						" max width " + round( m_dMaxWidthForIteration, 3 ) +
						" Time " + ( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" |V| " + m_vValueFunction.size() +
						" |V^| " + m_vfUpperBound.getUpperBoundPointCount() +
						" #ObservedBS = " + vObservedBeliefStates.size() +
						" #BS " + m_pPOMDP.getBeliefStateFactory().getBeliefUpdatesCount() +
						" #backups " + m_cBackups +
						" #V^(b) = " + m_cNewPointComputations +
						" #HV(B) = " + m_cApplyHComputations +
						" free memory " + rtRuntime.freeMemory() / 1000000 +
						" total memory " + rtRuntime.totalMemory() / 1000000 +
						" max memory " + rtRuntime.maxMemory() / 1000000;
			}
			Logger.getInstance().log( getName(), 0, "VI", sMsg );
			if( m_vValueFunction.getChangesCount() == cValueFunctionChanges ){
				cNoChange++;
			}
			else
				cNoChange = 0;
		}
		
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;
		
		sMsg = "Finished " + getName() + " - time : " + m_cElapsedExecutionTime +
				" |V| = " + m_vValueFunction.size() + 
				" backups = " + m_cBackups + 
				" GComputations = " + AlphaVector.getGComputationsCount() +
				" #V^(b) = " + m_cNewPointComputations +
				" Dot products = " + AlphaVector.dotProductCount();
		Logger.getInstance().log( "HSVI", 0, "VI", sMsg );
		
		if( ExecutionProperties.getReportOperationTime() )
			sMsg = "Avg time: backup " + ( m_cTimeInBackup / ( m_cBackups * 1.0 ) ) + 
					" G " + AlphaVector.getAvgGTime() +
					" Tau " + m_pPOMDP.getBeliefStateFactory().getAvgTauTime() + 
					" DP " + AlphaVector.getAvgDotProductTime() +
					" V^(b) " + ( m_cTimeInV / ( m_cNewPointComputations * 1.0 ) / 1000000 ) + 
					" HV(b) " + ( m_cTimeInHV / ( m_cApplyHComputations * 1.0 ) );
		Logger.getInstance().log( "HSVI", 0, "VI", sMsg );
	}

	
	protected void updateBounds( BeliefState bsCurrent ){		
		AlphaVector avNext = backup( bsCurrent );
		AlphaVector avCurrent = m_vValueFunction.getMaxAlpha( bsCurrent );
		double dCurrentValue = valueAt( bsCurrent );
		double dNewValue = avNext.dotProduct( bsCurrent );
		if( dNewValue > dCurrentValue ){
			m_vValueFunction.addPrunePointwiseDominated( avNext );
		}
		applyH( bsCurrent );
	}

	
	protected BeliefState getNextBeliefState( BeliefState bsCurrent, double dEpsilon, double dDiscount ){
		int iAction = getExplorationAction( bsCurrent );
		
		int iObservation = getExplorationObservation( bsCurrent, iAction, dEpsilon, dDiscount );
		
		if( iObservation == -1 ){
			return null;
		}
		
		return bsCurrent.nextBeliefState( iAction, iObservation );		
	}
	
	protected int explore( BeliefState bsCurrent, double dEpsilon, int iTime, double dDiscount, Vector<BeliefState> vObservedBeliefStates ){
		double dWidth = width( bsCurrent );
		int iAction = 0, iObservation = 0;
		BeliefState bsNext = null;
		int iMaxDepth = 0;

		if( m_bTerminate )
			return iTime;
		
		if( !vObservedBeliefStates.contains( bsCurrent ) )
			vObservedBeliefStates.add( bsCurrent );
		
		if( dWidth > m_dMaxWidthForIteration )
			m_dMaxWidthForIteration = dWidth;
		
		if( iTime > 200 || dWidth < ( dEpsilon / dDiscount ) )
			return iTime;
			
		bsNext = getNextBeliefState( bsCurrent, dEpsilon, dDiscount * m_dGamma );

		if( ( bsNext != null ) && ( bsNext != bsCurrent ) ){
			iMaxDepth = explore( bsNext, dEpsilon, iTime + 1, dDiscount * m_dGamma, vObservedBeliefStates );
		}
		else{
			iMaxDepth = iTime;
		}
		
		updateBounds( bsCurrent );	
		
		if( m_dExplorationFactor > 0.0 ){
			int iActionAfterUpdate = getExplorationAction( bsCurrent );
			if( iActionAfterUpdate != iAction ){
				if( m_rndGenerator.nextDouble() < m_dExplorationFactor ){
					iObservation = getExplorationObservation( bsCurrent, iActionAfterUpdate, dEpsilon, dDiscount );
					bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
					if( bsNext != null ){
						iMaxDepth = explore( bsNext, dEpsilon, iTime + 1, dDiscount * m_dGamma, vObservedBeliefStates );
						updateBounds( bsCurrent );	
					}
				}					
			}
		}
					
		return iMaxDepth;
	}

	protected int getExplorationObservation( BeliefState bsCurrent, int iAction, 
			double dEpsilon, double dDiscount ){
		int iObservation = 0, iMaxObservation = -1;
		double dProb = 0.0, dExcess = 0.0, dValue = 0.0, dMaxValue = 0.0;
		BeliefState bsNext = null;
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bsCurrent.probabilityOGivenA( iAction, iObservation );
			if( dProb > 0 ){
				bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
				dExcess = excess( bsNext, dEpsilon, dDiscount );
				dValue = dProb * dExcess;  
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					iMaxObservation = iObservation;
				}
			}
		}
		return iMaxObservation;
	}

	protected int getExplorationAction( BeliefState bsCurrent ){
		return m_vfUpperBound.getAction( bsCurrent );
	}
	
	public class ValueFunctionEntry{
		private double m_dValue;
		private int m_iAction;
		private double[] m_adQValues;
		private int m_cActions;
		
		public ValueFunctionEntry( double dValue, int iAction ){
			m_dValue = dValue;
			m_iAction = iAction;
			m_cActions = m_pPOMDP.getActionCount();
			m_adQValues = new double[m_cActions];
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				m_adQValues[iAction] = Double.POSITIVE_INFINITY;
			}
		}
		public void setValue( double dValue ){
			m_dValue = dValue;
		}
		public double getValue(){
			return m_dValue;
		}
		public void setAction( int iAction ){
			m_iAction = iAction;
		}
		public int getAction(){
			return m_iAction;
		}
		public void setQValue( int iAction, double dValue ){
			m_adQValues[iAction] = dValue;
		}
		public double getQValue( int iAction ){
			return m_adQValues[iAction];
		}
		public double getMaxQValue(){
			double dMaxValue = Double.NEGATIVE_INFINITY;
			int iAction = 0;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( m_adQValues[iAction] > dMaxValue )
					dMaxValue = m_adQValues[iAction];
			}
			return dMaxValue;
		}
		public int getMaxAction(){
			double dMaxValue = Double.NEGATIVE_INFINITY;
			int iAction = 0, iMaxAction = -1;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				if( m_adQValues[iAction] > dMaxValue ){
					dMaxValue = m_adQValues[iAction];
					iMaxAction = iAction;
				}
			}
			return iMaxAction;
		}
	}

}
