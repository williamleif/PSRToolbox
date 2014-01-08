package pomdp.algorithms;
/*
 * Created on May 6, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.environments.FactoredPOMDP;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.RandomGenerator;
import pomdp.utilities.TabularAlphaVector;
import pomdp.utilities.concurrent.ComputeG;
import pomdp.utilities.concurrent.ComputeLowLevelG;
import pomdp.utilities.concurrent.FindMaxAlphas;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.utilities.datastructures.LinkedList;
import pomdp.utilities.factored.FactoredBeliefState;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public abstract class ValueIteration extends PolicyStrategy{
	protected MDPValueFunction m_vfMDP;
	protected LinearValueFunctionApproximation m_vValueFunction;
	protected POMDP m_pPOMDP;
	protected int m_cStates;
	protected int m_cActions;
	protected int m_cObservations;
	protected double m_dGamma;
	protected int m_cBackups;
	protected AlphaVector m_avMaxValues;
	protected final static double MIN_INF = Double.NEGATIVE_INFINITY;
	protected final static double MAX_INF = Double.POSITIVE_INFINITY;
	protected boolean m_bConverged;
	protected double m_dEpsilon = ExecutionProperties.getEpsilon();
	protected long m_cElapsedExecutionTime;
	protected long m_cCPUExecutionTime;
	protected long m_cDotProducts;
	protected int m_cValueFunctionChanges;
	protected boolean m_bTerminate;
	
	protected long m_cTimeInBackup;
	protected long m_cTimeInHV;
	protected long m_cTimeInV;
	protected long m_cAlphaVectorNodes; 
	
	protected static int g_cTrials = 500;
	protected static int g_cStepsPerTrial = 100;
	
	protected static String m_sBlindPolicyValueFunctionFileName = null;
	
	protected RandomGenerator m_rndGenerator;	
	

	public ValueIteration( POMDP pomdp ){
		
		
		
		m_pPOMDP = pomdp;
		m_cStates = m_pPOMDP.getStateCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_cObservations = m_pPOMDP.getObservationCount();
		m_dGamma = m_pPOMDP.getDiscountFactor();
		m_cBackups = 0;
		m_avMaxValues = null;
		m_bConverged = false;
		m_cElapsedExecutionTime = 0;
		m_cCPUExecutionTime = 0;
		m_dFilteredADR = 0.0;
		m_cDotProducts = 0;
		
		m_cTimeInBackup = 0;
		m_cTimeInHV = 0;
		m_cTimeInV = 0;
		m_cAlphaVectorNodes = 0;
		
		m_bTerminate = false;
		
		m_rndGenerator = new RandomGenerator( "ValueIteration" );
		
		m_vValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
		m_vfMDP = pomdp.getMDPValueFunction();

		computeStepsPerTrial();

		//initValueFunctionToMin();
		initValueFunctionUsingBlindPolicy();

	}

	public RandomGenerator getRandomGenerator()
	{
		return m_rndGenerator;
	}
	public void initRandomGenerator( long iSeed ){
		m_rndGenerator.init( iSeed );	
	}
	
	protected void computeStepsPerTrial(){
		int cSteps = 0;
		double dTailSum = 0.0;
		
		for( cSteps = 100 ; cSteps < 10000 ; cSteps++ ){
			dTailSum = Math.pow( m_dGamma, cSteps ) / ( 1 - m_dGamma );
			if( dTailSum <= 0.005 ){
				Logger.getInstance().logln( "#Steps per trials = " + cSteps );
				if( cSteps < g_cStepsPerTrial )
					g_cStepsPerTrial = cSteps;
				break;
			}
		}
		
	}

	public double valueAt( BeliefState bs ){
		return m_vValueFunction.valueAt( bs );
	}
		
	public int getBestAction( BeliefState bs ){
		return m_vValueFunction.getBestAction( bs );
	}
		
	public AlphaVector getMaxAlpha( BeliefState bs ){
		return m_vValueFunction.getMaxAlpha( bs );
	}
	
	protected double diff( double d1, double d2 ){
		if( d1 > d2 )
			return d1 - d2;
		return d2 - d1;
	}
		
	public String toString(){
		String sVector = "[", sValue = "";
		Iterator<AlphaVector> it = m_vValueFunction.iterator();
		
		while( it.hasNext() ){
			sValue = it.next().toString();
			sVector += sValue + ",";
		}
		sVector = sVector.substring( 0, sVector.length() - 1 ) + "]";
		return sVector;
	}
	
	protected Iterator backwardIterator( Vector vElements ){
		Vector vBackward = new Vector();
		int iElement = 0, cElements = vElements.size();
		for( iElement = cElements - 1 ; iElement >= 0 ; iElement-- ){
			vBackward.add( vElements.get( iElement ) );
		}
		return vBackward.iterator();
	}

	protected Iterator randomPermutation( Vector vElements ){
		Vector vOriginal = new Vector( vElements );
		Vector vPermutation = new Vector();
		int idx = 0;
		
		while( vOriginal.size() > 0 ){
			idx = m_rndGenerator.nextInt( vOriginal.size() );
			vPermutation.add( vOriginal.remove( idx ) );
		}
		return vPermutation.iterator();
	}
	
	protected void permutate( Vector vElements, int cSwaps ){
		int iSwap = 0;
		int iFirstElement = 0, iSecondElement = 0, cElements = vElements.size();
		Object oAux = null;
		
		for( iSwap = 0 ; iSwap < cSwaps ; iSwap++ ){
			iFirstElement = m_rndGenerator.nextInt( cElements );
			iSecondElement = m_rndGenerator.nextInt( cElements );
			oAux = vElements.get( iFirstElement );
			vElements.set( iFirstElement, vElements.get( iSecondElement ) );
			vElements.set( iSecondElement, oAux );
		}
	}

	protected AlphaVector G( int iAction, BeliefState bs ){
		return G( iAction, bs, m_vValueFunction );
	}
	
	int cG = 0;
	
	//g_a = r_a + \sum_o argmax_i g^i_a,o \cdot b
	//TODO: Currently supporting only R(s,a)
	protected AlphaVector G( int iAction, BeliefState bs, LinearValueFunctionApproximation vValueFunction ){
		AlphaVector avMax = null, avG = null, avSum = null, avMaxOriginal = null;;
		int iObservation = 0, iState = 0;
		LinkedList<AlphaVector> vVectors = new LinkedList<AlphaVector>( vValueFunction.getVectors() );
		double dMaxValue = MIN_INF, dValue = 0, dProb = 0.0, dSumProbs = 0.0;

		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bs.probabilityOGivenA( iAction, iObservation );
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				dMaxValue = MIN_INF;
				//argmax_i g^i_a,o \cdot b
				for( AlphaVector avAlpha : vVectors ){
					if( avAlpha != null ){
						avG = avAlpha.G( iAction, iObservation );
						
						dValue = avG.dotProduct( bs );
						if( ( avMax == null ) || ( dValue >= dMaxValue ) ){
							dMaxValue = dValue;
							avMax = avG;
							avMaxOriginal = avAlpha;
						}
					}
				}
			}
			else{
				dMaxValue = 0.0;
				while( ( avMaxOriginal = vValueFunction.getLast() ) == null );
				avMax = avMaxOriginal.G( iAction, iObservation );
			}

			if( avSum == null ){
				avSum = avMax.copy(); 
			}
			else if( avMax != null ){
				avSum.accumulate( avMax ); 
			}
			avMax = null;
			
			//Logger.getInstance().logln( iObservation + ") " + avMaxOriginal );
		}
		
		
		AlphaVector avResult = avSum.addReward( iAction ); //* this also discounts
		avResult.setAction( iAction );
		cG++;
		avSum.release();
		return avResult;
	}
	
	protected AlphaVector backup( BeliefState bs, int iAction ){
		AlphaVector avNew = null;
		if( m_pPOMDP.useClassicBackup() ){
			avNew = G( iAction, bs, m_vValueFunction );
		}
		else{
			AlphaVector[] avNext = new AlphaVector[m_cObservations];
			double dValue = findMaxAlphas( iAction, bs, m_vValueFunction, avNext );
			avNew = G( iAction, m_vValueFunction, avNext, ExecutionProperties.useMultiThread() );
		}
		avNew.setWitness( bs );
		return avNew;
	}
	
	public AlphaVector backup( BeliefState bs ){
		return backup( bs, m_vValueFunction );
	}
		
	public AlphaVector backup( BeliefState bs, LinearValueFunctionApproximation vValueFunction ){
		AlphaVector avResult = null;
		long lTimeBefore = 0, lTimeAfter = 0;
		if( ExecutionProperties.getReportOperationTime() )
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();

		if( m_pPOMDP.useClassicBackup() )
			avResult = backupGBased( bs, vValueFunction );
		else //there is a bug in here somewhere - the two backups don't return the same answer. Suspecting that the bug is in the tau backup
			avResult = backupTauBased( bs, vValueFunction, ExecutionProperties.useMultiThread() );
		
		m_cBackups++;
		if( ExecutionProperties.getReportOperationTime() )
		{
			lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_cTimeInBackup += ( lTimeAfter - lTimeBefore ) / 1000000;
			m_cAlphaVectorNodes += avResult.size();
		}
		return avResult;
	}
	
	protected AlphaVector backupTauBased( BeliefState bs, LinearValueFunctionApproximation vValueFunction, boolean bMultiThread ){
		AlphaVector avMax = null;
		double dValue = 0.0, dMaxValue = Double.NEGATIVE_INFINITY;
		int iAction = 0, iMaxAction = -1;
		
		AlphaVector[] aNext = null, aBest = null;
		//Logger.getInstance().logln( m_cBackups + ") Backup for belief point " + bs );
		
		FindMaxAlphas[] aFinders = new FindMaxAlphas[m_cActions];
		if( bMultiThread ){
			//Thread[] aThreads = new Thread[m_cActions];
			
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				aFinders[iAction] = new FindMaxAlphas( m_pPOMDP, iAction, bs, vValueFunction );
				ThreadPool.getInstance().addTask( aFinders[iAction] );
			}
			
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				ThreadPool.getInstance().waitForTask( aFinders[iAction] );
			}
		}
		Vector<AlphaVector[]> vWinners = new Vector<AlphaVector[]>();
		Vector<Integer> vWinnersActions = new Vector<Integer>();
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			aNext = new AlphaVector[m_cObservations];
			if( bMultiThread ){
				dValue = aFinders[iAction].getValue();
			}
			else{
				dValue = findMaxAlphas( iAction, bs, vValueFunction, aNext );
			}
			
			//Logger.getInstance().logln( "Tau: " + m_cBackups + ") backup: Action value, a = " + iAction + " v = " + dValue );
			
			if( dValue > dMaxValue ){
				dMaxValue = dValue;
				vWinners.clear();
				vWinnersActions.clear();
			}
			if( dValue == dMaxValue ){
				if( bMultiThread ){
					aBest = aFinders[iAction].getNextVectors();
				}
				else{
					aBest = aNext;
				}
				iMaxAction = iAction;
				vWinners.add( aBest );
				vWinnersActions.add( iMaxAction );
			}
		}
		
		int idx = m_rndGenerator.nextInt( vWinners.size() );
		aBest = vWinners.elementAt( idx );
		iMaxAction = vWinnersActions.elementAt( idx );
		avMax = G( iMaxAction, vValueFunction, aBest, bMultiThread );
		avMax.setWitness( bs );
		bs.addBackup();
		
		return avMax;
	}

	
	//multi-thread implementation
	private AlphaVector G( int iAction, LinearValueFunctionApproximation vValueFunction, AlphaVector[] aNext, boolean bMultiThread ) {
		AlphaVector avAlpha = null, avG = null, avSum = null, avResult = null;
		int iObservation = 0;
		ComputeLowLevelG[] aComputeGs = new ComputeLowLevelG[m_cObservations];
		
		if( bMultiThread ){
			//Thread[] aThreads = new Thread[m_cObservations];
	
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				avAlpha = aNext[iObservation];
				aComputeGs[iObservation] = new ComputeLowLevelG( avAlpha, iAction, iObservation );
				//aThreads[iObservation] = new Thread( aComputeGs[iObservation] );
				//aThreads[iObservation].start();
				ThreadPool.getInstance().addTask( aComputeGs[iObservation] );
			}
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				ThreadPool.getInstance().waitForTask( aComputeGs[iObservation] );
			}
		}
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			if( bMultiThread ){
				avG = aComputeGs[iObservation].getG();
			}
			else{
				avAlpha = aNext[iObservation];
				avG = avAlpha.G( iAction, iObservation );
				//Logger.getInstance().logln( iObservation + ") " + avAlpha + ", " + avG );
			}
			
			if( avSum == null )
				avSum = avG.copy();
			else
				avSum.accumulate( avG );
		}
		
		avResult = avSum.addReward( iAction );
		avResult.setAction( iAction );

		//Logger.getInstance().logln( avSum + ", " + avResult );

		
		cG++;
		avSum.release();
		return avResult;
	}

	private double findMaxAlphas( int iAction, BeliefState bs, LinearValueFunctionApproximation vValueFunction, AlphaVector[] aNext ) {
		AlphaVector avAlpha = null;
		int iObservation = 0;
		double dSumValues = 0.0, dValue = 0, dProb = 0.0, dSumProbs = 0.0;
		BeliefState bsSuccessor = null;
		
		boolean bCache = m_pPOMDP.getBeliefStateFactory().isCachingBeliefStates();
		//m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bs.probabilityOGivenA( iAction, iObservation );
			
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				bsSuccessor = bs.nextBeliefState( iAction, iObservation );
				avAlpha = vValueFunction.getMaxAlpha( bsSuccessor );
				dValue = avAlpha.dotProduct( bsSuccessor );
				dSumValues += dValue * dProb;
			}
			else{
				avAlpha = vValueFunction.getLast();
			}
			
			aNext[iObservation] = avAlpha;
			
		}
		
		dSumValues /= dSumProbs; //in case due to rounding there is an error and probs do not exactly sum to 1
		dSumValues *= m_pPOMDP.getDiscountFactor();
		dSumValues += m_pPOMDP.immediateReward( bs, iAction ); 
		
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bCache );
		
		return dSumValues;
	}

	protected AlphaVector backupGBased( BeliefState bs, LinearValueFunctionApproximation vValueFunction ){
		AlphaVector avMax = null, avCurrent = null;
		double dValue = 0.0, dMaxValue = Double.MAX_VALUE * -1;
						
		if( ExecutionProperties.useMultiThread() ){
			ComputeG[] aTasks = new ComputeG[m_cActions];
			for( int iAction : m_pPOMDP.getRelevantActions( bs ) ){
				aTasks[iAction] = new ComputeG( bs, vValueFunction, iAction, m_cObservations );
				ThreadPool.getInstance().addTask( aTasks[iAction] );
			}
			for( int iAction : m_pPOMDP.getRelevantActions( bs ) ){
				ThreadPool.getInstance().waitForTask( aTasks[iAction] );
				avCurrent = aTasks[iAction].getG();
				dValue = avCurrent.dotProduct( bs );
	
				//Logger.getInstance().logln( m_cBackups + ") backup: Action value, a = " + iAction + " v = " + dValue + " " + avCurrent );
				
				if( dValue >= dMaxValue ){
					dMaxValue = dValue;
					if( avMax != null )
						avMax.release();
					avMax = avCurrent;
				}
				else{
					avCurrent.release();
				}
			}
		}
		else{
			if( bs.getMaxErrorAction() == -1 ){
				Vector<AlphaVector> vWinners = new Vector<AlphaVector>();
				Vector<Integer> vWinnersActions = new Vector<Integer>();
				int iMaxAction = -1;
				for( int iAction : m_pPOMDP.getRelevantActions( bs ) ){
					avCurrent = G( iAction, bs, vValueFunction );
					dValue = avCurrent.dotProduct( bs );
		
					//Logger.getInstance().logln( "G: " + m_cBackups + ") backup: Action value, a = " + iAction + " v = " + dValue + " " + avCurrent );
					
					if( dValue > dMaxValue ){
						dMaxValue = dValue;
						vWinners.clear();
						vWinnersActions.clear();
					}
					if( dValue == dMaxValue ){
						iMaxAction = iAction;
						vWinners.add( avCurrent );
						vWinnersActions.add( iMaxAction );
					}
				}
				int idx = m_rndGenerator.nextInt( vWinners.size() );
				avMax = vWinners.elementAt( idx );
				iMaxAction = vWinnersActions.elementAt( idx );				
			}
			else{
				avMax = G( bs.getMaxErrorAction(), bs, vValueFunction );
				//bs.setMaxErrorAction( -1 );
			}
		}
		avMax.setWitness( bs );
		bs.addBackup();
		
		return avMax;
	}
	
	protected double getMinReward(){
		double dMinValue = MAX_INF, dMinStateValue = 0.0, dValue = 0.0;
		int iState = 0, iAction = 0, iNextState = 0;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dMinStateValue = MIN_INF;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				for( iNextState = 0 ; iNextState < m_cStates ; iNextState++ ){
					dValue = m_pPOMDP.R( iState, iAction, iNextState );
					if( dValue < dMinStateValue )
						dMinStateValue = dValue;
				}
			}
			if( dMinStateValue < dMinValue ){
				dMinValue = dMinStateValue;
			}
		}
		
		return dMinValue;
	}
	
	protected double getMaxReward(){
		double dMaxValue = MIN_INF, dMaxStateValue = 0.0, dValue = 0.0;
		int iState = 0, iAction = 0, iNextState;
		
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dMaxStateValue = MIN_INF;
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				for( iNextState = 0 ; iNextState < m_cStates ; iNextState++ ){
					dValue = m_pPOMDP.R( iState, iAction, iNextState );
					if( dValue > dMaxStateValue )
						dMaxStateValue = dValue;
				}
			}
			if( dMaxStateValue > dMaxValue ){
				dMaxValue = dMaxStateValue;
			}
		}
		
		return dMaxValue;
	}
	
	protected double getMaxMinR(){
		return m_pPOMDP.getMaxMinR();
	}
	
	protected void initValueFunctionToMin(){
		m_vValueFunction.clear();
		initValueFunctionToMin( m_vValueFunction );	
	}
	
	protected void initValueFunctionToMin( LinearValueFunctionApproximation vValueFunction ){
		Logger.getInstance().logln( "Init value function to min" );
		double dMinValue = getMaxMinR();
		double dDefaultValue = dMinValue / ( 1 - m_dGamma );
		//double dDefaultValue = 0.0;

 		BeliefState bsUniform = m_pPOMDP.getBeliefStateFactory().getUniformBeliefState();
		Logger.getInstance().logln( "Min R value = " + dMinValue + " init value = " + dDefaultValue );
		//BeliefState bsInitial = m_pPOMDP.getBeliefStateFactory().getInitialBeliefState();
	
		AlphaVector avMin = null;
		avMin = m_pPOMDP.newAlphaVector();
		avMin.setAllValues( dDefaultValue );
		avMin.finalizeValues();
		avMin.setWitness( bsUniform );
		vValueFunction.add( avMin );
	}

	protected void initValueFunctionUsingBlindPolicy( int cMaxIterations ){
		initValueFunctionUsingBlindPolicy( cMaxIterations, m_vValueFunction );
	}
	

	protected void initValueFunctionUsingBlindPolicy(){
		m_vValueFunction.clear();
		
		int iAction = 0, iState = 0, iEndState = 0, iIteration = 0, iMaxDiffState = 0;
		double dValue = 0.0, dNewValue = 0.0, dReward = 0.0, dDiff = 0.0, dMaxDiff = 0.0, dTr = 0.0, dSum = 0.0;
		AlphaVector av = null;
		AlphaVector avNext = null;
		double dMaxResidual = MAX_INF;
		Entry entry = null;
		Iterator itNonZero = null;
		LinearValueFunctionApproximation vMin = new LinearValueFunctionApproximation( m_dEpsilon, false );
		initValueFunctionToMin( vMin );
		
		BeliefState bsUniform = m_pPOMDP.getBeliefStateFactory().getUniformBeliefState();
		
		if( m_sBlindPolicyValueFunctionFileName != null ){
			try{
				m_vValueFunction.load( m_sBlindPolicyValueFunctionFileName, m_pPOMDP );
				Logger.getInstance().logln( "Blind policy loaded successfully" );
				return;
				
			}
			catch( Exception e ){
				Logger.getInstance().logln( "Could not load blind policy - " + e );
			}
		}
		
		
		Logger.getInstance().logln( "Begin blind policy computation  " + m_cActions + " actions" );
		
		
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			av = vMin.elementAt( 0 );
			//av = m_pPOMDP.newAlphaVector();
			//Logger.getInstance().logln( "Initial " + av );
			iIteration = 0;
			dMaxResidual = MAX_INF;
			while( dMaxResidual > 0.1 ){
				avNext = av.newAlphaVector();
				dMaxDiff = 0.0;
				for( iState = 0 ; iState < m_cStates ; iState++ ){
					dSum = 0.0;
					itNonZero = m_pPOMDP.getNonZeroTransitions( iState, iAction );
					while( itNonZero.hasNext() ){
						entry = (Entry)itNonZero.next();
						iEndState = ((Number) entry.getKey()).intValue();
						dTr = ((Number)entry.getValue()).doubleValue();
						dValue = av.valueAt( iEndState );
						dSum += dTr * dValue;
					}
					dReward = m_pPOMDP.R( iState, iAction );
					dNewValue = dReward + dSum * m_dGamma;
					avNext.setValue( iState, dNewValue );
					
					dDiff = Math.abs( dNewValue - av.valueAt( iState ) );
					if( dDiff > dMaxDiff ){
						iMaxDiffState = iState;
						dMaxDiff = dDiff;
					}
				}
				
				dMaxResidual = dMaxDiff;
				avNext.finalizeValues();
				av = avNext;
				
				iIteration++;
				
				if( iIteration % 10 == 0 )
					Logger.getInstance().log( "." );
			}
						
			av.setWitness( bsUniform );
			av.setAction( iAction );
			m_vValueFunction.addPrunePointwiseDominated( av );
			Logger.getInstance().logln( "Done action " + iAction +
					" after " + iIteration + " iterations |V| = " + m_vValueFunction.size() );
		}		
		Logger.getInstance().logln( "Done blind policy" );
		
		if( m_sBlindPolicyValueFunctionFileName != null ){
			try{
				m_vValueFunction.save( m_sBlindPolicyValueFunctionFileName );
				Logger.getInstance().logln( "Blind policy saved successfully" );
				return;
				
			}
			catch( Exception e ){
				Logger.getInstance().logln( "Could not save blind policy - " + e );
			}
		}
		
	}
	
	protected void initValueFunctionUsingBlindPolicy( int cMaxIterations, LinearValueFunctionApproximation vValueFunction ){
		int iIteration = 0, iAction = 0, iState = 0, iNextState = 0;
		double dMinValue = getMaxMinR() / ( 1 - m_dGamma );
		double dTr = 0.0, dNextValue = 0.0, dReward = 0.0, dValue = 0.0;
		AlphaVector avNext = null, avCurrent = null;
		LinearValueFunctionApproximation vNextValueFunction = null;
		Iterator itNonZeroStates = null;
		Map.Entry e = null;
		
		Logger.getInstance().logln( "Init value function using blind policy" );
		
		vValueFunction.clear();
		
		BeliefState bsUniform = m_pPOMDP.getBeliefStateFactory().getUniformBeliefState();
		
		for( iIteration = 0 ; iIteration < cMaxIterations ; iIteration++ ){
			vNextValueFunction = new LinearValueFunctionApproximation( m_dEpsilon, true );
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){

				avNext = new TabularAlphaVector( null, 0, m_pPOMDP );
				avNext.setWitness( bsUniform );
				if( iIteration > 0 )
					avCurrent = (AlphaVector) vValueFunction.elementAt( iAction );
				avNext.setAction( iAction );
	 			for( iState = 0 ; iState < m_cStates ; iState++ ){
					dNextValue = 0.0;
					dReward = 0.0;
					itNonZeroStates = m_pPOMDP.getNonZeroTransitions( iState, iAction );
					while( itNonZeroStates.hasNext() ){
						e = (Entry) itNonZeroStates.next();
						iNextState = ((Number) e.getKey()).intValue();
						dTr = ((Number) e.getValue()).doubleValue();
						if( avCurrent != null )
							dValue = avCurrent.valueAt( iNextState );
						else
							dValue = dMinValue;
		 				dReward += m_pPOMDP.R( iState, iAction, iNextState ) * dTr;
		 				dNextValue += dValue * dTr;
					}
					avNext.setValue( iState, dReward + m_dGamma * dNextValue );
	 			}
				vNextValueFunction.add( avNext, false );
			}
			vValueFunction.copy( vNextValueFunction );
		}
	}
	
	protected void initValueFunctionUsingQMDP(){
		initValueFunctionUsingQMDP( m_vValueFunction );
	}
	
	protected void initValueFunctionUsingQMDP( LinearValueFunctionApproximation vValueFunction ){
		Logger.getInstance().logln( "Init value function using Qmdp" );
		double dMinValue = getMinReward();
		double dOffset = dMinValue / ( 1 - m_dGamma );
		MDPValueFunction vfMDP = m_pPOMDP.getMDPValueFunction();
		vfMDP.valueIteration( 1000, 0.000001 );
		vValueFunction.clear();
		vValueFunction.addAll( vfMDP.getValueFunction() );
	}
	
	public Object choose( Vector vObjects ){
		int iObject = m_rndGenerator.nextInt( vObjects.size() );
		Object oRetVal = vObjects.remove( iObject );
		return oRetVal;
	}
	
	protected boolean dominated( AlphaVector avFirst, AlphaVector avSecond, double dEpsilon ){
		int iState = 0;
		if( avFirst == avSecond )
			return true;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			if( avFirst.valueAt( iState ) > avSecond.valueAt( iState ) + dEpsilon ){
				return false;
			}
		}
		return true;
	}
	
	protected boolean dominated( AlphaVector av, Vector vAlphaVectors, double dEpsilon ){
		AlphaVector avCurrent = null;
		Iterator it = vAlphaVectors.iterator();
		
		while( it.hasNext() ){
			avCurrent = (AlphaVector)it.next();
			if( dominated( av, avCurrent, dEpsilon ) ){
				return true;
			}
		}
		
		return false;
	}
	
	
	protected void add( AlphaVector avNew ){
		m_vValueFunction.add( avNew );
	}
		
	protected double getMaxAlphaSum(){
		int iVector = 0, cVectors = m_vValueFunction.size(), iMaxVector = 0;
		double dMaxValue = Double.MAX_VALUE * -1.0, dValue = 0.0;
		for( iVector = 0 ; iVector < cVectors ; iVector++ ){
			dValue = ((AlphaVector) m_vValueFunction.elementAt( iVector )).sumValues();
			if( dValue > dMaxValue ){
				iMaxVector = iVector;
				dMaxValue = dValue;
			}
		}
		return dMaxValue;
	}
		
	public void valueIteration( int cMaxSteps ) {
		valueIteration( cMaxSteps, m_dEpsilon, Double.POSITIVE_INFINITY );
	}
	
	public void valueIteration( int cMaxSteps, double dEpsilon ){
		valueIteration( cMaxSteps, dEpsilon, Double.POSITIVE_INFINITY );
	}

	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue )
	{
		valueIteration( cMaxSteps, dEpsilon, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, 10 );
	}
	
	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue, int maxRunningTime)
	{
		valueIteration( cMaxSteps, dEpsilon, dTargetValue, maxRunningTime, 10);
	}
	
	public abstract void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue, int maxRunningTime, int numEvaluations);


	public int getAction( BeliefState bsCurrent ){		
		return getBestAction( bsCurrent );
	}
	
	protected String toString( Vector vAlphas ){
		if( vAlphas == null )
			return "null";
		
		String sResult = "[";
		Iterator it = vAlphas.iterator();
		AlphaVector avCurrent = null;
		while( it.hasNext() ){
			avCurrent = (AlphaVector) it.next();
			sResult += "AV" + avCurrent.getId() + " max " + round( avCurrent.getMaxValue(), 3 ) + ", ";
		}
		sResult += "]";
		return sResult;
	}

	public double getValue( BeliefState bsCurrent ){
		AlphaVector avMaxAlpha = getMaxAlpha( bsCurrent );
		return avMaxAlpha.dotProduct( bsCurrent );
	}
	
	public boolean hasConverged() {
		return m_bConverged;
	}

	public String getStatus() {
		return "|V|: " + m_vValueFunction.size() + 
				" ElapsedTime: " + m_cElapsedExecutionTime +
				" CPUTime " + m_cCPUExecutionTime +
				" Backups: " + m_cBackups + 
				" GComputations: " + AlphaVector.getGComputationsCount() +
				" ComputedBS: " + m_pPOMDP.getBeliefStateFactory().getBeliefStateCount() +
				" RealBeliefUpdates: " + m_pPOMDP.getBeliefStateFactory().getBeliefUpdatesCount() +
				" BeliefUpdates: " + BeliefState.g_cBeliefStateUpdates +
				" Dot products: " + AlphaVector.dotProductCount();
	}
	
	public void clearBackupStatistics(){
		m_cBackups = 0;
	}
	
	public static double round( double d, int cDigits ){
		int power = (int)Math.pow( 10, cDigits );
		int num = (int)Math.round( d * power );
		return ( 1.0 * num ) / power;
	}

	public LinearValueFunctionApproximation getValueFunction(){
		return m_vValueFunction;
	}
	
	public void setValueFunction( LinearValueFunctionApproximation vValueFunction ){
		m_vValueFunction = vValueFunction;
	}
	
	public String getName(){
		return "Value Iteration";
	}

	public double computeBellmanError( BeliefState bsCurrent ){
		return computeBellmanError( bsCurrent, m_vValueFunction );
	} 
	
	public double recomputeBellmanError( BeliefState bsCurrent ){
		return recomputeBellmanError( bsCurrent, m_vValueFunction );
	} 
	
	public double computeBellmanError( BeliefState bsCurrent, double dMaxError ){
		return computeBellmanError( bsCurrent, m_vValueFunction, dMaxError );
	} 
	
	public double computeBellmanError( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction, double dMaxError ){
		double dError = 0.0;
		int iAction = 0, iMaxAction = 0;
		double dActionValue = 0.0, dMaxActionValue = MIN_INF;
		double dValue = vValueFunction.valueAt( bsCurrent );
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dActionValue = computePotentialActionValue( bsCurrent, vValueFunction, iAction, dMaxError, dValue );			
			if( dActionValue > dMaxActionValue ){
				iMaxAction = iAction;
				dMaxActionValue = dActionValue;
			}
		}	
		
		dError = dMaxActionValue - dValue;

		return dError;
	}
	
	protected double computePotentialActionValue( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction, int iAction, double dMaxError, double dCurrentValue ){
		double dImmediateReward = m_pPOMDP.immediateReward( bsCurrent, iAction ), dMaximalLeftover = 0.0;
		double dActionValue = dImmediateReward, dMaxValue = vValueFunction.getMaxValue();
		double dUpperBoundError = dImmediateReward + m_dGamma * dMaxValue - dCurrentValue;
		double dSumProbs = 0.0;
		int iObservation = 0;
		BeliefState bsNext = null;
		double dNextValue = 0.0, dProb = 1.0;
		Iterator itSuccssessors = bsCurrent.getSortedSuccessors( iAction );
		Pair pEntry = null;
		
		if( dUpperBoundError < dMaxError )
			return dUpperBoundError;
		
		while( itSuccssessors.hasNext() ){
			pEntry = (Pair) itSuccssessors.next();
			dProb = ((Number) pEntry.second()).doubleValue();
			if( dProb > 0.00 ){
				bsNext = (BeliefState) pEntry.first();
				dNextValue = vValueFunction.valueAt( bsNext );
				dSumProbs += dProb;
				dActionValue += m_dGamma * dProb * dNextValue;
				
				dMaximalLeftover = m_dGamma * ( ( 1.0 - dSumProbs ) * dMaxValue );
				if( dActionValue + dMaximalLeftover < dMaxError )
					return dActionValue + dMaximalLeftover;
			}
		}
		return dActionValue;
	}
	
	protected double m_dMinimalProb = 0.0;
	protected long m_cComputations = 0;
	//protected boolean m_bDebug = false;
	
	protected double computePotentialActionValue( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction, int iAction ){
		double dImmediateReward = m_pPOMDP.immediateReward( bsCurrent, iAction );
		double dActionValue = dImmediateReward;
		double dSumProbs = 0.0;
		int iObservation = 0;
		BeliefState bsNext = null;
		double dNextValue = 0.0, dProb = 1.0;
		Iterator itSuccssessors = bsCurrent.getSortedSuccessors( iAction );
		Pair pEntry = null;

		while( itSuccssessors.hasNext() && dProb > m_dMinimalProb ){
			pEntry = (Pair) itSuccssessors.next();
			dProb = ((Number) pEntry.second()).doubleValue();
			if( dProb > m_dMinimalProb ){
				bsNext = (BeliefState) pEntry.first();
				dNextValue = vValueFunction.valueAt( bsNext );
				dSumProbs += dProb;
				dActionValue += m_dGamma * dProb * dNextValue;
				
				m_cComputations++;
			}
		}
		
		bsCurrent.setPotentialActionValue( iAction, dActionValue );
		
		return dActionValue;
	}
	
	public double computeBellmanError( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction ){
		double dError = 0.0;
		int iAction = 0, iMaxAction = 0;
		double dActionValue = 0.0, dMaxActionValue = MIN_INF;
		double dValue = 0.0;
		
		dValue = vValueFunction.valueAt( bsCurrent );
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dActionValue = computePotentialActionValue( bsCurrent, vValueFunction, iAction );
			dError = dActionValue - dValue;
			bsCurrent.setActionError( iAction, dError );
			if( dActionValue > dMaxActionValue ){
				iMaxAction = iAction;
				dMaxActionValue = dActionValue;
			}
		}	
		dError = dMaxActionValue - dValue;
		
		bsCurrent.setMaxErrorAction( iMaxAction );

		return dError;
	}
	
	public double recomputeBellmanError( BeliefState bsCurrent, LinearValueFunctionApproximation vValueFunction ){
		double dError = 0.0;
		int iAction = 0, iMaxAction = 0;
		double dActionValue = 0.0, dMaxActionValue = MIN_INF;
		double dValue = 0.0;
		
		dValue = vValueFunction.valueAt( bsCurrent );
		
		if( bsCurrent.getMaxErrorAction() == -1 ){
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				dActionValue = bsCurrent.getPotentialActionValue( iAction );
				dError = dActionValue - dValue;
				bsCurrent.setActionError( iAction, dError );
				if( dActionValue > dMaxActionValue ){
					iMaxAction = iAction;
					dMaxActionValue = dActionValue;
				}
			}	
		}
		else{
			iAction = -1;
			while( iAction != bsCurrent.getMaxErrorAction() ){
				iAction = bsCurrent.getMaxErrorAction();
				dActionValue = bsCurrent.getPotentialActionValue( iAction );
				dError = dActionValue - dValue;
				bsCurrent.setActionError( iAction, dError );
				if( dActionValue > dMaxActionValue ){
					iMaxAction = iAction;
					dMaxActionValue = dActionValue;
				}
			}
		}
		
		//dValue = vValueFunction.approximateValueAt( bsCurrent );
		dError = dMaxActionValue - dValue;
		
		bsCurrent.setMaxErrorAction( iMaxAction );

		return dError;
	}
	
	
	protected void initCornerPoints( Vector vCornerPoints ){
		BeliefState bs = null;
		
		for( int iState : m_pPOMDP.getValidStates() ){
			bs = m_pPOMDP.getBeliefStateFactory().getDeterministicBeliefState( iState );
			vCornerPoints.add( bs );
		}	
	}
	
	protected double m_dFilteredADR;
	
	protected boolean checkADRConvergence( POMDP pomdp, double dTargetADR, Pair pComputedADRs ){
		double dSimulatedADR = 0.0;
		boolean bConverged = false;
		boolean bIndependentBeliefState = false;
		
		if( false && m_pPOMDP instanceof FactoredPOMDP ){
			if( ((FactoredPOMDP) m_pPOMDP).getBeliefType() == BeliefType.Independent ){
				//FactoredBeliefStateFactory.getInstance( (FactoredPOMDP)m_pPOMDP );
				m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
				bIndependentBeliefState = true;
			}
		}
		

		pComputedADRs.setFirst( new Double( 0.0 ) );
		pComputedADRs.setSecond( new Double( 0.0 ) );
		

		if( pomdp != null && g_cTrials > 0 ){
			m_vValueFunction.initHitCounts();
			dSimulatedADR = pomdp.computeAverageDiscountedReward( g_cTrials, g_cStepsPerTrial, this );
			//if( dSimulatedADR > 0.0 && m_vValueFunction.size() > 100 )
			//	m_vValueFunction.pruneLowHitCountVectors( 0 );
			
			if( m_dFilteredADR == 0.0 ){
				m_dFilteredADR = dSimulatedADR;
			}
			else{
				m_dFilteredADR = ( m_dFilteredADR + dSimulatedADR ) / 2;
				if( m_dFilteredADR >= dTargetADR )
					bConverged = true;
				//if( Math.abs( m_dFilteredADR - dSimulatedADR ) < 0.01 )
				//	bConverged = true;
			}
			
			if( pComputedADRs != null ){
				pComputedADRs.setFirst( new Double( dSimulatedADR ) );
				pComputedADRs.setSecond( new Double( m_dFilteredADR ) );
			}						
		}
		
		if( bIndependentBeliefState ){
			Logger.getInstance().logln( "Factored operations - Tau: - operations " + FactoredBeliefState.getTauComputationCount() + " avg time " + 
					FactoredBeliefState.getAvgTauTime() );
			//IndependenBeliefStateFactory.getInstance( (FactoredPOMDP)m_pPOMDP );
			m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		}
		
		return bConverged || m_bTerminate;
	}
	
	public static void setBlindPolicyValueFunctionFileName( String sFileName ){
		m_sBlindPolicyValueFunctionFileName = sFileName;
	}

	protected boolean terminalBeliefState(BeliefState bsCurrent) {
		Iterator<Entry<Integer,Double>> itNonZero = bsCurrent.getNonZeroEntries().iterator();
		Entry<Integer,Double> e = null;
		double dSumTerminals = 0.0;
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			if( m_pPOMDP.isTerminalState( e.getKey() ) ){
				dSumTerminals += e.getValue();
				m_pPOMDP.isTerminalState( e.getKey() );
			}
		}
		return dSumTerminals > 0.99;
	}
	
	public void terminate(){
		m_bTerminate = true;
	}
	public MDPValueFunction getMDPValueFunction() {
		return m_vfMDP;
	}
	
	
	
	public void addPrunePointwiseDominated(AlphaVector av)
	{
		m_vValueFunction.addPrunePointwiseDominated(av);
	}
	
	public void ValueFunctionCopy(LinearValueFunctionApproximation vf)
	{
		m_vValueFunction.copy(vf);
	}
	
	public double getEpsilon()
	{
		return m_dEpsilon;
	}


	public POMDP getPOMDP() {
		return m_pPOMDP;
	}
	
}
