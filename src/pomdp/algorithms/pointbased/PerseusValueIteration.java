package pomdp.algorithms.pointbased;

import java.util.Iterator;
import java.util.Vector;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.valuefunction.LinearValueFunctionApproximation;


public class PerseusValueIteration extends ValueIteration{

	protected Vector m_vIterationBeliefPoints;
	protected Vector<BeliefState> m_vBeliefPoints;

	
	public PerseusValueIteration( POMDP pomdp ){
		super( pomdp );
		m_vIterationBeliefPoints = null;
		m_vBeliefPoints = null;
	}
	
	public void valueIteration(int cIterations, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations)
	{
		int iIteration = 0;
		boolean bDone = false;
		Pair pComputedADRs = new Pair();
		double dMaxDelta = 0.0;
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		Vector<BeliefState> vBeliefPoints = CreateBeliefSpaces.createRandomSpace( m_pPOMDP, 10000 );
		long cDotProducts = AlphaVector.dotProductCount(), cVnChanges = 0, cStepsWithoutChanges = 0;
		m_cElapsedExecutionTime = 0;
		
		Logger.getInstance().logln( "Starting " + getName() + " with " + vBeliefPoints.size() + " belief points " + " |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() );
		
		//cMaxSteps = vBeliefPoints.size() * 2;
		
		init( vBeliefPoints );

		
		for( iIteration = 0 ; ( iIteration < cIterations ) && !bDone ; iIteration++ ){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			cVnChanges = m_vValueFunction.getChangesCount();
			dMaxDelta = singleValueFunctionIteration( iIteration, dEpsilon, m_pPOMDP );
			bDone = isDone( dMaxDelta, dEpsilon );
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			lCurrentTime = System.currentTimeMillis();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;

			if( ( cVnChanges < m_vValueFunction.getChangesCount() ) && ( ( lCPUTimeTotal  / 1000000000 ) >= 0 ) &&
					( m_vValueFunction.size() > 5 ) ){
				cStepsWithoutChanges = 0;
				if( !bDone ) {
					bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
				
				}	
				rtRuntime.gc();
				Logger.getInstance().logln( "Iteration " + iIteration + 
						" |Vn| = " + m_vValueFunction.size() +
						" simulated ADR " + round( ((Number) pComputedADRs.first()).doubleValue(), 3 ) +
						" filtered ADR " + round( ((Number) pComputedADRs.second()).doubleValue(), 3 ) +
						" time " + 	( lCurrentTime - lStartTime ) / 1000 +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" #backups " + m_cBackups + 
						" V changes " + m_vValueFunction.getChangesCount() +
						" max delta " + round( dMaxDelta, 3 ) +
						" #dot product " + AlphaVector.dotProductCount() + 
						" |BS| " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
						" memory: " + 
						" total " + rtRuntime.totalMemory() / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 +
						"" );
			}
			else{
				cStepsWithoutChanges++;
				Logger.getInstance().logln( "Iteration " + iIteration + 
						" |Vn| = " + m_vValueFunction.size() +
						" time " + 	( lCurrentTime - lStartTime ) / 1000 +
						" V changes " + m_vValueFunction.getChangesCount() +
						" max delta " + round( dMaxDelta, 3 ) +
						" CPU time " + ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000000 +
						" CPU total " + lCPUTimeTotal  / 1000000000 +
						" #backups " + m_cBackups + 
						"" );
			
			}
			if( cStepsWithoutChanges == 50 )
				bDone = true;

		}	
		m_bConverged = true;

		m_cDotProducts = AlphaVector.dotProductCount() - cDotProducts;
		m_cElapsedExecutionTime /= 1000;
		m_cCPUExecutionTime /= 1000;
		
		Logger.getInstance().logln( "Finished " + getName() + " - time : " + m_cElapsedExecutionTime + " |BS| = " + vBeliefPoints.size() +
				" |V| = " + m_vValueFunction.size() + 
				" backups = " + m_cBackups + 
				" GComputations = " + AlphaVector.getGComputationsCount() +
				" Dot products = " + m_cDotProducts );
	}
	
	protected boolean isDone( double dMaxDelta, double dEpsilon ){
		return false;
	}

	protected double executeIteration( int iIteration, double dEpsilon ){
		BeliefState bsCurrent = null;
		LinearValueFunctionApproximation vNextAlphaVectors = null;
		AlphaVector avNext = null, avMaxAlpha = null;
		double dValue = 0.0, dNextValue = 0.0, dMaxDelta = Double.MAX_VALUE;
		boolean bDone = false;
		int iElement = 0;
		
		dMaxDelta = 0.0;
		
		initIterationPoints();
		
		vNextAlphaVectors = new LinearValueFunctionApproximation( m_dEpsilon, true );
		
		while( !iterationComplete() ){ 
			bsCurrent = chooseNext();
			dValue = m_vValueFunction.valueAt( bsCurrent );
			avMaxAlpha = vNextAlphaVectors.getMaxAlpha( bsCurrent );
			if( avMaxAlpha != null )
				dNextValue = avMaxAlpha.dotProduct( bsCurrent );
			else
				dNextValue = MIN_INF;
			//Logger.getInstance().logln( "BeliefPoint = " + bsCurrent + " old value = " + dValue +
			//		" new value = " + dNextValue );
			if( dValue > dNextValue ){
				avNext = backup( bsCurrent );
				avNext.setWitness( bsCurrent );
				dNextValue = avNext.dotProduct( bsCurrent );
				//Logger.getInstance().logln( "backup value = " + dNextValue );
				if( dNextValue < dValue + m_dEpsilon ){
					avNext = getMaxAlpha( bsCurrent );
					//Logger.getInstance().logln( "Choose previous best" );
				}
				else{
					if( dNextValue - dValue > dMaxDelta ){
						dMaxDelta = dNextValue - dValue;
					}
					//Logger.getInstance().logln( "Improving point found: " + bsCurrent + " delta = " + ( dNextValue - dValue ) );
				}
				avMaxAlpha = avNext;
				vNextAlphaVectors.add( avNext );
			}
			
			//updatePoint( bsCurrent, dNextValue - dValue, avMaxAlpha );
		}
		
		//finalizeIteration( m_vValueFunction, vNextAlphaVectors );
						
		if( m_vValueFunction.equals( vNextAlphaVectors ) ||
				( vNextAlphaVectors.size() > 1000 ) 
				|| ( ( m_vValueFunction.size() > 10 ) && ( dMaxDelta < dEpsilon ) ) )
			bDone = true;
		m_vValueFunction = vNextAlphaVectors;
		return 0.0;
	}
	
	protected long m_cExecutionTime = 0;
	protected int m_cIterations = 0;
	
	protected double singleValueFunctionIteration( int iIteration, double dEpsilon, POMDP pomdp ){
		BeliefState bsCurrent = null;
		AlphaVector avNext = null;
		double dValue = 0.0, dNextValue = 0.0;
		double dMaxDelta = 0.0, dDelta = 0.0;
		long lStart = JProf.getCurrentThreadCpuTimeSafe(), lEnd = 0;
		
		long iStart = 0, iEnd = 0, cPruneTime = 0, cBackupTime = 0;
		int cChecked = 0, cSuccessful = 0;
		Pair pComputedADRs = new Pair();
		
		iStart = System.currentTimeMillis();
		initIterationPoints();
		iEnd = System.currentTimeMillis();
		//Logger.getInstance().logln( "Time in init - " + ( iEnd - iStart ) / 1000.0 );
		
		while( !iterationComplete() ){ 
			bsCurrent = chooseNext();
			cChecked++;
			if( bsCurrent != null ){
				dValue = bsCurrent.getComputedValue();
				iStart = System.currentTimeMillis();
				avNext = backup( bsCurrent );
				iEnd = System.currentTimeMillis();
				cBackupTime += ( iEnd - iStart );
				avNext.setWitness( bsCurrent );
				dNextValue = avNext.dotProduct( bsCurrent );
				m_vIterationBeliefPoints.remove( bsCurrent );
				if( dNextValue >= dValue ){
					if( ExecutionProperties.getDebug() && ( dNextValue > dValue ) )
						Logger.getInstance().log( "PerseusValueIteration", 0, "singleValueFunctionIteration", "err " + round( dNextValue - dValue, 3 ) + ", bs" + bsCurrent.getId() + ", max " + avNext.getMaxValue() );
					
					iStart = System.currentTimeMillis();
					prunePoints( avNext );
					iEnd = System.currentTimeMillis();
					cPruneTime += ( iEnd - iStart );
					if( dNextValue > dValue ){
						cSuccessful++;
						//add( avNext );
						m_vValueFunction.addPrunePointwiseDominated( avNext );
						dDelta = dNextValue - dValue;
						if( dDelta > dMaxDelta )
							dMaxDelta = dDelta;
					}
				}
			}
		}
		
		return dMaxDelta;
	}
	
	
	protected void prunePoints( AlphaVector avNext ){
		BeliefState bsCurrent = null;
		Iterator itPoints = m_vIterationBeliefPoints.iterator();
		double dNewValue = 0.0, dComputedValue = 0.0;
		int cPruned = 0;
		
		while( itPoints.hasNext() ){
			bsCurrent = (BeliefState)itPoints.next();
			dNewValue = avNext.dotProduct( bsCurrent );
			dComputedValue = bsCurrent.getComputedValue();
			if( dNewValue > dComputedValue ){
				itPoints.remove();
				cPruned++;
			}
		}
	}

	protected BeliefState chooseNext() {
		return (BeliefState) choose( m_vIterationBeliefPoints );
	}
	
	protected boolean iterationComplete() {
		return m_vIterationBeliefPoints.isEmpty();
	}
	
	protected void initIterationPoints() {
		m_vIterationBeliefPoints = new Vector( m_vBeliefPoints );
		BeliefState bsCurrent = null;
		Iterator itPoints = m_vBeliefPoints.iterator();
		double dValue = 0.0;
		
		//Logger.getInstance().logln( "Begin setting computed values" );
		
		while( itPoints.hasNext() ){
			bsCurrent = (BeliefState)itPoints.next();
			dValue = valueAt( bsCurrent );
			bsCurrent.setComputedValue( dValue );
		}

		//Logger.getInstance().logln( "Done setting computed values" );

	}
	
	protected void init( Vector<BeliefState> vBeliefPoints ){
		m_vBeliefPoints = vBeliefPoints;
	}
	
	public String getName(){
		return "Perseus";
	}
}
