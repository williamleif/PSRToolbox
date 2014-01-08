package pomdp.environments;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.PolicyStrategy;
import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.sort.BubbleSort;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import pomdp.valuefunction.MDPValueFunction;

public class NetworkManagement extends FactoredPOMDP{
	protected boolean[] m_abWorkingSet;
	protected int m_cMachines;
	protected Map<Integer,Double>[][] m_amTransitions;
	protected Map<Integer,Double>[][] m_amBackwardTransitions;

	
	public NetworkManagement( int cMachines, BeliefType btFactored, boolean bSpecialADDForG, boolean bUseRelevantVariablesOnly ){
		super( cMachines, cMachines * 2 + 1, 2, btFactored, bSpecialADDForG, bUseRelevantVariablesOnly );
		m_cMachines = cMachines;
		m_abWorkingSet = new boolean[m_cMachines];
		m_cActions = m_cMachines * 2 + 1; //ping, reset, do nothing
		m_cObservations = 2; //working, not-working
		m_cStates = (int) Math.pow( 2, m_cMachines );
		m_dGamma = 0.9;
		//NetworkBeliefStateFactory.getInstance( this, 20 );
		//BeliefStateFactory.getInstance( this, 20 );
		m_amTransitions = new Map[m_cStates][m_cActions];
		m_amBackwardTransitions = new Map[m_cActions][m_cStates];

		initADDs();
		
		double dMdpAdr = computeMDPAverageDiscountedReward( 200, 250 );
		Logger.getInstance().logln( "MDP ADR = " + dMdpAdr );
	}

	public NetworkManagement(int cMachines) {
		this( cMachines, BeliefType.Factored, false, true );
	}

	public NetworkManagement(int cMachines, BeliefType bt) {
		this( cMachines, bt, false, true );
	}

	protected int stateToIndex( boolean[] abWorkingSet ){
		int iMachine = 0, iState = 0;
		for( iMachine = m_cMachines - 1 ; iMachine >= 0 ; iMachine-- ){
			iState *= 2;
			if( abWorkingSet[iMachine] )
				iState += 1;
		}
		return iState;
	}

	protected int stateToIndex( int[] aiVariables, boolean[] abValues ){
		int iVariable = 0, iState = 0, iValue = 0;
		for( iVariable = 0 ; iVariable < aiVariables.length ; iVariable++ ){			
			if( abValues[iVariable] ){
				iValue = 1 << aiVariables[iVariable];
				iState += iValue;
			}
		}
		return iState;
	}

