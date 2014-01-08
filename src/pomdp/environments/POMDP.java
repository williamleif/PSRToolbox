/*
 * Created on May 3, 2005
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
package pomdp.environments;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;
import pomdp.algorithms.PolicyStrategy;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.EndOfFileException;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.InvalidModelFileFormatException;
import pomdp.utilities.JProf;
import pomdp.utilities.LineReader;
import pomdp.utilities.Logger;
import pomdp.utilities.POMDPLoader;
import pomdp.utilities.Pair;
import pomdp.utilities.RandomGenerator;
import pomdp.utilities.SparseTabularFunction;
import pomdp.utilities.TabularAlphaVector;
import pomdp.utilities.concurrent.ComputeDiscountedReward;
import pomdp.utilities.concurrent.ThreadPool;
import pomdp.utilities.datastructures.Function;
import pomdp.utilities.datastructures.MapFunction;
import pomdp.utilities.datastructures.TabularFunction;
import pomdp.utilities.factored.LogisticsBeliefStateFactory;
import pomdp.utilities.visualisation.RockSampleVisualisationUnit;
import pomdp.utilities.visualisation.VisualisationUnit;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public class POMDP implements Serializable{
	private static final long serialVersionUID = -231630700034970161L;
	protected Function m_fTransition;
	protected Function m_fReward;
	protected Function m_fObservation;
	protected Function m_fStartState;
	protected Vector<String> m_vStateNames;
	protected Map<String, Integer> m_mStates;
	protected Map<String, Integer> m_mActionIndexes;
	protected Vector<String> m_vActionNames;
	protected Map<String, Integer> m_mObservations;
	protected int m_cStates;
	protected int m_cActions;
	protected int m_cObservations;
	protected double m_dGamma; //discount factor
	protected static String g_sNewline = System.getProperty( "line.separator" ); 
	protected static int g_sMaxTabularSize = 3000;
	protected boolean m_bExploration;
	protected Vector<Integer> m_vTerminalStates;
	protected Vector<Integer> m_vObservationStates;
	protected double[][] m_adStoredRewards;
	protected double[] m_adMinActionRewards;
	protected final static double MAX_INF = Double.POSITIVE_INFINITY;
	protected final static double MIN_INF = Double.NEGATIVE_INFINITY;
	protected RandomGenerator m_rndGenerator;
	protected long m_iRandomSeed;
	protected boolean m_bGBasedBackup;
	protected String m_sName;
	protected RewardType m_rtReward;
	protected boolean m_bCountStatistics;
	protected Map<Integer,Double>[][] m_amBackwardTransitions;
	protected BeliefStateFactory m_bsFactory;
	protected MDPValueFunction m_vfMDP;
	protected double m_dMinReward;
	protected VisualisationUnit m_vVisualisationUnit;
	
	public enum RewardType{
		StateActionState, ActionEndState, StateAction, State;
	}
		
	public POMDP(){
		m_fTransition = null;
		m_fReward = null;
		m_fObservation = null;
		m_mActionIndexes = null;
		m_vActionNames = null;
		m_mStates = null;
		m_mObservations = null;
		m_fStartState = null;
		m_cStates = 0;
		m_cActions = 0;
		m_cObservations = 0;
		m_dGamma = 0.95; 
		m_bExploration = true;
		m_vTerminalStates = null;
		m_vObservationStates = new Vector<Integer>();
		m_adMinActionRewards = null;
		m_iRandomSeed = 0;
		m_rndGenerator = new RandomGenerator( "POMDP" );
		m_sName = "";
		m_rtReward = RewardType.StateAction;
		m_bCountStatistics = true;
		
		//m_bGBasedBackup = true; //changed by Robert Kaplow
		m_bGBasedBackup = false;//tau based doesn't work right now - need to fix a bug there
		
		if( ExecutionProperties.useMultiThread() || ExecutionProperties.useHighLevelMultiThread() ){
			ThreadPool.createInstance( this );
		}
		
		m_amBackwardTransitions = null;
		m_bsFactory = null;
		m_vfMDP = null;
		m_dMinReward = 0.0;//Double.POSITIVE_INFINITY;
		
		m_vVisualisationUnit = null;
	}
	
	public void load( String sFileName ) throws IOException, InvalidModelFileFormatException{
		m_sName = sFileName.substring( sFileName.lastIndexOf( "/" ) + 1, sFileName.lastIndexOf( "." ) );
		POMDPLoader p = new POMDPLoader( this );
		p.load( sFileName );
		if( m_rtReward == RewardType.StateActionState )
			initStoredRewards();
		
		initBeliefStateFactory();
		m_vfMDP = new MDPValueFunction( this, 0.0 );
		
		Logger.getInstance().logln();
	}
	
	public BeliefStateFactory getBeliefStateFactory(){
		return m_bsFactory;
	}
	
	public MDPValueFunction getMDPValueFunction(){
		if( m_vfMDP == null ){
			m_vfMDP = new MDPValueFunction( this, 0.0 );
		}
		return m_vfMDP;
	}
	
	public void resetMDPValueFunction()
	{
		m_vfMDP = new MDPValueFunction( this, 0.0 );
	}
	
	public boolean useClassicBackup(){
		return m_bGBasedBackup;
	}
	
	protected void initStoredRewards(){
		m_adStoredRewards = new double[m_cStates][m_cActions];
		int iState = 0, iAction = 0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
				m_adStoredRewards[iState][iAction] = MIN_INF;
			}
		}
	}
	
	public double tr( int iState1, int iAction, int iState2 ){
		return m_fTransition.valueAt( iState1, iAction, iState2 );
	}
	
	/**
	 * Immediate reward function of the POMDP. Checks reward type (R(s,a,s'),R(s,a) or R(s)) before accessing the reward function. 
	 * @param iStartState
	 * @param iAction
	 * @param iEndState
	 * @return immediate reward
	 */
	public double R( int iStartState, int iAction, int iEndState ){
		double dReward = 0.0;
		if( m_rtReward == RewardType.StateActionState )
			dReward = m_fReward.valueAt( iStartState, iAction, iEndState );
		else if( m_rtReward == RewardType.StateAction )
			dReward = m_fReward.valueAt( iStartState, iAction );
		else if( m_rtReward == RewardType.ActionEndState )
			dReward = m_fReward.valueAt( iAction, iEndState );
		else if( m_rtReward == RewardType.State )
			dReward = m_fReward.valueAt( iStartState );
		return dReward;
	}
	
	/**
	 * Immediate reward function of the POMDP.
	 * @param iStartState
	 * @return immediate reward
	 */
	public double R( int iStartState ){
		double dReward = 0.0;
		if( m_rtReward == RewardType.State )
			dReward = m_fReward.valueAt( iStartState );
		return dReward;
	}
	
	/**
	 * Immediate reward function of the POMDP of the form R(s,a). If reward structure is of the form R(s,a,s') it sums over all possible s'. 
	 * @param iStartState
	 * @param iAction
	 * @return immediate reward
	 */
	public double R( int iStartState, int iAction ){
		int iEndState = 0;
		double dReward = 0.0, dSumReward = 0.0;
		double dTr = 0.0;
		Iterator<Entry<Integer,Double>> itNonZeroEntries = null;
		Entry<Integer,Double> e = null;
		
		if( m_rtReward == RewardType.StateAction ) 
			dReward = m_fReward.valueAt( iStartState, iAction );
		else if( m_rtReward == RewardType.State )
				dReward = m_fReward.valueAt( iStartState );
		else if( m_rtReward == RewardType.StateActionState ){
		
			dReward = m_adStoredRewards[iStartState][iAction];
			if( dReward == MIN_INF ){
			
				dSumReward = m_fReward.valueAt( iStartState, iAction );
				if( dSumReward == 0 ){	
					itNonZeroEntries = m_fReward.getNonZeroEntries( iStartState, iAction );
					if( itNonZeroEntries != null ){
						while( itNonZeroEntries.hasNext() ){
							e = itNonZeroEntries.next();
							iEndState = ((Number) e.getKey()).intValue();
							dReward = ((Number) e.getValue()).doubleValue();
							dTr = tr( iStartState, iAction, iEndState );
							if( dTr > 0 )
								dSumReward += dReward * dTr;
						}
					}
				}
				
				m_adStoredRewards[iStartState][iAction] = dSumReward;
				
				dReward = dSumReward;
			}
			
		}
		
		return dReward;
	}
	
	public double O( int iAction, int iEndState, int iObservation ){
		return m_fObservation.valueAt( iAction, iEndState, iObservation );
	}
	
	public double O(int iAction, BeliefState bsBelief, int iObservation) {
		
		double totalProbability = 0;
		
		int state;
		for (state = 0; state < m_cStates; state++)
		{
			totalProbability += bsBelief.valueAt(state) * O(iAction, state, iObservation);			
		}
		return totalProbability;
		
	}
	
	public double O( int iStartState, int iAction, int iEndState, int iObservation ){
		return O( iAction, iEndState, iObservation );
	}
	
	public String getActionName( int iAction ){
		if( m_vActionNames != null ){
			return ( m_vActionNames.get( new Integer( iAction ) ) ).toString();
		}
		return "" + iAction;
	}
	public int getActionIndex( String sAction ){
		if( m_mActionIndexes != null ){
			Object oIdx = m_mActionIndexes.get( sAction );
			if( oIdx != null ){
				return ((Integer)oIdx).intValue();
			}
		}
		try{
			return Integer.parseInt( sAction );
		}
		catch( NumberFormatException e ){
			return -1;
		}
	}
	public String getObservationName( int iObservation ) {
		return iObservation + "";
	}
	
	public String getStateName( int iState ){
		if( m_vStateNames != null )
			return (String) m_vStateNames.elementAt( iState );
		return iState + "";
	}
	
	public int getStateIndex( String sState ){
		Object oIdx = null;
		if( m_mStates != null )
			oIdx = m_mStates.get( sState );
		if( oIdx != null ){
			return ((Integer)oIdx).intValue();
		}
		else{
			try{
				return Integer.parseInt( sState );
			}
			catch( NumberFormatException e ){
				return -1;
			}
		}
	}
	public int getObservationIndex( String sObservation ){
		Object oIdx = null;
		if( m_mObservations != null )
			oIdx = m_mObservations.get( sObservation );
		if( oIdx != null ){
			return ((Integer)oIdx).intValue();
		}
		else{
			try{
				return Integer.parseInt( sObservation );
			}
			catch( NumberFormatException e ){
				return -1;
			}
		}
	}
	
	public void setTransition( int iStartState, int iAction, int iEndState, double dTr ){
		m_fTransition.setValue( iStartState, iAction, iEndState, dTr );
		//addBackwardTransition( iStartState, iAction, iEndState, dTr );
	}
	public void setObservation( int iAction, int iEndState, int iObservation, double dValue ){
		m_fObservation.setAllValues( iAction, iEndState, iObservation, dValue );
	}
	public void setDiscountFactor( double dGamma ){
		m_dGamma = dGamma;
	}
	public void setStateCount( int cStates ){
		m_cStates = cStates;
	}
	public void addState( String sState ){
		if( m_mStates == null ){
			m_mStates = new TreeMap<String, Integer>();
			m_vStateNames = new Vector<String>();
		}
		
		m_mStates.put( sState, m_cStates );
		m_vStateNames.add( sState );
		m_cStates++;
	}

	public void setActionCount( int cActions ){
		m_cActions = cActions;
	}
	public void addAction( String sAction ){
		if( m_mActionIndexes == null ){
			m_mActionIndexes = new TreeMap<String, Integer>();
			m_vActionNames = new Vector<String>();
		}
		
		m_mActionIndexes.put( sAction, m_cActions );
		m_vActionNames.add( sAction );
		m_cActions++;
	}

	public void setObservationCount( int cObservations ){
		m_cObservations = cObservations;
	}
	public void addObservation( String sObservation ){
		if( m_mObservations == null ){
			m_mObservations = new TreeMap<String, Integer>();
		}
		
		m_mObservations.put( sObservation, m_cObservations );
		m_cObservations++;
	}

	public int execute( int iAction, int iState ){
		int iNextState = -1;
		double dProb = m_rndGenerator.nextDouble();
		double dTr = 0.0;
		Iterator<Entry<Integer,Double>> itNonZero = getNonZeroTransitions( iState, iAction );
		Entry<Integer,Double> e = null;
		while( dProb > 0 ){
			e = itNonZero.next();
			iNextState = e.getKey();
			dTr = e.getValue();
			dProb -= dTr;
		}
		//assert iNextState >= 0 && iNextState < m_cStates;
		return iNextState;
	}
	
	public int observe( int iAction, int iState ){
		int iObservation = -1;
		double dProb = m_rndGenerator.nextDouble(), dO = 0.0;
		Iterator<Entry<Integer,Double>> itNonZeroObservations = m_fObservation.getNonZeroEntries( iAction, iState );
		Entry<Integer,Double> e = null;
		while( dProb > 0 ){
			e = itNonZeroObservations.next();
			iObservation = e.getKey();
			dO = e.getValue();
			dProb -= dO;
		}
		//assert iObservation >= 0 && iObservation < m_cObservations;	
		if( iObservation == m_cObservations )
			throw new Error( "Corrupted observation function - O( " + getActionName( iAction ) + ", " + getStateName( iState ) + ", * ) = 0" );
		return iObservation;
	}
	public int observe( BeliefState bs, int iAction ){
		int iObservation = -1;
		double dProb = m_rndGenerator.nextDouble(), dO = 0.0;
		
		for( iObservation = 0 ; ( iObservation < m_cObservations ) && ( dProb > 0.0 ) ; iObservation++ ){
			dO = bs.probabilityOGivenA( iAction, iObservation );
			dProb -= dO;
		}
		iObservation--;
		return iObservation;
	}
	
	
	public double computeAverageDiscountedReward( int cTests, int cMaxStepsToGoal, PolicyStrategy policy ){
		return computeAverageDiscountedReward( cTests, cMaxStepsToGoal, policy, true, ExecutionProperties.useHighLevelMultiThread() || ExecutionProperties.useMultiThread() );
		//return computeAverageDiscountedRewardImportanceSampling( cTests, cMaxStepsToGoal, policy, true, ExecutionProperties.useHighLevelMultiThread() || ExecutionProperties.useMultiThread() );
		//return computeAverageDiscountedRewardParticleFiltering( cTests, cMaxStepsToGoal, policy, true, ExecutionProperties.useHighLevelMultiThread() || ExecutionProperties.useMultiThread() );
		//return computeAverageDiscountedRewardParticleFilteringImportanceSampling( cTests, cMaxStepsToGoal, policy, true, ExecutionProperties.useHighLevelMultiThread() || ExecutionProperties.useMultiThread() );
	}
	
	public double computeAverageDiscountedReward( int cTests, int cMaxStepsToGoal, PolicyStrategy policy, boolean bOutputMessages, boolean bUseMultiThread ){
		countStatistics( false );
		
		m_cSteps = 0;
		
		double dSumDiscountedRewards = 0.0, dDiscountedReward = 0.0, dSumSquares = 0.0;
		int iTest = 0, iThread = 0, iAction = 0;
		long cCurrentMilliseconds = 0, iStartTime = System.currentTimeMillis(), iEndTime = 0;
		long lTotalCPU = 0, lStartCPU = JProf.getCurrentThreadCpuTimeSafe(), lEndCPU = 0;
		RandomGenerator rndGenerator = m_rndGenerator;
		//m_rndGenerator = new RandomGenerator( "ADR", m_iRandomSeed );
		double dCurrentSum = 0.0;
		int[] aiActionCount = new int[m_cActions];	
		double dStdev = 10000.0, dStandardError = 10.0, dADR = 0.0;
		
		boolean bCacheBeliefStates = getBeliefStateFactory().cacheBeliefStates( false );
		
		int cThreads = Runtime.getRuntime().availableProcessors();
		ComputeDiscountedReward[] aTrials = new ComputeDiscountedReward[cThreads];
		if( bUseMultiThread ){
			LinearValueFunctionApproximation vValueFunction = policy.getValueFunction();
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				aTrials[iThread] = new ComputeDiscountedReward( this, cMaxStepsToGoal, vValueFunction, cTests / cThreads );
				ThreadPool.getInstance().addTask( aTrials[iThread] );
			}
		}
		if( bUseMultiThread ){
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				ThreadPool.getInstance().waitForTask( aTrials[iThread] );
				dDiscountedReward = aTrials[iThread].getDiscountedReward();
				dCurrentSum += dDiscountedReward;
				dSumDiscountedRewards += dDiscountedReward;
			}
		}
		else{
			m_cSteps = 0;
			for( iTest = 0 ; ( iTest < cTests ) && ( dStandardError > 0.01 * dADR  )  ; iTest++ ){
				dDiscountedReward = computeDiscountedReward( cMaxStepsToGoal, policy, aiActionCount );
				dCurrentSum += dDiscountedReward;
				dSumSquares += ( dDiscountedReward * dDiscountedReward );
				dSumDiscountedRewards += dDiscountedReward;
					
				if( bOutputMessages && ( iTest % 100 == 0 ) ){
					Logger.getInstance().log( "" + ( iTest / 100 ) % 10 );
				}
				
				if( iTest >= 50 && iTest % 10 == 0 ){
					dADR = dSumDiscountedRewards / iTest;
					dStdev = Math.sqrt( ( dSumSquares - ( iTest + 1 ) * dADR * dADR ) / iTest );
					if( !Double.isNaN( dStdev ) )
						dStandardError = 2.0 * dStdev / Math.sqrt( iTest + 1 );
				}
			}
			Logger.getInstance().logln( "Avg. steps per trial " + m_cSteps / iTest );
		}
		
		if( bOutputMessages ){
			iEndTime = System.currentTimeMillis();
			lEndCPU = JProf.getCurrentThreadCpuTimeSafe();
			cCurrentMilliseconds = ( iEndTime - iStartTime ) / 1000;
			lTotalCPU = ( lEndCPU - lStartCPU ) / 1000000000;
			dADR = dSumDiscountedRewards / iTest;
			dStdev = Math.sqrt( ( dSumSquares - ( iTest + 1 ) * dADR * dADR ) / iTest );
			dStandardError = 2.0 * dStdev / Math.sqrt( iTest );
			Logger.getInstance().logln();
			Logger.getInstance().log( "POMDP", 0, "computeAverageDiscountedReward", "After " + iTest + " tests. ADR " + round( dADR, 3 ) +
					", stdev " + round( dStdev, 5 ) + " SE " + round( dStandardError, 5 ) + " time " + lTotalCPU );
		}


		String sMsg = "action execution ";
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			sMsg += getActionName( iAction ) + " = " + aiActionCount[iAction] + ", ";
		}
		//Logger.getInstance().logln( sMsg );
		
		countStatistics( true );

		m_rndGenerator = rndGenerator;
		
		getBeliefStateFactory().cacheBeliefStates( bCacheBeliefStates );
		
		return dSumDiscountedRewards / iTest;
	}
	
	public double computeAverageDiscountedRewardParticleFiltering( int cTests, int cMaxStepsToGoal, PolicyStrategy policy, boolean bOutputMessages, boolean bUseMultiThread ){
		countStatistics( false );
		
		m_cSteps = 0;
		int cParticles = 10;
		
		double dSumDiscountedRewards = 0.0, dDiscountedReward = 0.0, dSumSquares = 0.0;
		int iTest = 0, iAction = 0;
		long cCurrentMilliseconds = 0, iStartTime = System.currentTimeMillis(), iEndTime = 0;
		long lTotalCPU = 0, lStartCPU = JProf.getCurrentThreadCpuTimeSafe(), lEndCPU = 0;
		RandomGenerator rndGenerator = m_rndGenerator;
		//m_rndGenerator = new RandomGenerator( "ADR", m_iRandomSeed );
		double dPreviousDiscountedReward = 0.0;
		int[] aiActionCount = new int[m_cActions];	
		double dStdev = 10000.0, dStandardError = 10.0, dADR = 0.0;
		
		boolean bCacheBeliefStates = getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iTest = 0 ; ( iTest < cTests ) && ( dStandardError > 0.05 * dADR  )  ; iTest++ ){
			dPreviousDiscountedReward = dDiscountedReward;
			dDiscountedReward = computeDiscountedRewardWithParticleFiltering( cMaxStepsToGoal, policy, cParticles );
			//dDiscountedReward = computeDiscountedRewardI( cMaxStepsToGoal, policy, null, false, null );
			dSumSquares += ( dDiscountedReward * dDiscountedReward );
			dSumDiscountedRewards += dDiscountedReward;
				
			if( bOutputMessages && ( iTest % 100 == 0 ) ){
				Logger.getInstance().log( "" + ( iTest / 100 ) % 10 );
			}
			
			//Logger.getInstance().logln( iTest + ") " + dDiscountedReward + ", " + Math.abs( dDiscountedReward - dPreviousDiscountedReward ) );
			
			if( iTest >= 10 && iTest % 10 == 0 ){
				dADR = dSumDiscountedRewards / ( iTest + 1 );
				dStdev = Math.sqrt( dSumSquares / ( iTest + 1 ) - dADR * dADR );
				dStandardError = 2.0 * dStdev / Math.sqrt( iTest + 1 );
				Logger.getInstance().logln( iTest + ") " + dADR + ", " + dStdev + ", " + dStandardError );
			}
		}
		
		if( bOutputMessages ){
			iEndTime = System.currentTimeMillis();
			lEndCPU = JProf.getCurrentThreadCpuTimeSafe();
			cCurrentMilliseconds = ( iEndTime - iStartTime ) / 1000;
			lTotalCPU = ( lEndCPU - lStartCPU ) / 1000000000;
			dADR = dSumDiscountedRewards / iTest;
			dStdev = Math.sqrt( ( dSumSquares - ( iTest + 1 ) * dADR * dADR ) / iTest );
			dStandardError = 2.0 * dStdev / Math.sqrt( iTest );
			Logger.getInstance().logln();
			Logger.getInstance().log( "POMDP", 0, "computeAverageDiscountedReward", "After " + iTest + " tests. ADR " + round( dADR, 3 ) +
					", stdev " + round( dStdev, 5 ) + " SE " + round( dStandardError, 5 ) + " time " + lTotalCPU );
		}

		String sMsg = "action execution ";
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			sMsg += getActionName( iAction ) + " = " + aiActionCount[iAction] + ", ";
		}
		//Logger.getInstance().logln( sMsg );
		
		countStatistics( true );

		m_rndGenerator = rndGenerator;
		
		getBeliefStateFactory().cacheBeliefStates( bCacheBeliefStates );
		
		return dSumDiscountedRewards / iTest;
	}

	public double computeAverageDiscountedRewardParticleFilteringImportanceSampling( int cTests, int cMaxStepsToGoal, PolicyStrategy policy, boolean bOutputMessages, boolean bUseMultiThread ){
		countStatistics( false );
		
		m_cSteps = 0;
		
		int cParticles = 10;
		double dSumDiscountedRewards = 0.0, dDiscountedReward = 0.0, dSumSquares = 0.0;
		int iTest = 0;
		long cCurrentMilliseconds = 0, iStartTime = System.currentTimeMillis(), iEndTime = 0;
		long lTotalCPU = 0, lStartCPU = JProf.getCurrentThreadCpuTimeSafe(), lEndCPU = 0;
		RandomGenerator rndGenerator = m_rndGenerator;
		//m_rndGenerator = new RandomGenerator( "ADR", m_iRandomSeed );
		double dStdev = 10000.0, dStandardError = 10.0, dADR = 0.0;
		
		boolean bCacheBeliefStates = getBeliefStateFactory().cacheBeliefStates( false );
		
		m_rndGenerator = new RandomGenerator( "test", 0 );
		
		for( iTest = 0 ; ( iTest < cTests ) && ( dStandardError > 0.01 * dADR  )  ; iTest++ ){
			dDiscountedReward = computeDiscountedRewardParticleFilteringImportanceSampling( cMaxStepsToGoal, policy, cParticles );
			dSumSquares += ( dDiscountedReward * dDiscountedReward );
			dSumDiscountedRewards += dDiscountedReward;
				
			if( bOutputMessages && ( iTest % 100 == 0 ) ){
				Logger.getInstance().log( "" + ( iTest / 100 ) % 10 );
			}
			
			//Logger.getInstance().logln( iTest + ") " + dDiscountedReward );
			
			if( iTest >= 10 && iTest % 10 == 0 ){
				dADR = dSumDiscountedRewards / ( iTest + 1 );
				dStdev = Math.sqrt( dSumSquares / ( iTest + 1 ) - dADR * dADR );
				dStandardError = 2.0 * dStdev / Math.sqrt( iTest + 1 );
				iEndTime = System.currentTimeMillis();
				lEndCPU = JProf.getCurrentThreadCpuTimeSafe();
				cCurrentMilliseconds = ( iEndTime - iStartTime ) / 1000;
				lTotalCPU = ( lEndCPU - lStartCPU ) / 1000000000;
				Logger.getInstance().logln( iTest + ") " + dADR + ", " + dStdev + ", " + dStandardError + " time " + lTotalCPU );
			}
		}
		
		if( bOutputMessages ){
			iEndTime = System.currentTimeMillis();
			lEndCPU = JProf.getCurrentThreadCpuTimeSafe();
			cCurrentMilliseconds = ( iEndTime - iStartTime ) / 1000;
			lTotalCPU = ( lEndCPU - lStartCPU ) / 1000000000;
			dADR = dSumDiscountedRewards / iTest;
			dStdev = Math.sqrt( ( dSumSquares - ( iTest + 1 ) * dADR * dADR ) / iTest );
			dStandardError = 2.0 * dStdev / Math.sqrt( iTest );
			Logger.getInstance().logln();
			Logger.getInstance().log( "POMDP", 0, "computeAverageDiscountedReward", "After " + iTest + " tests. ADR " + round( dADR, 3 ) +
					", stdev " + round( dStdev, 5 ) + " SE " + round( dStandardError, 5 ) + " time " + lTotalCPU );
		}
		
		countStatistics( true );

		m_rndGenerator = rndGenerator;
		
		getBeliefStateFactory().cacheBeliefStates( bCacheBeliefStates );
		
		return dSumDiscountedRewards / iTest;
	}

	private double computeExpectation( Vector<Double> vPrLogs, Vector<Double> vValues ){
		int iElement = 0, cElements = vPrLogs.size();
		Vector<Double> vLogs = new Vector<Double>();
		double dPrLog = 0.0, dValue = 0.0;
		for( iElement = 0 ; iElement < cElements ; iElement++ ){
			dPrLog = vPrLogs.elementAt( iElement );
			dValue = vValues.elementAt( iElement );
			if( dValue > 0.0 )
				vLogs.add( dPrLog + Math.log( dValue ) );
		}
		return Math.exp( computeSumExp( vLogs ) );
	}
	
	private double computeVariance( Vector<Double> vPrLogs, Vector<Double> vValues, double dExpectation ){
		int iElement = 0, cElements = vPrLogs.size();
		Vector<Double> vLogs = new Vector<Double>();
		double dPrLog = 0.0, dValue = 0.0, dSquareValue = 0.0;
		for( iElement = 0 ; iElement < cElements ; iElement++ ){
			dPrLog = vPrLogs.elementAt( iElement );
			dValue = vValues.elementAt( iElement ) - dExpectation;
			dSquareValue = dValue * dValue;
			if( dValue > 0.0 )
				vLogs.add( dPrLog + Math.log( dSquareValue ) );
		}
		return Math.exp( computeSumExp( vLogs ) );
	}
	
    public double computeSumExpII(Vector<Double> vLogs){
    	SortedSet<Double> sLogs = new TreeSet<Double>( vLogs );
    	double dSum = 0.0;
    	for( double dLog : sLogs ){
    		dSum += Math.exp( dLog );
    	}
    	return Math.log( dSum );
    }
	
    public double computeSumExp(Vector<Double> vLogs){
        int cElements = vLogs.size();
        if (cElements == 1)
            return vLogs.elementAt( 0 );
        double dLogMax = Double.NEGATIVE_INFINITY, dLogK = 0.0, dSum = 0.0;
        for (double dLog : vLogs){
            if (dLog > dLogMax)
                dLogMax = dLog;
        }
        if (dLogMax == Double.NEGATIVE_INFINITY)
            return Double.NEGATIVE_INFINITY;
        dLogK = Math.log(Double.MAX_VALUE) - Math.log(cElements + 1) - dLogMax;
        for (double dLog : vLogs){
            if( !Double.isInfinite( dLog ) ) 
                dSum += Math.exp( dLog + dLogK );
        }
        return Math.log( dSum ) - dLogK;
    }
	

	protected double computeMDPAverageDiscountedReward( int cTests, int cMaxStepsToGoal ){
		MDPValueFunction vfMDP = getMDPValueFunction();
		vfMDP.valueIteration( 1000, ExecutionProperties.getEpsilon() );

		//m_rndGenerator.init( m_iRandomSeed );
		
		double dSumDiscountedRewards = 0.0;
		int iTest = 0;
		long cCurrentMilliseconds = 0, iStartTime = System.currentTimeMillis(), iEndTime = 0;
		boolean bCacheBeliefState = getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iTest = 0 ; iTest < cTests ; iTest++ ){
			dSumDiscountedRewards += computeMDPDiscountedReward( cMaxStepsToGoal, vfMDP, false, null );
		}
		
		iEndTime = System.currentTimeMillis();
		cCurrentMilliseconds = ( iStartTime - iEndTime ) / 1000;
		
		String sInfo = "Status: #tests = " + cTests + ": " + 
			" Time: " + cCurrentMilliseconds +
			" Reward: " + dSumDiscountedRewards / cTests + g_sNewline;
		Logger.getInstance().logln( sInfo );

		getBeliefStateFactory().cacheBeliefStates( bCacheBeliefState );
		return round( dSumDiscountedRewards / cTests, 3 );
	}
	
	protected double round( double d, int cDigits ) {
		double dPower = Math.pow( 10, cDigits );
		double d1 = Math.round( d1 = d * dPower );
		return d1 / dPower;
	}

	protected double computeDiscountedReward( int cMaxStepsToGoal, PolicyStrategy policy, int[] aiActionCount ){
		return computeDiscountedReward( cMaxStepsToGoal, policy, null, false, aiActionCount );
	}
	
	protected double computeDiscountedReward( int cMaxStepsToGoal, PolicyStrategy policy, Vector<BeliefState> vObservedBeliefPoints, int[] aiActionCount ){
		return computeDiscountedReward( cMaxStepsToGoal, policy, vObservedBeliefPoints, false, aiActionCount );
	}
	
	/**
	 * Execute a single trial for computing discounted reward (DR). Simulates agent-environment interactions. 
	 * @param cMaxStepsToGoal - maximal steps per trial. Used if the policy causes the agent to get stuck in a loop.
	 * @param policy - specifies agent actions given belief states (POMDP policy).
	 * @param vObservedBeliefPoints - can store observed belief points. Set to null to avoid storing.
	 * @param bExplore - specifies whther the agent explores.
	 * @param aiActionCount - counts the specific actions executed by the agent. Must be of size |A|.
	 * @return
	 */
	
	private int m_cSteps = 0;
	//private int x = 0;
	public double computeDiscountedReward( int cMaxStepsToGoal, PolicyStrategy policy, Vector<BeliefState> vObservedBeliefPoints, boolean bExplore, int[] aiActionCount ){
		//if( x++ % 2 == 0 )
			//return computeDiscountedRewardI( cMaxStepsToGoal, policy, vObservedBeliefPoints, bExplore, aiActionCount );
		//else
			return computeDiscountedRewardII( cMaxStepsToGoal, policy, vObservedBeliefPoints, bExplore, aiActionCount );
	}
	
	public double computeDiscountedRewardWithParticleFiltering( int cMaxStepsToGoal, PolicyStrategy policy, int cMaxBeliefs ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		double dCurrentStepDiscountedReward = 0.0, dSumWeights = 0.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		BeliefState bsCurrentBelief = null, bsNext = null;
		boolean bDone = false, bFound = false;
		BeliefStateComparator comp = new BeliefStateComparator( 0.0001 );
		Vector<Pair<BeliefState, Double>> vCurrentBeliefs = new Vector<Pair<BeliefState,Double>>();
		Vector<Pair<BeliefState, Double>> vNextBeliefs = new Vector<Pair<BeliefState,Double>>();
		double dWeight = 0.0, dPrOGivenA = 0.0, dSumTerminalBeliefs = 0.0, dSumNextWeights = 0.0;
		
		vCurrentBeliefs.add( new Pair<BeliefState, Double>( getBeliefStateFactory().getInitialBeliefState(), 1.0 ) );
		
		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			m_cSteps++;
			dCurrentStepDiscountedReward = 0.0;
			dSumWeights = 0.0;
			dSumNextWeights = 0.0;
			dSumTerminalBeliefs = 0.0;
			
			for( Entry<BeliefState, Double> e : vCurrentBeliefs ){
				bsCurrentBelief = e.getKey();
				
				dWeight = e.getValue();
				dSumWeights += dWeight;

				if( bsCurrentBelief.isDeterministic() && isTerminalState( bsCurrentBelief.getDeterministicIndex() ) ){
					dSumTerminalBeliefs += dWeight;
				}
				
				iAction = policy.getAction( bsCurrentBelief );
				
				for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){				
					bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
					if( bsNext != null ){
						dPrOGivenA = bsCurrentBelief.probabilityOGivenA( iAction, iObservation );
						bFound = false;
						for( Pair<BeliefState, Double> eNext : vNextBeliefs ){
							if( comp.compare( bsNext, eNext.getKey() ) == 0 ){
								eNext.setValue( eNext.getValue() + dWeight * dPrOGivenA );
								bFound = true;
								break;
							}
						}
						if( !bFound )
							vNextBeliefs.add( new Pair<BeliefState, Double>( bsNext, dWeight * dPrOGivenA ) );

						dSumNextWeights += dWeight * dPrOGivenA;
						
						dCurrentReward = R( bsCurrentBelief, iAction, bsNext );
						if( dCurrentReward != 0.0 ){
							dCurrentStepDiscountedReward += dWeight * dPrOGivenA * dCurrentReward * dDiscountFactor;
						}
					}
				}
				
			}
			
			if( dSumTerminalBeliefs > 0.99 ){
				bDone = true;
			}
			else{
				vCurrentBeliefs = reduceBeliefSet( vNextBeliefs, cMaxBeliefs, policy, comp );
				vNextBeliefs.clear();
			}
			
			dDiscountedReward += dCurrentStepDiscountedReward / dSumWeights;
			
			dDiscountFactor *= m_dGamma;
		}	
		
		return dDiscountedReward;
	}
	
	public double computeDiscountedRewardWithParticleFilteringImportanceSampling( int cMaxStepsToGoal, PolicyStrategy policy, int cMaxBeliefs ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		double dCurrentStepDiscountedReward = 0.0, dSumWeights = 0.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		BeliefState bsCurrentBelief = null, bsNext = null;
		boolean bDone = false;
		BeliefStateComparator comp = new BeliefStateComparator( 0.0001 );
		Vector<Pair<BeliefState, Double>> vCurrentBeliefs = new Vector<Pair<BeliefState,Double>>();
		Vector<Pair<BeliefState, double[]>> vNextBeliefs = new Vector<Pair<BeliefState, double[]>>();
		double dWeight = 0.0, dPrOGivenA = 0.0, dSumTerminalBeliefs = 0.0, dValue = 0.0;
		double[] adNextBeliefWeights = null;
		double dValueOffset = 0.1;
		
		vCurrentBeliefs.add( new Pair<BeliefState, Double>( getBeliefStateFactory().getInitialBeliefState(), 1.0 ) );
		
		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			m_cSteps++;
			dCurrentStepDiscountedReward = 0.0;
			dSumWeights = 0.0;
			dSumTerminalBeliefs = 0.0;
			
			for( Entry<BeliefState, Double> e : vCurrentBeliefs ){
				bsCurrentBelief = e.getKey();
				
				dWeight = e.getValue();
				dSumWeights += dWeight;

				if( bsCurrentBelief.isDeterministic() && isTerminalState( bsCurrentBelief.getDeterministicIndex() ) ){
					dSumTerminalBeliefs += dWeight;

					adNextBeliefWeights = new double[3];
					adNextBeliefWeights[0] = dWeight;
					adNextBeliefWeights[1] = 1.0;
					adNextBeliefWeights[2] = dValueOffset;
					vNextBeliefs.add( new Pair<BeliefState, double[]>( bsCurrentBelief, adNextBeliefWeights ) );
				}
				else{
					iAction = policy.getAction( bsCurrentBelief );
					
					for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){				
						bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
						if( bsNext != null ){
							dPrOGivenA = bsCurrentBelief.probabilityOGivenA( iAction, iObservation );
							dValue = policy.getMDPValueFunction().getValue( bsNext ) + dValueOffset;
							//dValue = policy.getValue( bsNext ) + dValueOffset;
							//dValue = 1.0;
							adNextBeliefWeights = new double[3];
							adNextBeliefWeights[0] = dWeight;
							adNextBeliefWeights[1] = dPrOGivenA;
							adNextBeliefWeights[2] = dValue;
							vNextBeliefs.add( new Pair<BeliefState, double[]>( bsNext, adNextBeliefWeights ) );
	
							dCurrentReward = R( bsCurrentBelief, iAction, bsNext );
							if( dCurrentReward != 0.0 ){
								dCurrentStepDiscountedReward += dWeight * dPrOGivenA * dCurrentReward * dDiscountFactor;
							}
						}
					}	
				}
			}
			
			if( dSumTerminalBeliefs > 0.99 ){
				bDone = true;
			}
			else{
				vCurrentBeliefs = reduceBeliefSet( vNextBeliefs, cMaxBeliefs, comp );
				vNextBeliefs.clear();
			}
			
			dDiscountedReward += dCurrentStepDiscountedReward / dSumWeights;
			
			dDiscountFactor *= m_dGamma;
		}	
		
		return dDiscountedReward;
	}
	
	private Vector<Pair<BeliefState, Double>> reduceBeliefSet( Vector<Pair<BeliefState, double[]>> vNextBeliefs, int cMaxSamples, BeliefStateComparator comp ) {
		double dRand = 0.0, dSumWeights = 0.0, dValue = 0.0;
		boolean bFound = false;
		int iSample = 0;
		double[] adBeliefWeights = null;
		Vector<Pair<BeliefState, double[]>> vSample = new Vector<Pair<BeliefState,double[]>>();
		Vector<Pair<BeliefState, Double>> vReduced = new Vector<Pair<BeliefState,Double>>();
		
		for( Pair<BeliefState, double[]> pOrg : vNextBeliefs ){
			adBeliefWeights = pOrg.getValue();
			//dValue = adBeliefWeights[1];// * adBeliefWeights[2];
			dValue = adBeliefWeights[0] * adBeliefWeights[1] * adBeliefWeights[2];
			dSumWeights += dValue;
		}
		
		vReduced = new Vector<Pair<BeliefState,Double>>();
		for( iSample = 0 ; iSample < cMaxSamples ; iSample++ ){
			dRand = m_rndGenerator.nextDouble( dSumWeights );
			for( Pair<BeliefState, double[]> pOrg : vNextBeliefs ){
				adBeliefWeights = pOrg.getValue();
				//dValue = adBeliefWeights[1];// * adBeliefWeights[2];
				dValue = adBeliefWeights[0] * adBeliefWeights[1] * adBeliefWeights[2];
				dRand -= dValue;
				if( dRand <= 0.0 ){
					vSample.add( new Pair<BeliefState, double[]>( pOrg.getKey(), adBeliefWeights ) );
					break;
				}
			}
		}
		dSumWeights = 0.0;
		for( Pair<BeliefState, double[]> pSample : vSample ){
			adBeliefWeights = pSample.getValue();
			//dValue = adBeliefWeights[0];// / adBeliefWeights[2]; //w_t = w_{t-1} * p(x_t) / h(x_t)
			dValue = 1.0 / adBeliefWeights[2];
			bFound = false;						
			for( Pair<BeliefState, Double> pNew : vReduced ){
				if( comp.compare( pNew.getKey(), pSample.getKey() ) == 0 ){
					pNew.setValue( pNew.getValue() + dValue );
					bFound = true;
					break;
				}
			}		
			if( !bFound )
				vReduced.add( new Pair<BeliefState, Double>( pSample.getKey(), dValue ) );
			dSumWeights += dValue;
		}				
		for( Pair<BeliefState, Double> pNew : vReduced ){
			pNew.setValue( pNew.getValue() / dSumWeights );
		}
		return vReduced;
	}
	
	//sample with repetitions
	private Vector<Pair<BeliefState, Double>> reduceBeliefSet( Vector<Pair<BeliefState, Double>> vNextBeliefs, int cMaxSamples, PolicyStrategy policy, BeliefStateComparator comp ) {
		double dRand = 0.0, dSumWeights = 0.0;
		boolean bFound = false;
		int iSample = 0;
		Vector<Pair<BeliefState, Double>> vReduced = null;
		
		if( vNextBeliefs.size() < cMaxSamples ){
			vReduced = new Vector<Pair<BeliefState,Double>>( vNextBeliefs );
		}
		else{
			vReduced = new Vector<Pair<BeliefState,Double>>();
			for( iSample = 0 ; iSample < cMaxSamples ; iSample++ ){
				dRand = m_rndGenerator.nextDouble( 1.0 );
				for( Pair<BeliefState, Double> pOrg : vNextBeliefs ){
					dRand -= pOrg.getValue();
					if( dRand <= 0.0 ){
						bFound = false;						
						for( Pair<BeliefState, Double> pNew : vReduced ){
							if( comp.compare( pNew.getKey(), pOrg.getKey() ) == 0 ){
								pNew.setValue( pNew.getValue() + 1.0 );
								bFound = true;
							}
						}						
						if( !bFound ){
							vReduced.add( new Pair<BeliefState, Double>( pOrg.getKey(), 1.0 ) );
						}
						dSumWeights++;
						break;
					}
				}
			}
			for( Pair<BeliefState, Double> pNew : vReduced ){
				pNew.setValue( pNew.getValue() / cMaxSamples );
			}
		}
		return vReduced;
	}

	public double computeDiscountedRewardI( int cMaxStepsToGoal, PolicyStrategy policy, Vector<BeliefState> vObservedBeliefPoints, boolean bExplore, int[] aiActionCount ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		BeliefState bsCurrentBelief = getBeliefStateFactory().getInitialBeliefState(), bsNext = null;
		boolean bDone = false;
		int cRewards = 0;
		
		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			m_cSteps++;
			
			if( bExplore ){
				double dRand = m_rndGenerator.nextDouble();
				if( dRand > 0.1 )
					iAction = policy.getAction( bsCurrentBelief );
				else
					iAction = m_rndGenerator.nextInt( m_cActions );
			}
			else{
				iAction = policy.getAction( bsCurrentBelief );
			}
			if( aiActionCount != null )
				aiActionCount[iAction]++;
			
			if( vObservedBeliefPoints != null ){
				bsCurrentBelief.setFactoryPersistence( true );
				vObservedBeliefPoints.add( bsCurrentBelief );
			}
			
			iObservation = observe( bsCurrentBelief, iAction );
			
			bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
			
			dCurrentReward = R( bsCurrentBelief, iAction, bsNext );
			if( dCurrentReward != 0.0 )
				dDiscountedReward += dCurrentReward * dDiscountFactor;
			dDiscountFactor *= m_dGamma;
			
			if( dCurrentReward != 0 )
				cRewards++;

			if( bsCurrentBelief.isDeterministic() )
				bDone = endADR( bsCurrentBelief.getDeterministicIndex(), dCurrentReward );
			 
			bsCurrentBelief = bsNext;			
		}	
		
		return dDiscountedReward;// + dDiscountFactor * m_dMinReward * ( 1 / ( 1 - m_dGamma ) );
	}
	
	BeliefStateFactory bsf = null;
	
	public double computeDiscountedRewardII( int cMaxStepsToGoal, PolicyStrategy policy, Vector<BeliefState> vObservedBeliefPoints, boolean bExplore, int[] aiActionCount ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		int iState = chooseStartState(), iNextState = 0;
		BeliefState bsCurrentBelief = getBeliefStateFactory().getInitialBeliefState(), bsNext = null;
		
		boolean bDone = false;
		int cRewards = 0;
		int cSameStates = 0;
		
		if( m_vVisualisationUnit != null )
			m_vVisualisationUnit.Show();
		
		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			if( m_vVisualisationUnit != null )
				m_vVisualisationUnit.UpdateState(bsCurrentBelief, iState);
			
			m_cSteps++;
			
			if( bExplore ){
				double dRand = m_rndGenerator.nextDouble();
				if( dRand > 0.1 )
					iAction = policy.getAction( bsCurrentBelief );
				else
					iAction = m_rndGenerator.nextInt( m_cActions );
			}
			else{
				iAction = policy.getAction( bsCurrentBelief );
				if( iAction == -1 )
					throw new Error( "Could not find optimal action for bs " + bsCurrentBelief );
					
			}
			
			if( iAction == -1 )
				return Double.NEGATIVE_INFINITY;
			
			if( aiActionCount != null )
				aiActionCount[iAction]++;
			
			if( vObservedBeliefPoints != null ){
				bsCurrentBelief.setFactoryPersistence( true );
				vObservedBeliefPoints.add( bsCurrentBelief );
			}
			
			iNextState = execute( iAction, iState );
			iObservation = observe( iAction, iNextState );
			
			if( m_rtReward == RewardType.StateAction )
				dCurrentReward = R( iState, iAction ); //R(s,a)
			else if( m_rtReward == RewardType.StateActionState )
				dCurrentReward = R( iState, iAction, iNextState ); //R(s,a,s')
			else if( m_rtReward == RewardType.State )
				dCurrentReward = R( iState );
			dDiscountedReward += dCurrentReward * dDiscountFactor;
			dDiscountFactor *= m_dGamma;
			
			if( dCurrentReward != 0 )
				cRewards++;

			bDone = endADR( iNextState, dCurrentReward );
			if( bDone )
				dDiscountFactor = 0.0;
			
			bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
			
			if( iState != iNextState )
				cSameStates = 0;
			else
				cSameStates++;
			if( bsNext == null || ( bsNext.valueAt( iNextState ) == 0 ) || ( cSameStates > 10 ) ){
				bDone = true;
			}

			iState = iNextState;
			bsCurrentBelief.release();
			bsCurrentBelief = bsNext;	
		}	
		if( m_vVisualisationUnit != null )
			m_vVisualisationUnit.Hide();
		return dDiscountedReward;// + m_dMinReward * ( 1 / ( 1 - dDiscountFactor ) );
	}
	public double computeDiscountedRewardParticleFilteringImportanceSamplingII( int cMaxStepsToGoal, PolicyStrategy policy, int cParticles ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		BeliefState[] aCurrent = new BeliefState[cParticles], aNext = null;
		double[] adWeights = null, adProb = null;
		int iStep = 0, iParticle = 0, iSample = 0, iAction = 0, iObservation = 0, cRemianingParticles = cParticles;
		BeliefState bsCurrent = null, bsNext = null;
		double dTrueProbability = 0.0, dImportanceProbability = 0.0, dWeight = 0.0;
		boolean bDone = false;
		
		for( iParticle = 0 ; iParticle < cParticles; iParticle++ )
			aCurrent[iParticle] = getBeliefStateFactory().getInitialBeliefState();		
		
		for( iStep = 0 ; iStep < cMaxStepsToGoal && !bDone ; iStep++ ){			
			adWeights = new double[cParticles];
			adProb = new double[cParticles];
			aNext = new BeliefState[cParticles];
			
			bDone = true;
			
			for( iParticle = 0 ; iParticle < cRemianingParticles ; iParticle++ ){
				bsCurrent = aCurrent[iParticle];

				if( bsCurrent.isTerminalBelief() ){
					adWeights[iParticle] = 0.0;
					aNext[iParticle] = bsCurrent;
					adProb[iParticle] = 1.0;
					cRemianingParticles--;
				}
				else{
					bDone = false;
				
					iAction = policy.getAction( bsCurrent );
					Number[] anSampling = policy.getMDPValueFunction().importanceSampling( iAction, bsCurrent );
					iObservation = anSampling[0].intValue();
					bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
					dImportanceProbability = anSampling[1].doubleValue();
					dTrueProbability = bsCurrent.probabilityOGivenA( iAction, iObservation );
					dWeight = dTrueProbability / dImportanceProbability;
					adWeights[iParticle] = dWeight;
					adProb[iParticle] = dTrueProbability;
					aNext[iParticle] = bsNext;
					
					dCurrentReward = dWeight * R( bsCurrent, iAction, bsNext ) / cParticles;
					if( dCurrentReward > 0 )
						R( bsCurrent, iAction, bsNext );
					
					dDiscountedReward += dDiscountFactor * dCurrentReward;
				}
			}
			if( !bDone ){
				aCurrent = new BeliefState[cParticles];
				for( iParticle = 0 ; iParticle < cRemianingParticles ; iParticle++ ){
					iSample = sampleFromWeights( adWeights );
					aCurrent[iParticle] = aNext[iSample];					
				}
			}
			dDiscountFactor *= m_dGamma;
		}
		
		return dDiscountedReward;
	}	
	
	public double computeDiscountedRewardParticleFilteringImportanceSampling( int cMaxStepsToGoal, PolicyStrategy policy, int cParticles ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		int iStep = 0, iAction = 0, iObservation = 0;
		BeliefState bsCurrentBelief = getBeliefStateFactory().getInitialBeliefState(), bsNext = null;
		boolean bDone = false;
		int iParticle = 0, iSample = -1;
		int iState = -1, iNextState = -1;
		double dWeight = 0.0, dTruePr = 0.0, dImportancePr = 0.0;
		int cUnfinishedParticles = cParticles;
		Number[] anSampling = policy.getMDPValueFunction().importanceSamplingForStartState();
		
		Vector<BeliefState[]> vBeliefTrajectories = new Vector<BeliefState[]>();
		Vector<int[]> vStateTrajectories = new Vector<int[]>();
		Vector<double[]> vWeightTrajectories = new Vector<double[]>();
		Vector<int[]> vParents = new Vector<int[]>();
		Vector<double[]> vDiscountedRewards = new Vector<double[]>(); 
		Vector<double[]> vTrueProbabilities = new Vector<double[]>(); 
		Vector<double[]> vImportanceProbabilities = new Vector<double[]>(); 
		Vector<double[]> vUnNormalizedWeighteds = new Vector<double[]>(); 
		Vector<Double> vFinalWeights = new Vector<Double>();
		Vector<Double> vFinalDiscountedRewards = new Vector<Double>();

		vBeliefTrajectories.add( new BeliefState[cParticles] );
		vStateTrajectories.add( new int[cParticles] );
		vWeightTrajectories.add( new double[cParticles] );
		vParents.add( new int[cParticles] );
		vDiscountedRewards.add( new double[cParticles] );
		vTrueProbabilities.add( new double[cParticles] );
		vImportanceProbabilities.add( new double[cParticles] );
		vUnNormalizedWeighteds.add( new double[cParticles] );
		
		for( iParticle = 0 ; iParticle < cParticles ; iParticle++ ){
			vBeliefTrajectories.elementAt( 0 )[iParticle] = getBeliefStateFactory().getInitialBeliefState();
			anSampling = policy.getMDPValueFunction().importanceSamplingForStartState();
			iState = anSampling[0].intValue();
			vWeightTrajectories.elementAt( 0 )[iParticle] = probStartState( iState ) / anSampling[1].doubleValue();
			
			vTrueProbabilities.elementAt( 0 )[iParticle] = probStartState( iState );
			vImportanceProbabilities.elementAt( 0 )[iParticle] = anSampling[1].doubleValue();
			vUnNormalizedWeighteds.elementAt( 0 )[iParticle] = probStartState( iState ) / anSampling[1].doubleValue();
			
			vStateTrajectories.elementAt( 0 )[iParticle] = iState;
			vParents.elementAt( 0 )[iParticle] = -1;
		}
		
		normalizeWeights( vWeightTrajectories.elementAt( 0 ) );
		
		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			
			m_cSteps++;
			
			vBeliefTrajectories.add( new BeliefState[cUnfinishedParticles] );
			vStateTrajectories.add( new int[cUnfinishedParticles] );
			vWeightTrajectories.add( new double[cUnfinishedParticles] );
			vParents.add( new int[cUnfinishedParticles] );
			vDiscountedRewards.add( new double[cUnfinishedParticles] );
			vTrueProbabilities.add( new double[cUnfinishedParticles] );
			vImportanceProbabilities.add( new double[cUnfinishedParticles] );
			vUnNormalizedWeighteds.add( new double[cUnfinishedParticles] );

			bDone = true;
			if( cUnfinishedParticles > 0 ){
				iSample = 0;
				for( iParticle = 0 ; iParticle < cUnfinishedParticles ; iParticle++ ){
					
					if( vUnNormalizedWeighteds.elementAt( iStep )[iSample] > 0 ){
						
						dWeight = vWeightTrajectories.elementAt( iStep )[iSample];
						bsCurrentBelief = vBeliefTrajectories.elementAt( iStep )[iSample];
						iState = vStateTrajectories.elementAt( iStep )[iSample];
					
						iAction = policy.getAction( bsCurrentBelief );
										
						anSampling = policy.getMDPValueFunction().importanceSampling( iAction, iState );
						iNextState = anSampling[0].intValue();
						iObservation = observe( iAction, iNextState );
						
						if( m_rtReward == RewardType.StateAction )
							dCurrentReward = R( iState, iAction ); //R(s,a)
						else if( m_rtReward == RewardType.StateActionState )
							dCurrentReward = R( iState, iAction, iNextState ); //R(s,a,s')
						else if( m_rtReward == RewardType.State )
							dCurrentReward = R( iState );
											
						dDiscountedReward += dWeight * dDiscountFactor * dCurrentReward;
						
						dTruePr = tr( iState, iAction, iNextState ) * O( iAction, iNextState, iObservation );
						dImportancePr = anSampling[1].doubleValue() * O( iAction, iNextState, iObservation );
	
						bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
						
						if( isTerminalState( iNextState ) ){
							vFinalWeights.add( vUnNormalizedWeighteds.elementAt( iStep )[iSample] * ( dTruePr / dImportancePr ) );
							vFinalDiscountedRewards.add( vDiscountedRewards.elementAt( iStep - 1 )[iSample] + dDiscountFactor * dCurrentReward );
							vUnNormalizedWeighteds.elementAt( iStep + 1 )[iParticle] = 0.0;
						}
						else{
							bDone = false;
							vUnNormalizedWeighteds.elementAt( iStep + 1 )[iParticle] = vUnNormalizedWeighteds.elementAt( iStep )[iSample] * ( dTruePr / dImportancePr );
						}
						
						vTrueProbabilities.elementAt( iStep + 1 )[iParticle] = dTruePr;
						vImportanceProbabilities.elementAt( iStep + 1 )[iParticle] = dImportancePr;
																																					 
						vParents.elementAt( iStep + 1 )[iParticle] = iSample;
						vWeightTrajectories.elementAt( iStep + 1 )[iParticle] = dWeight * ( dTruePr / dImportancePr );
						//vWeightTrajectories.elementAt( iStep + 1 )[iParticle] = ( 1.0 / cParticles ) * dTruePr / dImportancePr;
						vStateTrajectories.elementAt( iStep + 1 )[iParticle] = iNextState;
						vBeliefTrajectories.elementAt( iStep + 1 )[iParticle] = bsNext;
						if( iStep > 0 )
							vDiscountedRewards.elementAt( iStep )[iParticle] = vDiscountedRewards.elementAt( iStep - 1 )[iSample] + dDiscountFactor * dCurrentReward;
						else
							vDiscountedRewards.elementAt( iStep )[iParticle] = dDiscountFactor * dCurrentReward;
					}
					iSample++;
				}
				cUnfinishedParticles = cParticles - vFinalDiscountedRewards.size();
			}
			normalizeWeights( vWeightTrajectories.elementAt( iStep + 1 ) );
			dDiscountFactor *= m_dGamma;
		}	
		
		for( iParticle = 0 ; iParticle < cUnfinishedParticles ; iParticle++ ){
			vFinalWeights.add( vUnNormalizedWeighteds.elementAt( iStep )[iParticle] );
			vFinalDiscountedRewards.add( vDiscountedRewards.elementAt( iStep - 1 )[iParticle] );			
		}
		
		double dSumWeights = 0.0;
		double dNormalizedDiscountedReward = 0.0;
		for( iParticle = 0 ; iParticle < cParticles ; iParticle++ ){
			dWeight = vFinalWeights.elementAt( iParticle );
			dCurrentReward = vFinalDiscountedRewards.elementAt( iParticle );
			dNormalizedDiscountedReward += dWeight * dCurrentReward;
			dSumWeights += dWeight;
		}
		
		dNormalizedDiscountedReward /= dSumWeights;
		
		
		return dNormalizedDiscountedReward;
	}
	
	private void normalizeWeights( double[] adWeights ){
		double dSum = 0.0;
		int i = 0;
		for( i = 0 ; i < adWeights.length ; i++ ){
			dSum += adWeights[i];
		}
		for( i = 0 ; i < adWeights.length ; i++ ){
			adWeights[i] /= dSum;
		}
	}
	
	private int sampleFromWeights( double[] adWeights ) {
		int i = 0;
		double dSumWeights = 0.0;
		for( i = 0 ; i < adWeights.length ; i++ ){
			dSumWeights += adWeights[i];
		}
		double dRand = m_rndGenerator.nextDouble( dSumWeights );
		for( i = 0 ; i < adWeights.length ; i++ ){
			dRand -= adWeights[i];
			if( dRand <= 0 )
				return i;
		}
		return -1;
	}

	public boolean endADR( int iState, double dReward ){
		//return( ( terminalStatesDefined() && isTerminalState( iState ) ) ||
		//		( !terminalStatesDefined() && ( dReward > 0 ) ) );
		return(  terminalStatesDefined() && isTerminalState( iState ) );
	}
	
	/**
	 * Execute a single trial for computing discounted reward (DR) over the underlying MDP. Simulates agent-environment interactions. 
	 * Useful for checking what is the upper bound a policy might reach.
	 * @param cMaxStepsToGoal - maximal steps per trial. Used if the policy causes the agent to get stuck in a loop.
	 * @param policy - specifies agent actions given states (MDP policy).
	 * @param bExplore - specifies whther the agent explores.
	 * @param bMaintainBeliefStates - specifies whether belief states are computed through the trial.
	 * @return
	 */
	public double computeMDPDiscountedReward( int cMaxStepsToGoal, MDPValueFunction policy, boolean bExplore, Vector<BeliefState> vBeliefPoints ){
		double dDiscountedReward = 0.0, dCurrentReward = 0.0, dDiscountFactor = 1.0;
		int iStep = 0, iAction = 0, iObservation = 0, cRewards = 0, iStartState = 0;
		int iState = chooseStartState(), iNextState = 0;
		boolean bDone = false;
		double dProb = 0.0, dExplorationFactor = 0.2;
		BeliefState bsCurrentBelief = null, bsNext = null;
		
		if( vBeliefPoints != null )
			bsCurrentBelief = getBeliefStateFactory().getInitialBeliefState();
		
		iStartState = iState;
		policy.setExploration( false );

		for( iStep = 0 ; ( iStep < cMaxStepsToGoal ) && !bDone ; iStep++ ){
			iAction = policy.getAction( iState );
			if( bExplore ){
				dProb = m_rndGenerator.nextDouble();
				if( dProb < dExplorationFactor )
					iAction = m_rndGenerator.nextInt( m_cActions );
			}
			iNextState = execute( iAction, iState );
			iObservation = observe( iAction, iNextState );
			if( m_rtReward == RewardType.StateAction )
				dCurrentReward = R( iState, iAction ); //R(s,a)
			else if( m_rtReward == RewardType.StateActionState )
				dCurrentReward = R( iState, iAction, iNextState ); //R(s,a,s')
			else if( m_rtReward == RewardType.State )
				dCurrentReward = R( iState );
			dDiscountedReward += dCurrentReward * dDiscountFactor;
			dDiscountFactor *= m_dGamma;

			if( dCurrentReward != 0 )
				cRewards++;

			bDone = endADR( iNextState, dCurrentReward );
			
			if( false ){		
				Logger.getInstance().logln( "Step " + iStep + ": s = " + getStateName( iState ) + " a = " + getActionName( iAction ) + 
								" o = " + getObservationName( iObservation ) + " s' = " + getStateName( iNextState ) + 
								" reward = " + dCurrentReward + 
								" ADR = " + round( dDiscountedReward, 3 ) +
								" discount factor " + round( dDiscountFactor, 3 ) 
								 );
			}				
			iState = iNextState;
			if( vBeliefPoints != null ){
				if( !vBeliefPoints.contains( bsCurrentBelief ) )
					vBeliefPoints.add( bsCurrentBelief );
				bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
				
				if( bsNext == null ){
					//Logger.getInstance().logln( bsCurrentBelief + " " + iAction + " " + iObservation );
					//bsNext = bsCurrentBelief.nextBeliefState( iAction, iObservation );
					return dDiscountedReward;
				}
				
				bsCurrentBelief = bsNext;
			}
		}	
		if( vBeliefPoints != null ){
			if( !vBeliefPoints.contains( bsCurrentBelief ) )
				vBeliefPoints.add( bsCurrentBelief );
		}
		
		return dDiscountedReward;
	}
	
	private boolean isObsorving( int iState ){
		int iAction = 0;
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			if( tr( iState, iAction, iState ) < 1.0 )
				return false;
		}
		return true;
	}

	public int chooseStartState(){
		int iStartState = -1;
		double dInitialProb = m_rndGenerator.nextDouble();
		double dProb = dInitialProb;
		while( dProb > 0 ){
			iStartState++;
			dProb -= probStartState( iStartState );
		}
		return iStartState;
	}

		
	protected void setEnvironmentType( PolicyStrategy policy ){
		policy.setEnvironmentType( true );
	}

	public void clearStatistics(){
		AlphaVector.clearDotProductCount();
		getBeliefStateFactory().clearBeliefUpdateCount();
		getBeliefStateFactory().clearInternalBeliefStateCache();
	}
	
	public boolean countStatistics( boolean bCount ){
		boolean bPrevious = m_bCountStatistics;
		m_bCountStatistics = bCount;
		AlphaVector.countDotProduct( bCount );
		getBeliefStateFactory().countBeliefUpdates( bCount );
		return bPrevious;
	}
	
	public boolean isTerminalState( int iState ){
		if( terminalStatesDefined() ){
			return m_vTerminalStates.contains( new Integer( iState ) );
		}
		return false;
	}
	
	protected double maxReward( int iState ){
		int iAction = 0;
		double dReward;
		double dMaxReward = Double.NEGATIVE_INFINITY;
		
		for (iAction = 0 ; iAction < m_cActions; iAction++){
			dReward = R(iState, iAction);
			if(dReward > dMaxReward)
				dMaxReward = dReward;
		}
		return dMaxReward;
	}

	public boolean terminalStatesDefined(){
		return ( m_vTerminalStates != null );
	}

	public int getStateCount() {
		return m_cStates;
	}

	public int getActionCount() {
		return m_cActions;
	}

	public int getObservationCount() {
		return m_cObservations;
	}

	public double getDiscountFactor() {
		return m_dGamma;
	}
 
	public Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iStartState, int iAction ) {
		return m_fTransition.getNonZeroEntries( iStartState, iAction );
	}

	public Iterator<Entry<Integer,Double>> getNonZeroObservations( int iAction, int iEndState ) {
		return m_fObservation.getNonZeroEntries( iAction, iEndState );
	}

	private void addBackwardTransition( int iStartState, int iAction, int iEndState, double dTr ){
	}
	
	public Collection<Entry<Integer,Double>> getNonZeroBackwardTransitions( int iAction, int iEndState ) {
		if( m_amBackwardTransitions == null ){
			m_amBackwardTransitions = new TreeMap[m_cActions][m_cStates];
		}
		if( m_amBackwardTransitions[iAction][iEndState] == null ){
			Map<Integer,Double> mTr = new TreeMap<Integer, Double>();
			for( int iStartState = 0 ; iStartState < m_cStates ; iStartState++ ){
				double dTr = tr( iStartState, iAction, iEndState );
				if( dTr > 0.0 ){
					mTr.put( iStartState, dTr );
				}
			}
			m_amBackwardTransitions[iAction][iEndState] = mTr;
		}
		return m_amBackwardTransitions[iAction][iEndState].entrySet();
	}

	public double probStartState( int iState ){
		return m_fStartState.valueAt( iState );
	}
	
	public double getMinR(){
		return m_fReward.getMinValue();
	}
	
	public double getMaxR(){
		return m_fReward.getMaxValue();
	}
	
	public double getMaxMinR(){
		int iAction = 0;
		double dMaxR = MIN_INF;
		for( iAction = 0 ; iAction < m_cActions; iAction++ ){
			if( m_adMinActionRewards[iAction] > dMaxR )
				dMaxR = m_adMinActionRewards[iAction];
		}
		return dMaxR;
	}

	public int getStartStateCount() {
		return m_fStartState.countNonZeroEntries();
	}

	public Iterator<Entry<Integer, Double>> getStartStates() {
		return m_fStartState.getNonZeroEntries();
	}

	public void initRandomGenerator( long iSeed ){
		m_rndGenerator.init( iSeed );		
	}

	public void setRandomSeed( long iSeed ){
		m_iRandomSeed = iSeed;		
	}

	public AlphaVector newAlphaVector() {
		return new TabularAlphaVector( null, 0, this );
	}
	public boolean isValid( int iState ){
		return true;
	}
	
	public Collection<Integer> getValidStates(){
		return new IntegerCollection( 0, m_cStates );
	}
	public static class IntegerCollection extends AbstractCollection<Integer> implements Serializable{
		private int m_iFirst, m_iLast;
		
		public IntegerCollection( int iFirst, int iLast ){
			m_iFirst = iFirst;
			m_iLast = iLast;
		}
		public Iterator<Integer> iterator() {
			return new IntegerIterator( m_iFirst, m_iLast );
		}

		public int size() {
			return m_iLast - m_iFirst;
		}
	}
	
	private static class IntegerIterator implements Iterator<Integer>{
		
		private int m_iNext, m_iLast;
		public IntegerIterator( int iFirst, int iLast ){
			m_iNext = iFirst;
			m_iLast = iLast;
		}

		public boolean hasNext() {
			return m_iNext < m_iLast;
		}

		public Integer next() {
			m_iNext++;
			return m_iNext - 1;
		}

		public void remove() {
		}
		
	}

	public boolean isFactored() {
		return false;
	}
	
	public String getName(){
		return m_sName;
	}
	
	/**
	 * Computes the immediate reward for a belief state over all actions
	 * @param bs
	 * @return
	 */
	public double immediateReward( BeliefState bs ){
		int iAction = 0;
		double dReward = 0.0, dMaxReward = Double.MAX_VALUE * -1.0;
		
		if( bs == null )
			return 0.0;
		
		dReward = bs.getActionImmediateReward( iAction );
		if( dReward != MIN_INF )
			return dReward;
		
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			dReward = immediateReward( bs, iAction );
			if( dReward > dMaxReward ){
				dMaxReward = dReward;
			}
		}
		
		bs.setImmediateReward( dMaxReward );
		
		return dMaxReward;
	}

	
	/**
	 * Computes the immediate reward for a belief state and a specific action
	 * @param bs - belief state
	 * @param iAction - action index
	 * @return
	 */
	public double immediateReward( BeliefState bs, int iAction ){
		
		if( bs == null )
			return 0.0;
		
		double dReward = bs.getActionImmediateReward( iAction );
		if( dReward != MIN_INF )
			return dReward;
		
		dReward = computeImmediateReward( bs, iAction );
		
		bs.setActionImmediateReward( iAction, dReward );
		
		return dReward;
	}
	
	protected double computeImmediateReward( BeliefState bs, int iAction ){
		int iState = 0;
		double dReward = 0.0, dPr = 0.0, dValue = 0.0;
		
		for( Entry<Integer, Double> e : bs.getNonZeroEntries() ){
			iState = e.getKey();
			dPr = e.getValue();
			dValue = R( iState, iAction );
			dReward += dPr * dValue;
		}
		return dReward;
	}

	public Vector<Integer> getObservationRelevantStates() {
		return m_vObservationStates;
	}

	public RewardType getRewardType() {
		return m_rtReward;
	}

	public void initBeliefStateFactory() {
		m_bsFactory = new BeliefStateFactory( this, 20 );		
	}
	
	public double R( BeliefState bsCurrent, int iAction, BeliefState bsNext ){
		Iterator<Entry<Integer,Double>> itNonZero = bsCurrent.getNonZeroEntries().iterator(), itNonZeroNext = null;
		Entry<Integer,Double> e = null, eNext = null;
		double dR = 0.0;
		int iState = 0, iNextState = 0;
		double dBelief = 0.0, dNextBelief = 0.0;
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			iState = e.getKey();
			dBelief = e.getValue();
			if( m_rtReward == RewardType.State )
				dR += R( iState ) * dBelief;
			if( m_rtReward == RewardType.StateAction )
				dR += R( iState, iAction ) * dBelief;
			if( m_rtReward == RewardType.StateActionState ){
				itNonZeroNext = bsNext.getNonZeroEntries().iterator();
				while( itNonZeroNext.hasNext() ){
					eNext = itNonZeroNext.next();
					iNextState = eNext.getKey();
					dNextBelief = eNext.getValue();
					dR += dBelief * dNextBelief * R( iState, iAction, iNextState );
				}
			}
		}
		return dR;
	}

	public double R( BeliefState bsCurrent, int iAction ){
		double dR = 0.0;
		int iState = 0;
		double dBelief = 0.0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dBelief = bsCurrent.valueAt( iState );
			dR += dBelief * R( iState, iAction );
		}
		return dR;
	}

	public Collection<Integer> getRelevantActions( BeliefState bs ) {
		return new IntegerCollection( 0, getActionCount() );
	}

	public void addTerminalState( int iTerminalState ) {
		if( m_vTerminalStates == null )
			m_vTerminalStates = new Vector<Integer>();
		m_vTerminalStates.add( iTerminalState );
	}

	public void addObservationSensitiveState(int iObservationState) {
		m_vObservationStates.add( iObservationState );
	}

	public void setStartStateProb(int iStartState, double dValue) {
		m_fStartState.setValue( iStartState, dValue ); 		
	}

	public void setRewardType(RewardType type) {
		m_rtReward = type;
		
	}

	public void setReward(int iStartState, double dValue) {
		m_fReward.setValue( iStartState, dValue );
		
	}

	public void setReward(int iStartState, int iAction, double dValue) {
		m_fReward.setValue( iStartState, iAction, dValue );
		
	}

	public void setReward( int iStartState, int iAction, int iEndState, double dValue ) {
		m_fReward.setValue( iStartState, iAction, iEndState, dValue );
		
	}

	public void setMinimalReward(int iAction, double dValue) {
		if( iAction != -1 ){
			if( dValue < m_adMinActionRewards[iAction] ){
				m_adMinActionRewards[iAction] = dValue;
			}
		}
		if( dValue < m_dMinReward )
			m_dMinReward = dValue;		
	}

	public void initDynamicsFunctions() {
		int[] aDims = new int[3];
		aDims[0] = m_cStates;
		aDims[1] = m_cActions;
		aDims[2] = m_cStates;
		m_fTransition = new SparseTabularFunction( aDims );
		aDims[0] = m_cActions;
		aDims[1] = m_cStates;
		aDims[2] = m_cObservations;
		m_fObservation = new SparseTabularFunction( aDims );
		aDims = new int[1];
		aDims[0] = m_cStates;
		if( m_cStates > g_sMaxTabularSize )
			m_fStartState = new SparseTabularFunction( aDims );
		else
			m_fStartState = new TabularFunction( aDims );
		aDims = new int[3];
		aDims[0] = m_cStates;
		aDims[1] = m_cActions;
		aDims[2] = m_cStates;
		if( m_cStates > g_sMaxTabularSize )
			//m_fReward = new MapFunction( aDims );
			m_fReward = new SparseTabularFunction( aDims );
		else
			m_fReward = new TabularFunction( aDims );
		
		m_adMinActionRewards = new double[m_cActions];
		for( int idx = 0 ; idx < m_cActions ; idx++ ){
			m_adMinActionRewards[idx] = 0;
		}
	}
	
	public RandomGenerator getRandomGenerator(){
		return m_rndGenerator;
	}

	public void setVisualisationUnit(
			VisualisationUnit vVisualisationUnit) {
		// TODO Auto-generated method stub
		
	}
}
