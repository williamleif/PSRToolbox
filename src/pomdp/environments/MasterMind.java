package pomdp.environments; 

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import pomdp.utilities.Logger;


public class MasterMind extends FactoredPOMDP {

	protected int m_cBits;
	protected double m_dObservationNoise;
	
	public String getName(){
		String sName = "MasterMind" + m_cBits + "_" + m_dObservationNoise;
		sName = sName.replace( '.', '_' );
		return sName;
	}
	
	public MasterMind( int cBits, double dObservationNoise ){
		super( cBits, (int) Math.pow( 2, cBits ), cBits + 1, BeliefType.Factored , false, true );
		m_cBits = cBits;
		m_dObservationNoise = dObservationNoise;
		
		initADDs();
		
		double dMdpAdr = computeMDPAverageDiscountedReward( 200, 250 );
		Logger.getInstance().logln( "MDP ADR = " + dMdpAdr );
	}
	
	protected int stateToIndex( boolean[] abState ){
		int iState = 0;
		int iVariable = abState.length - 1, iBit = 0;
		for( iBit = 0 ; iBit < m_cBits ; iBit++ ){
			iState *= 2;
			if( abState[iVariable] )
				iState += 1;
			iVariable--;
		}
		return iState;
	}

	
	protected int stateToIndex( int[] aiStateVariableIndexes, boolean[] abStateVariableValues ){
		int cVariables = getStateVariablesCount();
		int iState = 0;
		int iVariable = cVariables - 1, idx = aiStateVariableIndexes.length - 1;
		for( iVariable = cVariables - 1 ; iVariable >= 0 ; iVariable-- ){
			iState *= 2;
			if( ( idx >= 0 ) && ( aiStateVariableIndexes[idx] == iVariable ) ){
				if( abStateVariableValues[idx] )
					iState += 1;
				idx--;
			}
		}		
		return iState;
	}
	
	public int[] getRelevantVariables( int iAction ){
		int[] aiRelevant = new int[m_cBits];
		int iBit = 0;
		for( iBit = 0 ; iBit < m_cBits ; iBit++ )
			aiRelevant[iBit] = iBit;
		return aiRelevant;
	}

	protected int[] getObservationRelevantVariables( int iAction ){
		int[] aiRelevant = new int[m_cBits];
		int iBit = 0;
		for( iBit = 0 ; iBit < m_cBits ; iBit++ )
			aiRelevant[iBit] = iBit;
		return aiRelevant;
	}

	protected int[] getRewardRelevantVariables( int iAction ){
		int[] aiRelevant = new int[m_cBits];
		int iBit = 0;
		for( iBit = 0 ; iBit < m_cBits ; iBit++ )
			aiRelevant[iBit] = iBit;
		return aiRelevant;
	}

	public boolean[] indexToState( int iState ){
		boolean[] abState = new boolean[getStateVariablesCount()];
		int iBit = 0;
		boolean bNonTerminal = false;
		for( iBit = 0 ; iBit < m_cBits ; iBit++ ){
			abState[iBit] = ( iState % 2 == 1 );
			iState /= 2;
		}
		return abState;
	}
	
	public double tr( int iStartState, int iAction, int iEndState ){
		if( iStartState == iEndState )
			return 1.0;
		else
			return 0.0;
	}

	//probability that a state variable will have the value TRUE if action a is taken at state s
	protected double stateVariableTransition( int iState, int iAction, int iStateVariable ){
		int iOffset = 1 << iStateVariable;
		if( ( iState & iOffset ) == 0 )
			return 0.0;
		return 1.0;
	}

	public double R( int iStartState, int iAction ){
		if( iStartState == iAction )
			return 10.0;
		return 0.0;
	}
	
	public double R( int iStartState, int iAction, int iEndState ){
		return R( iStartState, iAction ); 
	}
	
	public double R( int iStartState ){
		return -1;
	}
		
	private double getCheckSuccessProbability( int iX, int iY, int iRockX, int iRockY ){
		double dDistance = Math.sqrt(  ( iRockX - iX ) * ( iRockX - iX ) + ( iRockY - iY ) * ( iRockY - iY ) );
		double dEfficiency = Math.exp( -dDistance );
		return round( dEfficiency * 0.5 + 0.5, 2 );
	}
	
	public int observe( int iAction, int iEndState ){
		boolean[] abState = indexToState( iEndState );
		boolean[] abAction = indexToState( iAction );
		double dPr = 1.0;
		int iBit = 0;
		double dRand = 0.0;
		int cCorrect = 0;
		
		for( iBit = 0 ; iBit < m_cBits ; iBit++ ){
			dRand = m_rndGenerator.nextDouble();
			if( abState[iBit] == abAction[iBit] ){
				if( dRand > m_dObservationNoise )
					cCorrect++;
			}
			else{
				if( dRand < m_dObservationNoise )
					cCorrect++;
			}
		}
		
		return cCorrect;
	}
	
	public double prCorrect( boolean[] abState, boolean[] abAction, int cSuccesses, int iBit ){
		double dPrTrue = 0.0, dPrFalse = 0.0;
		
		if( iBit == abAction.length )
			return 1.0;
		if( cSuccesses > ( abAction.length - iBit ) )
			return 0.0;
		if( cSuccesses < 0 )
			return 0.0;
		
		if( abState[iBit] == abAction[iBit] ){
			if( cSuccesses > 0 )
				dPrTrue = (1 - m_dObservationNoise ) * prCorrect( abState, abAction, cSuccesses - 1, iBit + 1 );
			if( cSuccesses < ( abAction.length - iBit ) )
				dPrFalse = m_dObservationNoise * prCorrect( abState, abAction, cSuccesses, iBit + 1 );
		}
		else{
			if( cSuccesses < ( abAction.length - iBit ) )
				dPrTrue = ( 1 - m_dObservationNoise ) * prCorrect( abState, abAction, cSuccesses, iBit + 1 );
			if( cSuccesses > 0 )
				dPrFalse = m_dObservationNoise * prCorrect( abState, abAction, cSuccesses - 1, iBit + 1 );
		}
		return dPrFalse + dPrTrue;
	}
	
