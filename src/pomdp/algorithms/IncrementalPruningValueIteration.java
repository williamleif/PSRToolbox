package pomdp.algorithms;

import java.util.Iterator;
import java.util.Vector;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.TabularAlphaVector;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class IncrementalPruningValueIteration extends ValueIteration{

	protected Vector<BeliefState> m_vBeliefPoints;
	
	public IncrementalPruningValueIteration( POMDP pomdp ){
		super( pomdp );
		m_vBeliefPoints = null;
	}

	protected double diff( Vector<AlphaVector> v1, Vector<AlphaVector> v2 ){
		Iterator<BeliefState> itPoints = m_vBeliefPoints.iterator();
		BeliefState bsCurrent = null;
		double dV1 = 0.0, dV2 = 0.0, dDelta = 0.0, dMaxDelta = 0.0;
		while( itPoints.hasNext() ){
			bsCurrent = itPoints.next();
			dV1 = valueAt( bsCurrent, v1 );
			dV2 = valueAt( bsCurrent, v2 );
			dDelta = dV2 - dV1;
			if( dDelta < -0.01 )
				dDelta *= -1;
			if( dDelta > dMaxDelta )
				dMaxDelta = dDelta;
		}
		return dMaxDelta;
	}

	protected double diff( AlphaVector av, Vector<AlphaVector> v ){
		Iterator<AlphaVector> itVectors = v.iterator();
		AlphaVector avCurrent = null;
		double dDiff = 0.0, dMinDiff = MAX_INF;
		while( itVectors.hasNext() ){
			avCurrent = itVectors.next();
			dDiff = diff( av, avCurrent );
			if( dDiff < dMinDiff )
				dMinDiff = dDiff;
		}
		Logger.getInstance().log( "IPVI", 10, "diff", "AV" + av.getId() + " - " + "V = " + dMinDiff );
		return dMinDiff;
	}
	
	protected double diff( AlphaVector av1, AlphaVector av2 ){
		int iState = 0;
		double dValue1 = 0.0, dValue2 = 0, dDiff = 0.0, dMaxDiff = 0.0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue1 = av1.valueAt( iState );
			dValue2 = av2.valueAt( iState );
			dDiff = diff( dValue1, dValue2 );
			if( dDiff > dMaxDiff )
				dMaxDiff = dDiff;
		}
		Logger.getInstance().log( "IPVI", 10, "diff", "AV" + av1.getId() + " - " + "AV" + av2.getId() + " = " + dMaxDiff );
		return dMaxDiff;
	}
	
	protected Vector<AlphaVector> combine( Vector<AlphaVector> vaOld, Vector<AlphaVector> vaNew ){
		Vector<AlphaVector> vCombined = new Vector<AlphaVector>();
		BeliefState bsCurrent = null;
		Iterator<BeliefState> itPoints = m_vBeliefPoints.iterator();
		AlphaVector avCurrent = null;
		double dVOld = 0.0, dVNew = 0.0, dVCombined = 0.0;
		
		while( itPoints.hasNext() ){
			bsCurrent = itPoints.next();
			dVNew = valueAt( bsCurrent, vaNew );
			dVOld = valueAt( bsCurrent, vaOld );
			if( dVNew < dVOld ){
				avCurrent = best( bsCurrent, vaOld );
			}
			else{
				avCurrent = best( bsCurrent, vaNew );
			}
			if( !vCombined.contains( avCurrent ) )
				vCombined.add( avCurrent );
		}
		
		return vCombined;
	}

	protected Vector<AlphaVector> executeIteraiton( Vector<AlphaVector> vCurrent, 
													int cMaxIterations, double dEpsilon ){
		int iIteration = 0;
		double dDiff = MAX_INF;
		Vector<AlphaVector> vNext = null, vPrevious = null;
		for( iIteration = 0 ; ( iIteration < cMaxIterations ) && ( dDiff >= dEpsilon ) ; iIteration++ ){
			vNext = dynamicProgrammingUpdate( vCurrent );
			Logger.getInstance().log( "IPVI", 1, "executeIteraiton", iIteration + ") |V| = " + vNext.size() + " diff = " + dDiff );
			vPrevious = vCurrent;
			vCurrent = combine( vCurrent, vNext );
			dDiff = diff( vPrevious, vCurrent );
			Logger.getInstance().log( "IPVI", 1, "executeIteraiton", toString( vCurrent ) );
		}
		
		return vCurrent;
	}
	
	protected Vector<AlphaVector> dynamicProgrammingUpdate( Vector<AlphaVector> vS ){
		Logger.getInstance().log( "IPVI", 1, "DP", " |V| = " + vS.size() );
		
		int iAction = 0, iObservation = 0, iVector = 0;
		Vector<AlphaVector> vSaz = null, vStag = null, vUnion = null;
		Vector<AlphaVector>[] avSaz = new Vector[m_cObservations];
		Vector<AlphaVector>[] avSa = new Vector[m_cActions];
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				vSaz = next( vS, iAction, iObservation );
				avSaz[iObservation] = filter( vSaz );
			}
			avSa[iAction] = incrementalPruning( avSaz );
			for( iVector = 0 ; iVector < avSa[iAction].size() ; iVector++ )
				avSa[iAction].elementAt( iVector ).setAction( iAction );
		}
		vUnion = union( avSa );
		vStag = filter( vUnion );
		return vStag;
	}

	private Vector<AlphaVector> union( Vector<AlphaVector>[] avSa ){
		Vector<AlphaVector> vUnion = new Vector<AlphaVector>();
		int iVector = 0, cVectors = avSa.length;
		Iterator<AlphaVector> it = null;
		AlphaVector avCurrent = null;
		for( iVector = 0 ; iVector < cVectors ; iVector++ ){
			it = avSa[iVector].iterator();
			while( it.hasNext() ){
				avCurrent = it.next();
				if( !vUnion.contains( avCurrent ) )
					vUnion.add( avCurrent );
			}
		}
		return vUnion;
	}

	private Vector<AlphaVector> union( Vector<AlphaVector> vFirst, Vector<AlphaVector> vSecond ){
		Vector<AlphaVector> vUnion = new Vector<AlphaVector>( vFirst );
		Iterator<AlphaVector> it = vSecond.iterator();
		AlphaVector avCurrent = null;
		while( it.hasNext() ){
			avCurrent = it.next();
			if( !vUnion.contains( avCurrent ) )
				vUnion.add( avCurrent );
		}
		return vUnion;
	}

	private Vector<AlphaVector> incrementalPruning( Vector<AlphaVector>[] avSaz ){
		Logger.getInstance().log( "IPVI", 2, "IP", " avSaz = " + toString( avSaz ) );
		Vector<AlphaVector> avWinners = restrictedRegion( avSaz[0], avSaz[1] );
		int iS = 0;
		for( iS = 3 ; iS < avSaz.length ; iS++ )
			avWinners = restrictedRegion( avSaz[iS], avWinners );
		return avWinners;
	}

	private String toString( Vector<AlphaVector>[] av ){
		int iVector = 0, cVectors = av.length;
		String sOutput = "[";
		for( iVector = 0 ; iVector < cVectors ; iVector++ ){
			//sOutput += "[" + toString( av[iVector] ) + "], ";
			sOutput += av[iVector].size() + ", ";
		}
		sOutput += "]";
		return sOutput;
	}

	private Vector<AlphaVector> restrictedRegion( Vector<AlphaVector> vFirst, Vector<AlphaVector> vSecond ){
		Logger.getInstance().log( "IPVI", 3, "", "RR: " + toString( vFirst ) + " --- " + toString( vSecond ) );

		Vector<AlphaVector> vWinners = new Vector<AlphaVector>();
		Vector<AlphaVector> vDominate = null;
		Vector<AlphaVector> vaFilter = crossSum( vFirst, vSecond );
		AlphaVector avCurrent = null, avBest = null;
		BeliefState bsWitness = null;
		Vector<BeliefState> vBeliefPoints = new Vector<BeliefState>( m_vBeliefPoints );
		
		if( vaFilter.size() == 1 ){
			return vaFilter;
		}
			
		while( !vaFilter.isEmpty() ){
			avCurrent = vaFilter.firstElement();
			if( vFirst.size() > vSecond.size() ){
				vDominate = getRelevantVectors( avCurrent.getSumIds(), 1, vaFilter, avCurrent.getId() );
			}
			else{
				vDominate = getRelevantVectors( avCurrent.getSumIds(), 2, vaFilter, avCurrent.getId() );
			}
			bsWitness = dominate( avCurrent, union( vDominate, vWinners ), vBeliefPoints );
			if( bsWitness == null ){
				vaFilter.remove( 0 );
			}
			else{
				vBeliefPoints.remove( bsWitness );
				avBest = best( bsWitness, vaFilter );
				vaFilter.remove( avBest );
				vWinners.add( avBest );
			}
				
		}
		
		Logger.getInstance().log( "IPVI", 3, "RR", "end: " + toString( vWinners )  );
		
		if( vWinners.size() == 0 ){
			Logger.getInstance().logError( "IPVI", "RR", "end: no winners" );
			restrictedRegion( vFirst, vSecond );
		}
		
		return vWinners;
	}

	private Vector<AlphaVector> getRelevantVectors( long[] aiSumIds, int iRelevantIndex,
			Vector<AlphaVector> vVectors, long iExcludeId ){
		Iterator<AlphaVector> itVectors = vVectors.iterator();
		AlphaVector avCurrent = null;
		Vector<AlphaVector> vaResult = new Vector<AlphaVector>();
		
		while( itVectors.hasNext() ){
			avCurrent = itVectors.next();
			if( avCurrent.getId() != iExcludeId ){
				if( iRelevantIndex == 1 ){
					if( avCurrent.getSumIds()[0] == aiSumIds[0] )
						vaResult.add( avCurrent );
				}
				else if( iRelevantIndex == 2 ){
					if( avCurrent.getSumIds()[1] == aiSumIds[1] )
						vaResult.add( avCurrent );
				}
			}
		}
		return vaResult;
	}

	private Vector<Pair> indexCrossSum( int cElements1, int cElements2 ){
		int iElement1 = 0, iElement2 = 0;
		Pair pNew = null;
		Vector<Pair> vSum = new Vector<Pair>();
		
		for( iElement1 = 0 ; iElement1 < cElements1 ; iElement1++ ){
			for( iElement2 = 0 ; iElement2 < cElements2 ; iElement2++ ){
				pNew = new Pair( iElement1, iElement2 );
				vSum.add( pNew );
			}
		}
		return vSum;
	}
	
	private Vector<AlphaVector> crossSum( Vector<AlphaVector> vFirst, Vector<AlphaVector> vSecond ){
		Vector<AlphaVector> vSum = new Vector<AlphaVector>();
		Iterator<AlphaVector> itFirst = vFirst.iterator(), itSecond = vSecond.iterator();
		AlphaVector avFirst = null, avSecond = null, avNew = null;
		
		while( itFirst.hasNext() ){
			avFirst = itFirst.next();
			itSecond = vSecond.iterator();
			while( itSecond.hasNext() ){
				avSecond = itSecond.next();
				avNew = sum( avFirst, avSecond );
				vSum.add( avNew );
			}
		}
		
		return vSum;
	}

	private Vector<AlphaVector> crossSum( AlphaVector avFirst, Vector<AlphaVector> vSecond ){
		Vector<AlphaVector> vSum = new Vector<AlphaVector>();
		Iterator<AlphaVector> itSecond = vSecond.iterator();
		AlphaVector avSecond = null, avNew = null;
		
		while( itSecond.hasNext() ){
			avSecond = itSecond.next();
			avNew = sum( avFirst, avSecond );
			vSum.add( avNew );
		}
		
		return vSum;
	}

	private AlphaVector sum( AlphaVector avFirst, AlphaVector avSecond ){
		TabularAlphaVector avSum = new TabularAlphaVector( null, -1, m_pPOMDP );
		int iState = 0;
		double dValue1 = 0.0, dValue2 = 0.0, dSum = 0.0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dValue1 = avFirst.valueAt( iState );
			dValue2 = avSecond.valueAt( iState );
			dSum = dValue1 + dValue2;
			avSum.setValue( iState, dSum );
		}
		avSum.setSumIds( avFirst.getId(), avSecond.getId() );
		return avSum;
	}

	private Vector<AlphaVector> filter( Vector<AlphaVector> vFilter ){
		Logger.getInstance().log( "IPVI", 3, "", "Filter: " + toString( vFilter ) );
		if( vFilter.size() <= 1 ){
			Logger.getInstance().log( "IPVI", 3, "", "Filter - nothing to do" );
			return new Vector<AlphaVector>( vFilter );
		}
		Vector<AlphaVector> vWinners = new Vector();
		AlphaVector avCurrent = null, avBest = null;
		BeliefState bsWitness = null;
		Vector<BeliefState> vBeliefPoints = new Vector<BeliefState>( m_vBeliefPoints );
		while( !vFilter.isEmpty() ){
			avCurrent = vFilter.firstElement();
			bsWitness = dominate( avCurrent, vWinners, vBeliefPoints );
			if( bsWitness == null ){
				vFilter.removeElement( avCurrent );
			}
			else{
				avBest = best( bsWitness, vFilter );
				vWinners.add( avBest );
				vFilter.removeElement( avBest );
				vBeliefPoints.removeElement( bsWitness );
			}
		}
		
		Logger.getInstance().log( "IPVI", 3, "", "Filter end " + toString( vWinners ) );
		
		return vWinners;
	}

	private AlphaVector best( BeliefState bsWitness, Vector<AlphaVector> vVectors ){
		//Logger.getInstance().log( "IPVI", "", "best: " + bsWitness +", " + toString( vVectors ) );

		double dValue = 0, dMaxValue = MIN_INF;
		AlphaVector avCurrent = null, avBest = null;
		Iterator<AlphaVector> itVectors = vVectors.iterator();
		while( itVectors.hasNext() ){
			avCurrent = itVectors.next();
			dValue = avCurrent.dotProduct( bsWitness );
			if( dValue > dMaxValue ){
				dMaxValue = dValue;
				avBest = avCurrent;
			}
		}
		return avBest;
	}

	private double valueAt( BeliefState bs, Vector<AlphaVector> vVectors ){
		if( vVectors.isEmpty() )
			return MIN_INF;
		AlphaVector avBest = best( bs, vVectors );
		return avBest.dotProduct( bs );
	}
	
	private BeliefState dominate( AlphaVector avCurrent, Vector<AlphaVector> vVectors, Vector<BeliefState> vBeliefPoints ){
		Logger.getInstance().log( "IPVI", 4, "", "dominate: AV" + avCurrent.getId() + ", " + toString( vVectors ) );

		Iterator<BeliefState> itPoints = vBeliefPoints.iterator();
		BeliefState bsCurrent = null;
		double dValue = 0.0, dNewValue = 0.0;
		
		while( itPoints.hasNext() ){
			bsCurrent = itPoints.next();
			dValue = valueAt( bsCurrent, vVectors );
			dNewValue = avCurrent.dotProduct( bsCurrent );
			if( dNewValue >= dValue + 0.01 )
				return bsCurrent;
		}
		return null;
	}

	private Vector<AlphaVector> next( Vector<AlphaVector> vS, int iAction, int iObservation ){
		Vector<AlphaVector> vNext = new Vector<AlphaVector>();
		Iterator<AlphaVector> itVectors = vS.iterator();
		AlphaVector avCurrent = null, avNext = null;
		while( itVectors.hasNext() ){
			avCurrent = itVectors.next();
			avNext = next( avCurrent, iAction, iObservation );
			vNext.add( avNext );
		}		
		return vNext;
	}

	private AlphaVector next( AlphaVector avCurrent, int iAction, int iObservation ){
		int iState = 0, iNextState = 0;
		double dTr = 0.0, dAlphaValue = 0.0, dO = 0.0, dR = 0.0, dSum = 0.0;
		AlphaVector avNext = new TabularAlphaVector( null, iAction, m_pPOMDP );
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dSum = 0.0;
			for( iNextState = 0 ; iNextState < m_cStates ; iNextState++ ){
				dTr = m_pPOMDP.tr( iState, iAction, iNextState );
				dO = m_pPOMDP.O( iAction, iNextState, iObservation );
				dAlphaValue = avCurrent.valueAt( iNextState );
				dSum += dTr * dO * dAlphaValue;
			}
			dSum *= m_dGamma;
			dR = m_pPOMDP.R( iState, iAction );
			dSum += dR / m_cObservations;
			avNext.setValue( iState, dSum );
		}
		return avNext;
	}
	
	protected Vector<BeliefState> expand( Vector<BeliefState> vBeliefPoints ){
		Vector<BeliefState> vExpanded = new Vector<BeliefState>( vBeliefPoints );
		Iterator it = vBeliefPoints.iterator();
		BeliefState bsCurrent = null, bsNext = null;
			
		while( it.hasNext() ){
			bsCurrent = (BeliefState) it.next();
			bsNext = m_pPOMDP.getBeliefStateFactory().computeFarthestSuccessor( vExpanded, bsCurrent );
			if( ( bsNext != null ) && ( !vExpanded.contains( bsNext ) ) ){
				vExpanded.add( bsNext );
			}
		}
		return vExpanded;
	}
	
	
	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
		int iIteration = 0;
		boolean bDone = false;
		Pair pComputedADRs = new Pair();
		double dMaxDelta = 0.0;
		Vector<AlphaVector> vCurrentVF = null, vNextVF = null;
		long lStartTime = System.currentTimeMillis(), lCurrentTime = 0;
		long lCPUTimeBefore = 0, lCPUTimeAfter = 0, lCPUTimeTotal = 0;
		Runtime rtRuntime = Runtime.getRuntime();
		
		long cDotProducts = AlphaVector.dotProductCount(), cVnChanges = 0, cStepsWithoutChanges = 0;
		m_cElapsedExecutionTime = 0;
		
		Logger.getInstance().log( "IPVI", 4, "", "Starting Incremental Pruning" );
		
		m_vBeliefPoints = new Vector<BeliefState>();
		m_vBeliefPoints.add( m_pPOMDP.getBeliefStateFactory().getInitialBeliefState() );
		
		for( iIteration = 0 ; ( iIteration < cMaxSteps ) && !bDone ; iIteration++ ){
			lStartTime = System.currentTimeMillis();
			lCPUTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
			if( iIteration > 0 )
				m_vBeliefPoints = expand( m_vBeliefPoints );
			cVnChanges = m_vValueFunction.getChangesCount();
			vCurrentVF = new Vector<AlphaVector>( m_vValueFunction.getVectors() );
			vNextVF = executeIteraiton( vCurrentVF, 25, 0.01 );
			m_vValueFunction.setVectors( vNextVF );
			lCPUTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			lCurrentTime = System.currentTimeMillis();
			m_cElapsedExecutionTime += ( lCurrentTime - lStartTime );
			m_cCPUExecutionTime += ( lCPUTimeAfter - lCPUTimeBefore ) / 1000000;
			lCPUTimeTotal += lCPUTimeAfter - lCPUTimeBefore;

			if( ( lCPUTimeTotal  / 1000000000 ) >= 0 ){
				cStepsWithoutChanges = 0;
				if( !bDone )
					bDone = checkADRConvergence( m_pPOMDP, dTargetValue, pComputedADRs );
			
				rtRuntime.gc();
				Logger.getInstance().log( "IPVI", 0, "", "Iteration " + iIteration + 
						" |Vn| = " + m_vValueFunction.size() +
						" |B| = " + m_vBeliefPoints.size() +
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
				Logger.getInstance().log( "IPVI", 0, "", "Iteration " + iIteration + 
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
		
		Logger.getInstance().log( "IPVI", 0, "", "Finished " + getName() + " - time : " + m_cElapsedExecutionTime  +
				" |V| = " + m_vValueFunction.size() + 
				" backups = " + m_cBackups + 
				" GComputations = " + AlphaVector.getGComputationsCount() +
				" Dot products = " + AlphaVector.dotProductCount() );
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon,
			double targetValue, int maxRunningTime, int numEvaluations) {
		throw new NotImplementedException();		
	}
}
