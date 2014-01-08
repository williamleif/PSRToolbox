package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.concurrent.Backup;
import pomdp.utilities.concurrent.ComputeFarthestSuccessors;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class PointBasedValueIteration extends ValueIteration {

	protected Iterator m_itCurrentIterationPoints;
	protected boolean m_bSingleValueFunction = true;
	protected boolean m_bRandomizedActions;
	
	public PointBasedValueIteration( POMDP pomdp ){
		super(pomdp);
		
		m_itCurrentIterationPoints = null;
		m_bRandomizedActions = true;
	}

	public PointBasedValueIteration( POMDP pomdp, boolean bRandomizedActionExpansion ){
		super(pomdp);
		
		m_itCurrentIterationPoints = null;
		m_bRandomizedActions = bRandomizedActionExpansion;
	}

	protected Vector<BeliefState> expandPBVI( Vector<BeliefState> vBeliefPoints ){
		Vector<BeliefState> vExpanded = new Vector<BeliefState>( vBeliefPoints );
		Iterator it = vBeliefPoints.iterator();
		BeliefState bsCurrent = null;
		BeliefState bsNext = null;

		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		while( vExpanded.size() < vBeliefPoints.size() + 100 ){
			bsCurrent = vExpanded.elementAt( m_rndGenerator.nextInt( vExpanded.size() ) );				
			bsNext = m_pPOMDP.getBeliefStateFactory().computeRandomFarthestSuccessor( vBeliefPoints, bsCurrent );
			if( ( bsNext != null ) && ( !vExpanded.contains( bsNext ) ) )
				vExpanded.add( 0, bsNext );
		}
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return vExpanded;
	}
	
	protected Vector<BeliefState> expandMultiThread( Vector<BeliefState> vBeliefPoints ){
		Vector<BeliefState> vExpanded = new Vector<BeliefState>( vBeliefPoints );
		Vector<BeliefState> vSuccessors = null;
		ComputeFarthestSuccessors[] abThreads = new ComputeFarthestSuccessors[ExecutionProperties.getThreadCount()]; 
		int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
		
		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			abThreads[iThread] = new ComputeFarthestSuccessors( vBeliefPoints );
			abThreads[iThread].setPOMDP( m_pPOMDP );
		}
		
		iThread = 0;
		for( BeliefState bs : vBeliefPoints ){
			abThreads[iThread].addBelief( bs );
			iThread = ( iThread + 1 ) % cThreads;
		}
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().addTask( abThreads[iThread] );
		}
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().waitForTask( abThreads[iThread] );
		}

		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			vSuccessors = abThreads[iThread].getSuccessors();
			for( BeliefState bs : vSuccessors ){
				if( !vExpanded.contains( bs ) ){
					vExpanded.add( bs );
					//Logger.getInstance().logln( bs );
				}
			}
		}
			
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return vExpanded;
	}
	
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations){
		
		int currentEvaluation = 1;
		// with numEvaluations evalations, we find out how much time alloted per evaluation
		int timePerEval = maxRunningTime / numEvaluations;
		
		
		
		
		Pair<Double, Double> pComputedADRs = new Pair<Double, Double>(new Double(0.0), new Double(0.0));
		boolean bDone = false;
		boolean bDoneInternal = false;
		long lStartTime;
		long lCurrentTime = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		
		int iIteration = 0;
		int cInternalIterations = 1;
		int iInternalIteration = 0;
		double dDelta = 1.0;
		double dMinDelta = 0.01;
		int cBeliefPoints = 0;
		
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		
		long lCPUTimeBefore;
		
		long lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		int cValueFunctionChanges = 0;		
			
		Vector<BeliefState>	vBeliefPoints = new Vector<BeliefState>();
		
		/* initialize the list of belief points with the initial belief state */
		vBeliefPoints.add( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() );
		
		Logger.getInstance().logln( "Begin " + getName() );

		for( iIteration = 0 ; iIteration < cIterations && !bDone ; iIteration++ ){
			
				
			/* Compute quality of solution at this time */
			long elapsedTimeSeconds = m_cElapsedExecutionTime / 1000000000;
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			/* first, expand the belief set */
			if( iIteration > 0 ){
											
				Logger.getInstance().logln( "Expanding belief space" );
				m_dFilteredADR = 0.0;
				cBeliefPoints = vBeliefPoints.size();
				if( ExecutionProperties.useHighLevelMultiThread() )
					vBeliefPoints = expandMultiThread( vBeliefPoints );
				else
					vBeliefPoints = expandPBVI( vBeliefPoints );
				Logger.getInstance().logln( "Expanded belief space - |B| = " + vBeliefPoints.size() );
				if( vBeliefPoints.size() == cBeliefPoints )
					bDone = true;
			}
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cElapsedExecutionTime += (lCPUTimeAfter - lCPUTimeBefore);
			lCPUTimeTotal += (lCPUTimeAfter - lCPUTimeBefore);
					
			dDelta = 1.0;
			bDoneInternal = false;
			for( iInternalIteration = 0 ; 
				( iInternalIteration < cInternalIterations ) && ( dDelta > dMinDelta ) && !bDoneInternal ; iInternalIteration++ ){
				
				
				/* Compute quality of solution at this time */
				elapsedTimeSeconds = m_cElapsedExecutionTime / 1000000000;
				
				lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
				cValueFunctionChanges = m_vValueFunction.getChangesCount();
				if( ExecutionProperties.useHighLevelMultiThread() )
					dDelta = improveValueFunctionMultiThreaded( vBeliefPoints );
				else
					dDelta = improveValueFunction( vBeliefPoints );

				lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();			
				
				m_cElapsedExecutionTime += (lCPUTimeAfter - lCPUTimeBefore);
				lCPUTimeTotal += (lCPUTimeAfter - lCPUTimeBefore);
				
				if( dDelta < dEpsilon && cValueFunctionChanges == m_vValueFunction.getChangesCount() ){
					Logger.getInstance().logln( "Value function did not change - iteration " + iIteration + " complete" );
					bDoneInternal = true;
				}
				else{
					if( iIteration > 0 ){
						bDone = bDone || checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
						if( bDone )
							bDoneInternal = true;
					}
					
					rtRuntime.gc();
					Logger.getInstance().logln( "PBVI: Iteration " + iIteration + "," + iInternalIteration +
							" |Vn| = " + m_vValueFunction.size() +
							" |B| = " + vBeliefPoints.size() +
							" Delta = " + round( dDelta, 4 ) +
							" simulated ADR " + ((Number) pComputedADRs.first()).doubleValue() +
							" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
							//" Time " + ( lCurrentTime - lStartTime ) / 1000 +
							" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
							" CPU total " + lCPUTimeTotal  / 1000000000 +
							" #backups " + m_cBackups + 
							" #dot product " + AlphaVector.dotProductCount() + 
							" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
							" memory: " + 
							" total " + rtRuntime.totalMemory() / 1000000 +
							" free " + rtRuntime.freeMemory() / 1000000 +
							" max " + rtRuntime.maxMemory() / 1000000 +
							"" );
				}
			}
		}

		Logger.getInstance().logln( "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + " |BS| = " + vBeliefPoints.size() +
				" |V| = " + m_vValueFunction.size() + " backups = " + m_cBackups + " GComputations = " + AlphaVector.getGComputationsCount() );
	}

	
	protected double improveValueFunction( Vector vBeliefPoints ){
		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
		BeliefState bsCurrent = null, bsMax = null;
		AlphaVector avBackup = null, avNext = null, avCurrentMax = null;
		double dMaxDelta = 1.0, dDelta = 0.0, dBackupValue = 0.0, dValue = 0.0;
		double dMaxOldValue = 0.0, dMaxNewValue = 0.0;
		int iBeliefState = 0;

		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		if( m_itCurrentIterationPoints == null )
			m_itCurrentIterationPoints = vBeliefPoints.iterator();
		dMaxDelta = 0.0;
		
		while( m_itCurrentIterationPoints.hasNext() ){
			bsCurrent= (BeliefState) m_itCurrentIterationPoints.next();
			avCurrentMax = m_vValueFunction.getMaxAlpha( bsCurrent );
			avBackup = backup( bsCurrent );
			
			dBackupValue = avBackup.dotProduct( bsCurrent );
			dValue = avCurrentMax.dotProduct( bsCurrent );
			dDelta = dBackupValue - dValue;
			
			
			if( dDelta > dMaxDelta ){
				dMaxDelta = dDelta;
				bsMax = bsCurrent;
				dMaxOldValue = dValue;
				dMaxNewValue = dBackupValue;
			}
			
			avNext = avBackup;
			
			if(dDelta >= 0)
				m_vValueFunction.addPrunePointwiseDominated( avBackup );
			iBeliefState++;
		}
		if( m_bSingleValueFunction ){
			Iterator it = vNextValueFunction.iterator();
			while( it.hasNext() ){
				avNext = (AlphaVector) it.next();
				m_vValueFunction.addPrunePointwiseDominated( avNext );
			}
		}
		else{
			m_vValueFunction.copy( vNextValueFunction );
		}
		
		
		if( !m_itCurrentIterationPoints.hasNext() )
			m_itCurrentIterationPoints = null;
		
		Logger.getInstance().logln( "Max delta over " + bsMax + 
				" from " + round( dMaxOldValue, 3 ) + 
				" to " + round( dMaxNewValue, 3 ) );
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return dMaxDelta;
	}
	
	protected double improveValueFunctionMultiThreaded( Vector<BeliefState> vBeliefPoints ){
		LinearValueFunctionApproximation vNextValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
		BeliefState bsCurrent = null, bsMax = null;
		AlphaVector avBackup = null, avNext = null;
		double dMaxDelta = 1.0, dDelta = 0.0, dBackupValue = 0.0, dValue = 0.0;
		double dMaxOldValue = 0.0, dMaxNewValue = 0.0;

		Backup[] abThreads = new Backup[ExecutionProperties.getThreadCount()]; 
		int iThread = 0, cThreads = ExecutionProperties.getThreadCount();
		int iVector = 0, cVectors = 0;
		
		boolean bPrevious = m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		Iterator<BeliefState> itCurrentIterationPoints = vBeliefPoints.iterator();
		dMaxDelta = 0.0;
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			abThreads[iThread] = new Backup( m_pPOMDP, m_vValueFunction );
		}
		
		iThread = 0;
		while( itCurrentIterationPoints.hasNext() ){
			bsCurrent= itCurrentIterationPoints.next();
			abThreads[iThread].addBelief( bsCurrent );
			iThread = ( iThread + 1 ) % cThreads;
		}
		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().addTask( abThreads[iThread] );
		}
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			ThreadPool.getInstance().waitForTask( abThreads[iThread] );
		}

		
		for( iThread = 0 ; iThread < cThreads ; iThread++ ){
			cVectors = abThreads[iThread].getResultsCount();
			for( iVector = 0 ; iVector < cVectors ; iVector++ ){
				bsCurrent = abThreads[iThread].getBeliefState( iVector );
				avBackup = abThreads[iThread].getResult( iVector );
				dBackupValue = avBackup.dotProduct( bsCurrent );
				dValue = m_vValueFunction.valueAt( bsCurrent );
				dDelta = dBackupValue - dValue;
				if( dDelta > dMaxDelta ){
					dMaxDelta = dDelta;
					bsMax = bsCurrent;
					dMaxOldValue = dValue;
					dMaxNewValue = dBackupValue;
				}
				vNextValueFunction.addPrunePointwiseDominated( avBackup );
			}
		}
		
		if( m_bSingleValueFunction ){
			Iterator it = vNextValueFunction.iterator();
			while( it.hasNext() ){
				avNext = (AlphaVector) it.next();
				m_vValueFunction.addPrunePointwiseDominated( avNext );
			}
		}
		else{
			m_vValueFunction.copy( vNextValueFunction );
		}
		
		Logger.getInstance().logln( "Max delta over " + bsMax + 
				" from " + round( dMaxOldValue, 3 ) + 
				" to " + round( dMaxNewValue, 3 ) );
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bPrevious );
		
		return dMaxDelta;
	}
	
	public String getName(){
		return "PBVI";
	}
}