	public double O( int iAction, int iEndState, int iObservation ){
		boolean[] abState = indexToState( iEndState );
		boolean[] abAction = indexToState( iAction );
		int iBit = 0;
		int cCorrect = 0;
		/*
		for( iBit = 0 ; iBit < m_cBits ; iBit++ ){
			if( abState[iBit] == abAction[iBit] ){
				cCorrect++;
			}
		}
		*/
		return prCorrect( abState, abAction, iObservation, iBit );
	}
	
	public Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iStartState, int iAction ) {
		Map<Integer, Double> mTransitions = new TreeMap<Integer, Double>();
		mTransitions.put( iStartState, 1.0 );
		return mTransitions.entrySet().iterator();
	}

	public Collection<Entry<Integer,Double>> getNonZeroBackwardTransitions( int iAction, int iEndState ) {
		Map<Integer, Double> mTransitions = new TreeMap<Integer, Double>();
		mTransitions.put( iEndState, 1.0 );
		return mTransitions.entrySet();
	}

	//begin at leftmost cloumn in the middle with at least one good rock
	public int chooseStartState(){
		return m_rndGenerator.nextInt( m_cStates );
	}

	public double probStartState( int iState ){
		return 1.0 / m_cStates;
	}
	
	public int execute( int iAction, int iState ){
		return iState;
	}
		
	public boolean isTerminalState( int iState ){
		return false;
	}
	
	public boolean terminalStatesDefined(){
		return false;
	}
	
	public double getMaxMinR(){
		return 0.0;
	}

	public int getStartStateCount() {
		return m_cStates;
	}
	
	public String getStateName( int iState ){
		boolean[] abState = indexToState( iState );
		String sState = "[";
		for( boolean b: abState ){
			if( b )
				sState += "1";
			else
				sState += "0";
		}
		sState += "]";
		return sState;
	}
	
	public String getActionName( int iAction ){
		return getStateName( iAction );
	}

	protected double stateVariableTransition( int[] aiStateVariableIndexes, boolean[] abStateVariableValuesBefore, int iAction, boolean[] abStateVariableValuesAfter ){
		int iBit = 0;
		for( iBit = 0 ; iBit < abStateVariableValuesAfter.length ; iBit++ ){
			if( abStateVariableValuesAfter[iBit] != abStateVariableValuesBefore[iBit] )
				return 0.0;
		}
		return 1.0;
	}	
	protected double stateVariableObservation( int[] aiStateVariableIndexes, int iAction, boolean[] abStateVariableValues, int iObservation ){
		int iEndState = stateToIndex( aiStateVariableIndexes, abStateVariableValues );
		return O( iAction, iEndState, iObservation );
	}
	protected String getObservationName() {
		return "Correct";
	}

	public String getObservationName( int iObservation ) {
		return "" + iObservation;
	}

	protected String getVariableName(int iVariable) {
		return "Bit" + iVariable;
	}

	public double getInitialVariableValueProbability( int iVariable, boolean bValue ){
		return 0.5;
	}

	protected boolean relevantTransitionVariable( int iAction, int iVariable ){
		return true;
	}

	
	private boolean getBit( int iNumber, int iDigit ){
		iNumber = iNumber >> iDigit;
		return iNumber % 2 == 1;
	}
	
	
	public double transitionGivenRelevantVariables( int iAction, int iVariable, boolean bValue, int[] aiRelevantVariables, boolean[] abValues ) {
		int idx = 0;
		for( idx = 0 ; idx < aiRelevantVariables.length ; idx++ ){
			if( aiRelevantVariables[idx] == iVariable ){
				if( abValues[idx] == bValue )
					return 1.0;
			}
		}
		return 0.0;
	}	
	
	public double observationGivenRelevantVariables( int iAction, int iObservation, int[] aiRelevantVariables, boolean[] abValues ) {
		int iState = stateToIndex( abValues );
		return O( iAction, iState, iObservation );
	}	
	
	public double rewardGivenRelevantVariables( int iAction, int[] aiRelevantVariables, boolean[] abValues ) {
		int iState = stateToIndex( abValues );
		if( iState == iAction )
			return 10.0;
		return 0.0;
	}	
	
	
	protected double getInitialVariableValueProbability( int iVariable, int iValue ) {
		return 0.5;
	}

	@Override
	public boolean changingComponent(int component, int action, int observation) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int[] getIndependentComponentVariables(int component) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIndependentComponentsCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getInitialComponentValueProbability(int component, int value) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getRelevantComponents(int action, int observation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getRelevantComponentsForComponent(int action, int component) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getRelevantVariablesForComponent(int action, int component) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double transitionGivenRelevantVariables(int action,
			int[] aiComponent, boolean[] abComponentValues,
			int[] aiRelevantVariables, boolean[] abRelevantValues) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double R(int[][] aiState, int action) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[][] chooseStartState2() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[][] execute(int action, int[][] aiState) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStateName(int[][] aiState) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isTerminalState(int[][] aiState) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int observe(int action, int[][] aiState) {
		// TODO Auto-generated method stub
		return 0;
	}
}