	public boolean[] indexToState( int iState ){
		boolean[] abWorkingSet = new boolean[m_cMachines];
		int iMachine = 0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			abWorkingSet[iMachine] = ( iState % 2 == 1 );
			iState /= 2;
		}
		return abWorkingSet;
	}
	
	private int mod( int i, int m ){
		if( i >= 0 )
			return i % m;
		else
			return i % m + m;
	}
	
	public int[] getNeighbors( int iMachine ){
		return new int[]{ mod( iMachine - 1, m_cMachines ), mod( iMachine + 1, m_cMachines ) };
	}
	
	public double tr( int iStartState, int iAction, int iEndState ){
		double dPr = 1.0;
		int iMachine = iAction;
		if( iAction < m_cMachines ){ //reboot machine iAction
			if( !isWorking( iEndState, iMachine ) )
				return 0.0;
		}
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( iMachine != iAction ){// a mchine that wasn't rebooted
				if( !isWorking( iStartState, iMachine ) ){ // machine that wasn't working and not rebooted must stay down
					if( isWorking( iEndState, iMachine ) )//a mchine must reboot to start working
						return 0.0;
				}
				else{  //machine was working
					if( isWorking( iEndState, iMachine ) ){ //machine continues to work
						if( isNeighborDown( iMachine, iStartState ) )
							dPr *= 0.85;//0.67;
						else
							dPr *= 0.95;
					}
					else{ //machine has failed
						if( isNeighborDown( iMachine, iStartState ) )
							dPr *= 0.15;//0.33;
						else
							dPr *= 0.05;
					}
				}
			}
		}
		
		return dPr;
	}
	
	protected double stateVariableTransition( int iState, int iAction, int iMachine ){
		if( iAction == iMachine ) //reboot for current machine
			return 1.0;
		else if( !isWorking( iState, iMachine ) ) //otherwise machine cannot be working now
			return 0.0;
		else if( isNeighborDown( iMachine, iState ) )
			return 0.67;
		return 0.95;
	}

	protected boolean isNeighborDown( int iMachine, int iState ){
		int[] aiNeighbors = getNeighbors( iMachine );
		int iNeighbor = 0;
		for( iNeighbor = 0 ; iNeighbor < aiNeighbors.length ; iNeighbor++ )
			if( !isWorking( iState, aiNeighbors[iNeighbor] ) )
				return true;
		return false;
	}
	
	protected boolean isNeighborDown( int iMachine, int[] aiStateVariables, boolean[] abValues ){
		int[] aiNeighbors = getNeighbors( iMachine );
		int iNeighbor = 0;
		for( iNeighbor = 0 ; iNeighbor < aiNeighbors.length ; iNeighbor++ )
			if( !isWorking( aiStateVariables, abValues, aiNeighbors[iNeighbor] ) )
				return true;
		return false;
	}
	
	protected boolean isNeighborDown( int iMachine, boolean[] abMachines ){
		int[] aiNeighbors = getNeighbors( iMachine );
		int iNeighbor = 0, i = 0;
		for( i = 0 ; i < aiNeighbors.length ; i++ ){
			iNeighbor = aiNeighbors[i];
			if( !abMachines[iNeighbor] )
				return true;
		}
		return false;
	}
	public double R( int iStartState, int iAction, int iEndState ){
		double dReward = computeReward( iEndState );
		if( iAction < m_cActions - 1 ){
			if( iAction < m_cMachines ) //reboot
				dReward -= 1.0;
			else //ping
				dReward -= 0.1;
		}	
		return dReward;
	}
	
	public double R( int iStartState, int iAction ){
		int iEndState = 0;
		double dReward = 0.0, dSumReward = 0.0;
		double dTr = 0.0;
		
		if( m_adStoredRewards != null ){
			dReward = m_adStoredRewards[iStartState][iAction];
			if( dReward != MIN_INF ){
				return dReward;
			}
		}
		
		for( iEndState = 0 ; iEndState < m_cStates ; iEndState++ ){
			dReward = R( iStartState, iAction, iEndState );
			dTr = tr( iStartState, iAction, iEndState );
			dSumReward += dReward * dTr;
		}
		
		if( m_adStoredRewards != null ){
			m_adStoredRewards[iStartState][iAction] = dSumReward;
		}
		
		return dSumReward;
	}
	
	public double R( int iStartState ){
		return -1;
	}
	
	protected boolean isWorking( int iState, int iMachine ){
		if( ( iState >> iMachine ) % 2 == 1 )
			return true;
		return false;
	}
	
	protected boolean isWorking( int[] aiStateVariables, boolean[] abValues, int iMachine ){
		int i = 0;
		for( i = 0 ; i < aiStateVariables.length ; i++ ){
			if( aiStateVariables[i] == iMachine )
				return abValues[i];
		}
		return false;
	}
	
	protected double computeReward( int[] aiRelevantVariables, boolean[] abValues ){
		int iMachine = 0;
		double dReward = 0.0;
		
		if( isWorking( aiRelevantVariables, abValues, 0 ) ) // machine 0 is the server
			dReward = 2;
		for( iMachine = 1 ; iMachine < m_cMachines ; iMachine++ ){
			if( isWorking( aiRelevantVariables, abValues, iMachine ) )
				dReward += 1;
		}
		
		return dReward;
	}
	
	protected double computeReward( int iState ){
		int iMachine = 0;
		double dReward = 0.0;
		
		if( isWorking( iState, 0 ) ) // machine 0 is the server
			dReward = 1;//should be 2
		for( iMachine = 1 ; iMachine < m_cMachines ; iMachine++ ){
			if( isWorking( iState, iMachine ) )
				dReward += 1;
		}
		
		return dReward;
	}
	
	protected int observe( int iObservation, double dProbability ){
		if( m_rndGenerator.nextDouble() < dProbability )
			return iObservation;
		else
			return 1 - iObservation;
	}
	
	public int observe( int iAction, int iEndState ){
		if( iAction == m_cActions - 1 ){
			return 0;
		}
		else if( iAction < m_cMachines ){//reboot
			return 1;
		}
		else{//ping
			int iMachine = iAction % m_cMachines;
			boolean bMachineWorking = isWorking( iEndState, iMachine );
			int iObservation = 1;
			if( !bMachineWorking )
				iObservation = 0;
			return observe( iObservation, 0.95 );
		}
	}
	
	
	public double O( int iAction, int iEndState, int iObservation ){
		if( iAction == m_cActions - 1 ){ //do nothing returns uninformative message
			//return 0.5;
			if( iObservation == 0 )
				return 1.0;
			else
				return 0.0;
		}
		else if( iAction < m_cMachines ){//reboot
			if( iObservation == 1 )
				return 1.0;
			else
				return 0.0;
		}
		else{
			int iMachine = iAction % m_cMachines;
			boolean bMachineWorking = isWorking( iEndState, iMachine );
			boolean bObservation = ( iObservation == 1 );
			if( bMachineWorking == bObservation )
				return 0.95;
			return 0.05;				
		}
	}
	
	public Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iStartState, int iAction ) {
		Map<Integer,Double> mTransitions = m_amTransitions[iStartState][iAction];
		int iEndState = 0;
		double dTr = 0.0;
		double dSumTr = 0.0;
		if( mTransitions == null ){
			mTransitions = new TreeMap<Integer,Double>();
			for( iEndState = 0 ; iEndState < m_cStates ; iEndState++ ){
				dTr = tr( iStartState, iAction, iEndState );
				if( dTr > 0 ){
					mTransitions.put( iEndState, dTr );
					dSumTr += dTr;
				}
			}
			m_amTransitions[iStartState][iAction] = mTransitions;
			if( Math.abs( dSumTr - 1.0 ) > 0.000001 )
				Logger.getInstance().logln( "getTransitionNonZeroEntries: BUGBUG - sum tr( " + iStartState + ", " + iAction + ", * ) = " + dSumTr );
		}
		return mTransitions.entrySet().iterator();
	}

	public Iterator<Entry<Integer,Double>> getBackwardTransitionNonZeroEntries( int iAction, int iEndState ) {
		Map<Integer,Double> mTransitions = m_amBackwardTransitions[iAction][iEndState];
		int iStartState = 0;
		double dTr = 0.0;
		double dSumTr = 0.0;
		if( mTransitions == null ){
			mTransitions = new TreeMap<Integer,Double>();
			for( iStartState = 0 ; iStartState < m_cStates ; iStartState++ ){
				dTr = tr( iStartState, iAction, iEndState );
				if( dTr > 0 ){
					mTransitions.put( iStartState, dTr );
					dSumTr += dTr;
				}
			}
			m_amBackwardTransitions[iAction][iEndState] = mTransitions;
		}
		return mTransitions.entrySet().iterator();
	}

	public int chooseStartState(){
		boolean[] abStartState = new boolean[m_cMachines];
		int iMachine = 0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( m_rndGenerator.nextDouble() < probMachineWorkingAtStart( iMachine ) )
				abStartState[iMachine] = true;
			else
				abStartState[iMachine] = false;
		}		
		return stateToIndex( abStartState );
	}

	public double probMachineWorkingAtStart( int iMachine ){
		return 0.9;
	}
	
	public double probStartState( int iState ){
		int iMachine = 0;
		double dProb = 1.0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( isWorking(iState, iMachine ) ){
				dProb *= probMachineWorkingAtStart( iMachine );
			}
			else{
				dProb *= 1 - probMachineWorkingAtStart( iMachine );
			}
		}
		return dProb;
	}
	
	public String toString( boolean[] abState ){
		int iMachine = 0;
		String sResult = "[";
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( abState[iMachine] )
				sResult += "1";
			else
				sResult += "0";
			sResult += ",";
		}
		sResult += "]";
		return sResult;
	}
	
	public int execute( int iAction, int iState ){
		boolean[] abMachines = indexToState( iState );
		boolean[] abNewState = new boolean[m_cMachines];
		int iMachine = 0, iRestartMachine = iAction < m_cMachines ? iAction : -1;
		double dRand = 0.0, dMinProb = 0.0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( iRestartMachine == iMachine )
				abNewState[iMachine] = true;
			else{
				if( abMachines[iMachine] ){
					dRand = m_rndGenerator.nextDouble();
					if( isNeighborDown( iMachine, abMachines ) ){
						dMinProb = 0.67;
					}
					else{
						dMinProb = 0.95;
					}
					if( dRand >= dMinProb )
						abNewState[iMachine] = false;
					else
						abNewState[iMachine] = true;						
				}
				else{
					abNewState[iMachine] = false;
				}
			}
		}
		return stateToIndex( abNewState );
	}
	
	public boolean isTerminalState( int iState ){
		return false;
	}
	
	public boolean terminalStatesDefined(){
		return true;
	}
	
	public double getMaxMinR(){
		return 0.0;
	}

	public int getStartStateCount() {
		return -1;
	}

	public int getMachineCount() {
		return m_cMachines;
	}
	
	public String getStateName( int iState ){
		boolean[] abWorkingSet = indexToState( iState );
		String sState = "[";
		for( boolean bWorking: abWorkingSet ){
			if( bWorking )
				sState += "1";
			else
				sState += "0";
		}
		sState += "]";
		return sState;
	}
	
	public String getActionName( int iAction ){
		if( iAction == m_cActions - 1 )
			return "no_op";
		if( iAction < m_cMachines )
			return "restart_" + iAction;
		return "ping_" + iAction % m_cMachines;
	}
	
	public boolean endADR( int iState, double dReward ){
		return false;
	}
	
	public String getName(){
		return "NetworkManagement" + m_cMachines;
	}
	
	
	private class DoNothing extends PolicyStrategy{
		private int m_cMachines;		
		public DoNothing( int cMachines ){
			m_cMachines = cMachines;
		}		
		public int getAction( BeliefState bsCurrent ){
			return m_cMachines * 2;
		}
		public double getValue(BeliefState bsCurrent) {
			return 0;
		}
		public boolean hasConverged() {
			return false;
		}
		public String getStatus() {
			return null;
		}
		public LinearValueFunctionApproximation getValueFunction() {
			return null;
		}
	}
	private class Restart extends PolicyStrategy{
		private int m_cMachines;
		private int m_iMachine;
		public Restart( int cMachines ){
			m_cMachines = cMachines;
			m_iMachine = 0;
		}		
		public int getAction( BeliefState bsCurrent ){
			m_iMachine = ( m_iMachine + 1 ) % m_cMachines;
			return m_iMachine;
		}
		public double getValue(BeliefState bsCurrent) {
			return 0;
		}
		public boolean hasConverged() {
			return false;
		}
		public String getStatus() {
			return null;
		}
		public LinearValueFunctionApproximation getValueFunction() {
			return null;
		}
	}

	public int[] getRelevantVariables( int iAction ){
		int[] aiMachines = new int[m_cMachines];
		int i = 0;
		for( i = 0 ; i < m_cMachines ; i++ )
			aiMachines[i] = i;
		return aiMachines;
	}

	public int[] getRelevantVariables( int iAction, int iVariable ){
		if( iAction == iVariable ){//reboot for machine i - machine is always 1 afterwards
			int[] aiMachines = new int[0];
			return aiMachines;
		}
		else{//other operations - restart for other machines, ping and no-op, machines may crash based on its neighbors
			int[] aiNeighbors = getNeighbors( iVariable );
			int[] aiMachines = new int[aiNeighbors.length + 1];
			int i = 0;
			for( i = 0 ; i < aiNeighbors.length ; i++ ){
				aiMachines[i] = aiNeighbors[i];
			}
			aiMachines[i] = iVariable;
			return aiMachines;
		}
	}


	protected double stateVariableObservation( int[] aiStateVariableIndexes, int iAction, boolean[] abStateVariableValues, int iObservation ){
		if( iAction == m_cActions - 1 ){
			if( iObservation == 0 ) 
				return 1.0;
			return 0.0;
		}
		else if( iAction < m_cMachines ){//reboot
			if( iObservation == 1 ) 
				return 1.0;
			return 0.0;
		}
		else{//ping
			int iMachine = iAction % m_cMachines;
			boolean bMachineWorking = isWorking( aiStateVariableIndexes, abStateVariableValues, iMachine );
			if( bMachineWorking && ( iObservation == 1 ) )
				return 0.95;
			if( !bMachineWorking && ( iObservation == 0 ) )
				return 0.95;
			return 0.05;
		}
	}


	protected double stateVariableTransition( int[] aiStateVariableIndexes, boolean[] abStateVariableValuesBefore, int iAction, boolean[] abStateVariableValuesAfter ) {
		double dPr = 1.0;
		int iMachine = iAction;
		if( iAction < m_cMachines ){ //reboot machine iAction
			if( !isWorking( aiStateVariableIndexes, abStateVariableValuesAfter, iMachine ) )
				return 0.0;
		}
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( iMachine != iAction ){// a mchine that wasn't rebooted
				if( !isWorking( aiStateVariableIndexes, abStateVariableValuesBefore, iMachine ) ){ // machine that wasn't working and not rebooted must stay down
					if( isWorking( aiStateVariableIndexes, abStateVariableValuesAfter, iMachine ) )//a mchine must reboot to start working
						return 0.0;
				}
				else{  //machine was working
					if( isWorking( aiStateVariableIndexes, abStateVariableValuesAfter, iMachine ) ){ //machine continues to work
						if( isNeighborDown( iMachine, aiStateVariableIndexes, abStateVariableValuesBefore ) )
							dPr *= 0.67;
						else
							dPr *= 0.95;
					}
					else{ //machine has failed
						if( isNeighborDown( iMachine, aiStateVariableIndexes, abStateVariableValuesBefore ) )
							dPr *= 0.33;
						else
							dPr *= 0.05;
					}
				}
			}
		}
		
		return dPr;
	}

	public double getInitialVariableValueProbability( int iVariable, boolean bValue ) {
		if( bValue )
			return probMachineWorkingAtStart( iVariable );
		return 1 - probMachineWorkingAtStart( iVariable );
	}
	
	
	public double getInitialComponentValueProbability( int iComponent, int iValue ){
		return getInitialVariableValueProbability( iComponent, iValue != 0 );
	}

	
	public String getObservationName( int iObservation ){
		if( iObservation == 1 )
			return "Working";
		return "Down";
	}

	protected String getObservationName() {
		return "MachineState";
	}

	protected int[] getObservationRelevantVariables( int iAction ){
		if( iAction == m_cActions - 1 ){
			return new int[0];
		}
		else if( iAction < m_cMachines ){//reboot
			return new int[0];
		}
		else{//ping
			int iMachine = iAction % m_cMachines;
			int[] aiMachine = { iMachine };
			return aiMachine;
		}
	}

	protected int[] getRewardRelevantVariables( int iAction ){
		int[] aiMachines = new int[m_cMachines];
		int i = 0;
		for( i = 0 ; i < m_cMachines ; i++ )
			aiMachines[i] = i;
		return aiMachines;
	}

	protected String getVariableName( int iVariable ){
		return "Machine" + iVariable;
	}

	public double observationGivenRelevantVariables( int iAction, int iObservation, int[] aiRelevantVariables, boolean[] abValues ) {
		if( iAction == m_cActions - 1 ){ //no-op
			if( iObservation == 0 ) 
				return 1.0;
			return 0.0;
		}
		else if( iAction < m_cMachines ){//reboot
			if( iObservation == 1 ) 
				return 1.0;
			return 0.0;
		}
		else{//ping
			int iMachine = iAction % m_cMachines;
			boolean bMachineWorking = isWorking( aiRelevantVariables, abValues, iMachine );
			if( bMachineWorking && ( iObservation == 1 ) )
				return 0.95;
			if( !bMachineWorking && ( iObservation == 0 ) )
				return 0.95;
			return 0.05;
		}
	}

	protected boolean relevantTransitionVariable( int iAction, int iVariable ) {
		return true; //all variables are relevant since all machines can fail after each action
	}

	
	public double rewardGivenRelevantVariables( int iAction, int[] aiRelevantVariables, boolean[] abValues ) {
		double dReward = computeReward( aiRelevantVariables, abValues );
		if( iAction < m_cActions - 1 ){
			if( iAction < m_cMachines ) //reboot
				dReward -= 2.5;
			else //ping
				dReward -= 0.1;
		}	
		return dReward;
	}

	
	public double transitionGivenRelevantVariables( int iAction, int iVariable, boolean bValue, int[] aiRelevantVariables, boolean[] abValues ) {
		int iMachine = iVariable;
		if( iMachine == iAction ){ //reboot machine iAction
			if( bValue )
				return 1.0;
			else
				return 0.0;
		}
		else{
			if( !isWorking( aiRelevantVariables, abValues, iMachine ) ){
				if( bValue )
					return 0.0; // machine starts working without restart
				else
					return 1.0;
			}
			else{ //machine was working
				if( bValue ){ //machine still works
					if( isNeighborDown( iMachine, aiRelevantVariables, abValues ) ){
						return 0.67;
					}
					else{ 
						return 0.95;
					}
				}
				else{ // machine failed
					if( isNeighborDown( iMachine, aiRelevantVariables, abValues ) ){
						return 0.33;
					}
					else{ 
						return 0.05;
					}
				}
				
			}
		}
	}

	
	protected double getInitialVariableValueProbability(int iVariable, int iValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	protected int[] getObservationRelevantVariablesMultiValue(int iAction) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected int getRealStateVariableCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	protected String getRealVariableName(int iVariable) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected int[] getRelevantVariablesMultiValue(int iAction, int iVariable) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected int[] getRewardRelevantVariablesMultiValue(int iAction) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected int getValueCount(int iVariable) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	protected String getValueName(int iVariable, int iValue) {
		// TODO Auto-generated method stub
		return null;
	}

	
	protected double observationGivenRelevantVariablesMultiValue(int iAction, int iObservation, int[] aiRelevantVariables, int[] aiValues) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	protected boolean relevantTransitionRealVariable(int iAction, int iVariable) {
		// TODO Auto-generated method stub
		return false;
	}

	
	protected double rewardGivenRelevantVariablesMultiValue(int iAction, int[] aiRelevantVariables, int[] aiValues) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	protected double transitionGivenRelevantVariablesMultiValue(int iAction, int iVariable, int iValue, int[] aiRelevantVariables, int[] atValues) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	public int[] getIndependentComponentVariables( int iComponent ) {
		if( iComponent >= 0 && iComponent < m_cMachines ){
			return new int[]{ iComponent };
		}
		return null;
	}

	
	public int getIndependentComponentsCount() {
		return m_cMachines;
	}

	
	public double transitionGivenRelevantVariables( int iAction, int[] aiComponent, boolean[] abComponentValues, int[] aiRelevantVariables, boolean[] abRelevantValues ){
		if( aiComponent.length == 1 ){
			return transitionGivenRelevantVariables( iAction, aiComponent[0], abComponentValues[0], aiRelevantVariables, abRelevantValues );
		}
		return 0.0;
	}

	
	public boolean changingComponent( int iComponent, int iAction, int iObservation ){
		return true;
	}

	
	public int[] getRelevantComponents( int iAction, int iObservation ) {
		if( ( iAction < m_cMachines ) || ( iAction == m_cActions - 1 ) ) //restart and no-op
			return new int[0];
		return new int[]{ iAction - m_cMachines };
	}

	
	public int[] getRelevantVariablesForComponent( int iAction, int iComponent ){
		int[] aiNeighbors = getNeighbors( iComponent );
		int[] aiRelevant = new int[aiNeighbors.length + 1];
		int i = 0;
		for( i = 0 ; i < aiNeighbors.length ; i++ ){
			aiRelevant[i] = aiNeighbors[i];
		}
		aiRelevant[i] = iComponent;
		BubbleSort bs = new BubbleSort();
		bs.sort( aiRelevant );
		return aiRelevant;
	}

	
	public int[] getRelevantComponentsForComponent(int iAction, int iComponent) {
		return getRelevantVariablesForComponent( iAction, iComponent );
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
