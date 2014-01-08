package pomdp.environments;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.utilities.factored.LogisticsBeliefState;
import pomdp.utilities.factored.LogisticsBeliefStateFactory;
import pomdp.valuefunction.MDPValueFunction;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class Logistics extends FactoredPOMDP {

	private int m_cCities, m_cTrucks, m_cPackages;
	private int m_cBitsPerPackage, m_cBitsPerTruck;
	
	public Logistics( int cCities, int cTrucks, int cPackages, BeliefType bFactored, boolean bUseSpecialADDForG, boolean bUseRelevantVariablesOnly ){
		super( cPackages * ( log( cTrucks + cCities ) ) /*package location*/ + cTrucks * log( cCities ) /*truck location*/, 
				cPackages * cTrucks /*load/unload*/ + cTrucks * cCities/*move truck to city*/ + cTrucks /*ping truck*/ + cPackages /*ping package*/ , 
				cCities + cTrucks, bFactored, bUseSpecialADDForG, bUseRelevantVariablesOnly );
		m_cCities = cCities;
		m_cTrucks = cTrucks;
		m_cPackages = cPackages;
		m_cBitsPerPackage = log( cTrucks + cCities );
		m_cBitsPerTruck = log( cCities );
		m_dGamma = 0.9995;

		m_bsFactory = new LogisticsBeliefStateFactory( this );
		
		initADDs();
		
		//double dMdpAdr = computeMDPAverageDiscountedReward( 200, 250 );
		//Logger.getInstance().logln( "MDP ADR = " + dMdpAdr );
	}	

	public Logistics( int cCities, int cTrucks, int cPackages, BeliefType bFactored ){
		this( cCities, cTrucks, cPackages, bFactored, false, true );
	}	

	public String getName(){
		return "PackageWorld" + m_cCities + "_" + m_cTrucks + "_" + m_cPackages;
	}
	public boolean isLoadUnloadAction( int iAction ){
		return iAction < m_cPackages * m_cTrucks;
	}
	public boolean isDriveAction( int iAction ){
		return iAction >= m_cPackages * m_cTrucks && iAction < m_cPackages * m_cTrucks + m_cTrucks * m_cCities;
	}
	public boolean isPingAction( int iAction ){
		return iAction >= m_cPackages * m_cTrucks + m_cTrucks * m_cCities &&
			iAction < m_cActions;
	}
	public boolean isPingTruck( int iAction ){
		return iAction >= m_cPackages * m_cTrucks + m_cTrucks * m_cCities && 
			iAction < m_cPackages * m_cTrucks + m_cTrucks * m_cCities + m_cTrucks;
	}
	public boolean isPingPackage( int iAction ){
		return iAction >= m_cPackages * m_cTrucks + m_cTrucks * m_cCities + m_cTrucks &&
			iAction < m_cActions;
	}
	public int getActivePackage( int iAction ){
		if( isLoadUnloadAction( iAction ) )
			return iAction / ( m_cTrucks );
		if( isPingPackage( iAction ) ){
			return iAction - ( m_cPackages * m_cTrucks + m_cTrucks * m_cCities + m_cTrucks );
		}
		return -1;
	}
	public int getActiveTruck( int iAction ){
		if( isLoadUnloadAction( iAction ) ){
			return iAction % m_cTrucks;
		}
		else if( isPingTruck( iAction ) ){
			return iAction - ( m_cPackages * m_cTrucks + m_cTrucks * m_cCities );
		}
		else if( isDriveAction( iAction ) ){
			return ( iAction - m_cPackages * m_cTrucks ) / m_cCities;
		}
		return -1;
	}
	public int getTruckDestination( int iAction ){
		if( isDriveAction( iAction ) ){
			return ( iAction - m_cPackages * m_cTrucks ) % m_cCities;
		}	
		return -1;
	}
	public int getFirstBitForPackage( int iPackage ){
		return iPackage * m_cBitsPerPackage;
	}
	public int getFirstBitForTruck( int iTruck ){
		return m_cPackages * m_cBitsPerPackage + iTruck * m_cBitsPerTruck;
	}
	public int getPackageLocation( int iState, int iPackage ){
		int iBit = 0, iLastBit = getFirstBitForPackage( iPackage + 1 ), iFirstBit = getFirstBitForPackage( iPackage );
		int iLocation = 0, iOffset = 1;
		
		iState = iState >> iFirstBit;
		
		for( iBit = iFirstBit ; iBit < iLastBit ; iBit++ ){
			iLocation += ( iState % 2 ) * iOffset;
			iOffset *= 2;
			iState /= 2;
		}
		
		return iLocation;
	}

	public Collection<Integer> getPackageLocation( Vector<Pair<Integer, Boolean>> vAssignment, int iPackage ){
		int iLastBit = getFirstBitForPackage( iPackage + 1 ), iFirstBit = getFirstBitForPackage( iPackage );
		return getLocations( iFirstBit, iLastBit, vAssignment );
	}

	private int getPackageLocation( int[] aiVariables, boolean[] abValues, int iPackage ){
		int iBit = 0, iLastBit = getFirstBitForPackage( iPackage + 1 ), iFirstBit = getFirstBitForPackage( iPackage );
		int iLocation = 0, iOffset = 1, iVariable = 0;
				
		for( iVariable = 0 ; iVariable < aiVariables.length && aiVariables[iVariable] != iFirstBit ; iVariable++ );
		
		if( iVariable < aiVariables.length ){
			for( iBit = 0 ; iBit < m_cBitsPerPackage ; iBit++ ){
				if( abValues[iVariable + iBit] )
					iLocation += 1 * iOffset;
				iOffset *= 2;
			}
		}
		return iLocation;
	}

	public int getTruckLocation( int iState, int iTruck ){
		int iBit = 0, iLastBit = getFirstBitForTruck( iTruck + 1 ), iFirstBit = getFirstBitForTruck( iTruck );
		int iLocation = 0, iOffset = 1;
		
		iState = iState >> iFirstBit;
		
		for( iBit = iFirstBit ; iBit < iLastBit ; iBit++ ){
			iLocation += ( iState % 2 ) * iOffset;
			iOffset *= 2;
			iState /= 2;
		}
		
		return iLocation;
	}
	
	private int getValue( Vector<Pair<Integer, Boolean>> vAssignment, int iVariable ){
		for( Pair<Integer, Boolean> p : vAssignment ){
			if( p.getKey() == iVariable ){
				if( p.getValue() )
					return 1;
				else
					return 0;
			}
		}
		return -1;
	}
	
	private void getPossibleStates( boolean[][] abVariables, int iVar, int iOffset, int iCurrentValue, Vector<Integer> vValues ){
		if( iVar == abVariables.length ){
			vValues.add( iCurrentValue );
		}
		else{
			int iValue = 0;
			for( boolean bVal : abVariables[iVar] ){
				if( bVal )
					iValue = 1;
				else
					iValue = 0;
				getPossibleStates( abVariables, iVar + 1, iOffset * 2, iCurrentValue + iOffset * iValue, vValues );
			}
		}
	}
	
	public Collection<Integer> getTruckLocation( Vector<Pair<Integer, Boolean>> vAssignment, int iTruck ){
		int iLastBit = getFirstBitForTruck( iTruck + 1 ), iFirstBit = getFirstBitForTruck( iTruck );
		return getLocations( iFirstBit, iLastBit, vAssignment );
	}

	private Collection<Integer> getLocations( int iFirstBit, int iLastBit, Vector<Pair<Integer, Boolean>> vAssignment ){
		int iValue = 0, iBit = 0;
		boolean bSpecificValues = false;
		boolean[][] abVariables = new boolean[iLastBit - iFirstBit][];
		
		for( iBit = iFirstBit ; iBit < iLastBit ; iBit++ ){
			iValue = getValue( vAssignment, iBit );
			if( iValue == -1 ){
				abVariables[iBit - iFirstBit] = new boolean[]{ false, true };
			}
			else{
				bSpecificValues = true;
				abVariables[iBit - iFirstBit] = new boolean[]{ iValue == 1 };
			}
		}
		Vector<Integer> vVariables = new Vector<Integer>();
		if( bSpecificValues )
			getPossibleStates( abVariables, 0, 1, 0, vVariables );
		return vVariables;
	}
	
	private int getTruckLocation( int[] aiVariables, boolean[] abValues, int iTruck ){
		int iBit = 0, iLastBit = getFirstBitForTruck( iTruck + 1 ), iFirstBit = getFirstBitForTruck( iTruck );
		int iLocation = 0, iOffset = 1, iVariable = 0;
		
		
		for( iVariable = 0 ; iVariable < aiVariables.length && aiVariables[iVariable] != iFirstBit ; iVariable++ );

		if( iVariable < aiVariables.length ){
			for( iBit = 0 ; iBit < m_cBitsPerTruck ; iBit++ ){
				if( abValues[iVariable + iBit] )
					iLocation += 1 * iOffset;
				iOffset *= 2;
			}
		}
		return iLocation;
	}

	private int getTruckLocation( int[] aiVariables, int[] aiValues, int iTruck ){
		for( int iVariable = 0 ; iVariable < aiVariables.length ; iVariable++ ){
			if( aiVariables[iVariable] == iTruck )
				return aiValues[iVariable];
		}
		
		return -1;
	}

	private int getPackageLocation( int[] aiVariables, int[] aiValues, int iPackage ){
		for( int iVariable = 0 ; iVariable < aiVariables.length ; iVariable++ ){
			if( aiVariables[iVariable] == m_cTrucks + iPackage )
				return aiValues[iVariable];
		}
		
		return -1;
	}

	private boolean[] setPackageLocation( int[] aiVariables, boolean[] abValues, int iPackage, int iPackageLocation ){
		boolean[] abRevisedValues = new boolean[abValues.length];
		int iLastBit = getFirstBitForPackage( iPackage + 1 ), iFirstBit = getFirstBitForPackage( iPackage );
		int i = 0;
		for( i = 0 ; i < abValues.length ; i++ ){
			if( aiVariables[i] < iFirstBit || aiVariables[i] >= iLastBit ){
				abRevisedValues[i] = abValues[i];
			}
			else{
				abRevisedValues[i] = ( iPackageLocation % 2 == 1 );
				iPackageLocation /= 2;
			}
		}
		return abRevisedValues;
	}

	private int setPackageLocation( int iState, int iPackage, int iPackageLocation ){
		int iLastBit = getFirstBitForPackage( iPackage + 1 ), iFirstBit = getFirstBitForPackage( iPackage );
		int iRight = iState % ( 1 << iFirstBit );
		iState = iState >> iLastBit;
		iState = iState << m_cBitsPerPackage;
		iState += iPackageLocation;
		iState = iState << iFirstBit;
		iState += iRight;
		return iState;
	}

	private int setTruckLocation( int iState, int iTruck, int iTruckLocation ){
		int iLastBit = getFirstBitForTruck( iTruck + 1 ), iFirstBit = getFirstBitForTruck( iTruck );
		int iRight = iState % ( 1 << iFirstBit );
		iState = iState >> iLastBit;
		iState = iState << m_cBitsPerTruck;
		iState += iTruckLocation;
		iState = iState << iFirstBit;
		iState += iRight;
		return iState;
	}

	private boolean[] setTruckLocation( int[] aiVariables, boolean[] abValues, int iTruck, int iTruckLocation ){
		boolean[] abRevisedValues = new boolean[abValues.length];
		int iLastBit = getFirstBitForTruck( iTruck + 1 ), iFirstBit = getFirstBitForTruck( iTruck );
		int i = 0;
		for( i = 0 ; i < abValues.length ; i++ ){
			if( aiVariables[i] < iFirstBit || aiVariables[i] >= iLastBit ){
				abRevisedValues[i] = abValues[i];
			}
			else{
				abRevisedValues[i] = ( iTruckLocation % 2 == 1 );
				iTruckLocation /= 2;
			}
		}
		return abRevisedValues;
	}

	
	public boolean isTerminalLocation( int iPackage, int iLocation ){
		if( iPackage % m_cCities == iLocation )
			return true;
		return false;
	}
	public int getTerminalLocation( int iPackage ){
		return iPackage % m_cCities;
	}

	private boolean isOnTruck( int iState, int iPackage, int iTruck ){
		int iLocation = getPackageLocation( iState, iPackage );
		if( iLocation - m_cCities == iTruck )
			return true;
		return false;
	}


	private boolean isOnTruck( int[] aiStateVariables, boolean[] abValues, int iPackage, int iTruck ){
		int iLocation = getPackageLocation( aiStateVariables, abValues, iPackage );
		if( iLocation - m_cCities == iTruck )
			return true;
		return false;
	}

	
	private int loadPackage( int iState, int iPackage, int iTruck ){
		return setPackageLocation( iState, iPackage, m_cCities + iTruck );
	}
	
	private boolean[] loadPackage( int[] aiStateVariables, boolean[] abValues, int iPackage, int iTruck ){
		return setPackageLocation( aiStateVariables, abValues, iPackage, m_cCities + iTruck );
	}


	private int unloadPackage( int iState, int iPackage, int iTruck ){
		int iCity = getTruckLocation( iState, iTruck );
		return setPackageLocation( iState, iPackage, iCity );
	}

	private boolean[] unloadPackage( int[] aiStateVariables, boolean[] abValues, int iPackage, int iTruck ){
		int iCity = getTruckLocation( aiStateVariables, abValues, iTruck );
		return setPackageLocation( aiStateVariables, abValues, iPackage, iCity );
	}

	public double R( int iStartState, int iAction, int iEndState ){
		return R( iStartState, iAction );
	}
	
	public int[] getRelevantVariables( int iAction ){
		int[] aiVars = null;
		int iTruck = -1, iPackage = -1, iBit = 0, iLastBit = -1, iFirstBit = -1, iTruckBit = 0, iPackageBit = 0;
		iTruck = getActiveTruck( iAction );
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			iFirstBit = getFirstBitForTruck( iTruck );
			iLastBit = getFirstBitForTruck( iTruck + 1 );
			aiVars = new int[m_cBitsPerTruck];
			for( iTruckBit = iFirstBit ; iTruckBit < iLastBit ; iTruckBit++ ){
				aiVars[iBit++] = iTruckBit;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			aiVars = new int[m_cBitsPerTruck + m_cBitsPerPackage];
			iPackage = getActivePackage( iAction );
			iFirstBit = getFirstBitForPackage( iPackage );
			iLastBit = getFirstBitForPackage( iPackage + 1 );
			for( iPackageBit = iFirstBit ; iPackageBit < iLastBit ; iPackageBit++ ){
				aiVars[iBit++] = iPackageBit;
			}
			iFirstBit = getFirstBitForTruck( iTruck );
			iLastBit = getFirstBitForTruck( iTruck + 1 );
			for( iTruckBit = iFirstBit ; iTruckBit < iLastBit ; iTruckBit++ ){
				aiVars[iBit++] = iTruckBit;
			}
		}
		else if( isPingPackage( iAction ) ){
			aiVars = new int[m_cBitsPerPackage];
			iPackage = getActivePackage( iAction );
			iFirstBit = getFirstBitForPackage( iPackage );
			iLastBit = getFirstBitForPackage( iPackage + 1 );
			for( iPackageBit = iFirstBit ; iPackageBit < iLastBit ; iPackageBit++ ){
				aiVars[iBit++] = iPackageBit;
			}		
		}
		return aiVars;
	}
	
	protected int stateToIndex( boolean[] abState ){
		int cVariables = getStateVariablesCount();
		int iState = 0;
		int iVariable = cVariables - 1, idx = abState.length - 1;
		for( iVariable = cVariables - 1 ; iVariable >= 0 ; iVariable-- ){
			iState *= 2;
			if( abState[idx] )
				iState += 1;
			idx--;
		}		
		return iState;
	}
	
	private int getTerminalState(){
		int iPackage = 0, iCity = 0;
		int iState = 0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iCity = getTerminalLocation( iPackage );
			iState = setPackageLocation( iState, iPackage, iCity );
		}
		return iState;
	}
	
	protected int stateToIndex( int[] aiStateVariableIndexes, boolean[] abStateVariableValues ){
		int cVariables = getStateVariablesCount();
		int iState = 0, iTerminalState = getTerminalState();
		int iVariable = cVariables - 1, idx = aiStateVariableIndexes.length - 1;
		boolean[] abTerminalState = indexToState( iTerminalState );
		for( iVariable = cVariables - 1 ; iVariable >= 0 ; iVariable-- ){
			iState *= 2;
			if( ( idx >= 0 ) && ( aiStateVariableIndexes[idx] == iVariable ) ){
				if( abStateVariableValues[idx] )
					iState += 1;
				idx--;
			}
			else{
				if( abTerminalState[iVariable] )
					iState += 1;
			}
		}		
		return iState;
	}

	
	public boolean[] indexToState( int iState ){
		int iPackage = 0, iBit = 0, iPackageBit = 0;
		int iTruck = 0, iTruckBit = 0;
		boolean[] abState = new boolean[m_cTrucks * m_cBitsPerTruck + m_cPackages * m_cBitsPerPackage];
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			for( iPackageBit = 0 ; iPackageBit < m_cBitsPerPackage ; iPackageBit++ ){
				abState[iBit] = ( iState % 2 == 1 );
				iBit++;
				iState /= 2;
			}
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			for( iTruckBit = 0 ; iTruckBit < m_cBitsPerTruck ; iTruckBit++ ){
				abState[iBit] = ( iState % 2 == 1 );
				iBit++;
				iState /= 2;
			}
		}
		return abState;
	}

	protected double stateVariableObservation( int[] aiStateVariableIndexes,
			int iAction, boolean[] abStateVariableValues, int iObservation ) {
		throw new NotImplementedException();
	}

	protected double stateVariableTransition( int[] aiStateVariableIndexes,
			boolean[] abStateVariableValuesBefore, int iAction,
			boolean[] abStateVariableValuesAfter ) {
		throw new NotImplementedException();
	}

	protected double stateVariableTransition( int iState, int iAction,
			int iStateVariable ){
		throw new NotImplementedException();
	}
	
	@Override
	public double R(int[][] aiState, int iAction) {
		int[] aiVariables = aiState[0];
		boolean[] abState = toBool( aiState[1] );
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abState, iTruck );
			if( iLocation == iDestination )
				return -0.5;
			return -1 * ( iTruck + 1 );
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iTruckLocation = getTruckLocation( aiVariables, abState, iTruck );
			if( isOnTruck( aiVariables, abState, iPackage, iTruck ) && isTerminalLocation( iPackage, iTruckLocation ) )
				return 10.0;
			return -1;
		}
		else if( isPingAction( iAction ) ){//ping
			return 0.0;
		}
		return -100.0;
	}

	
	public double R( int iState, int iAction ){
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iState, iTruck );
			if( iLocation == iDestination )
				return -0.5;
			return -1 * ( iTruck + 1 );
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iTruckLocation = getTruckLocation( iState, iTruck );
			if( isOnTruck( iState, iPackage, iTruck ) && isTerminalLocation( iPackage, iTruckLocation ) )
				return 10.0;
			return -1;
		}
		else if( isPingAction( iAction ) ){//ping
			return 0.0;
		}
		return -100.0;
	}
	
	public double getMaxMinR(){
		return 0.0;
	}
	
	public double O(int iAction, int[][] aiEndState, int iObservation) {
		int[] aiVariables = aiEndState[0];
		boolean[] abValues = toBool( aiEndState[1] );
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abValues, iTruck );
			if( iLocation == iDestination ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( isOnTruck( aiVariables, abValues, iPackage, iTruck ) ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iPackageLocation = getPackageLocation( aiVariables, abValues, iPackage );
				if( iPackageLocation == iObservation ){
					return 0.75;
				}
				else{
					return 0.25 / ( m_cCities + m_cTrucks - 1 );					
				}
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iTruckLocation = getTruckLocation( aiVariables, abValues, iTruck );
				if( iTruckLocation == iObservation ){
					return 0.75;
				}
				else if( iObservation < m_cCities ){
					return 0.25 / ( m_cCities - 1 );					
				}
				return 0.0;
			}
		}
		return -1.0;
	}
	
	public double O( int iAction, int iEndState, int iObservation ){
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iEndState, iTruck );
			if( iLocation == iDestination ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( isOnTruck( iEndState, iPackage, iTruck ) ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iPackageLocation = getPackageLocation( iEndState, iPackage );
				if( iPackageLocation == iObservation ){
					return 0.75;
				}
				else{
					return 0.25 / ( m_cCities + m_cTrucks - 1 );					
				}
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iTruckLocation = getTruckLocation( iEndState, iTruck );
				if( iTruckLocation == iObservation ){
					return 0.75;
				}
				else if( iObservation < m_cCities ){
					return 0.25 / ( m_cCities - 1 );					
				}
				return 0.0;
			}
		}
		return -1.0;
	}
	

	@Override
	public int observe(int iAction, int[][] aiEndState) {
		int[] aiVariables = aiEndState[0];
		boolean[] abValues = toBool( aiEndState[1] );
		double dProb = m_rndGenerator.nextDouble();
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abValues, iTruck );
			if( iLocation == iDestination ){
				if( dProb < 0.9 )
					return 1;
				else
					return 0;
			}
			else{
				if( dProb < 0.9 )
					return 0;
				else
					return 1;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( isOnTruck( aiVariables, abValues, iPackage, iTruck ) ){
				if( dProb < 0.9 )
					return 1;
				else
					return 0;
			}
			else{
				if( dProb < 0.9 )
					return 0;
				else
					return 1;
			}
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iPackageLocation = getPackageLocation( aiVariables, abValues, iPackage );
				if( dProb < 0.8 ){
					return iPackageLocation;
				}
				else{
					dProb -= 0.8;
					for( int i = 0 ; i < m_cCities + m_cTrucks ; i++ ){
						if( i != iPackageLocation ){
							if( dProb <  0.2 / ( m_cCities + m_cTrucks - 1 ) )
								return i;
						}
						dProb -= 0.2 / ( m_cCities + m_cTrucks - 1 );
					}
				}
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iTruckLocation = getTruckLocation( aiVariables, abValues, iTruck );
				if( dProb < 0.8 ){
					return iTruckLocation;
				}
				else{
					dProb -= 0.8;
					for( int i = 0 ; i < m_cCities ; i++ ){
						if( i != iTruckLocation ){
							if( dProb <  0.2 / ( m_cCities - 1 ) )
								return i;
						}
						dProb -= 0.2 / ( m_cCities - 1 );
					}
				}
			}
		}
		return -1;
	}


		
	public int observe( int iAction, int iEndState ){
		double dProb = m_rndGenerator.nextDouble();
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iEndState, iTruck );
			if( iLocation == iDestination ){
				if( dProb < 0.9 )
					return 1;
				else
					return 0;
			}
			else{
				if( dProb < 0.9 )
					return 0;
				else
					return 1;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( isOnTruck( iEndState, iPackage, iTruck ) ){
				if( dProb < 0.9 )
					return 1;
				else
					return 0;
			}
			else{
				if( dProb < 0.9 )
					return 0;
				else
					return 1;
			}
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iPackageLocation = getPackageLocation( iEndState, iPackage );
				if( dProb < 0.8 ){
					return iPackageLocation;
				}
				else{
					dProb -= 0.8;
					for( int i = 0 ; i < m_cCities + m_cTrucks ; i++ ){
						if( i != iPackageLocation ){
							if( dProb <  0.2 / ( m_cCities + m_cTrucks - 1 ) )
								return i;
						}
						dProb -= 0.2 / ( m_cCities + m_cTrucks - 1 );
					}
				}
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iTruckLocation = getTruckLocation( iEndState, iTruck );
				if( dProb < 0.8 ){
					return iTruckLocation;
				}
				else{
					dProb -= 0.8;
					for( int i = 0 ; i < m_cCities ; i++ ){
						if( i != iTruckLocation ){
							if( dProb <  0.2 / ( m_cCities - 1 ) )
								return i;
						}
						dProb -= 0.2 / ( m_cCities - 1 );
					}
				}
			}
		}
		return -1;
	}
	
	public double tr( int[][] aiStartState, int iAction, int[][] aiEndState ) {
		if( !equals( aiStartState[0], aiEndState[0] ) )
			return Double.NaN;
		int[] aiVariables = aiStartState[0];
		boolean[] abStartState = toBool( aiStartState[1] );
		boolean[] abEndState = toBool( aiEndState[1] );
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abStartState, iTruck );
			if( iLocation == iDestination ){
				if( equals( abStartState, abEndState ) )
					return 1.0;
				else
					return 0.0;
			}
			int iCity = 0;
			boolean[] abState = null;
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				abState = setTruckLocation( aiVariables, abStartState, iTruck, iCity );
				if( equals( abStartState, abEndState ) ){
					if( iCity == iDestination )
						return 0.75; //reached destination city
					else
						return 0.25 / ( m_cCities - 1 ); //reached another city
				}
			}
			return 0.0; //other variables changed too
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiVariables, abStartState, iPackage );
			boolean[] abExpectedState = null;
			if( isTerminalLocation( iPackage, iPackageLocation ) ){
				if( equals( abStartState, abEndState ) )
					return 1.0;
				return 0.0;
			}
			int iTruckLocation = getTruckLocation( aiVariables, abStartState, iTruck );
			if( isOnTruck( aiVariables, abStartState, iPackage, iTruck ) ){
				abExpectedState = unloadPackage( aiVariables, abStartState, iPackage, iTruck );
				if( equals( abEndState, abExpectedState ) ){
					return 0.9;
				}
				else if( equals( abStartState, abEndState ) ){
					return 0.1;
				}
				return 0.0;
			}
			else{ //package currently not on truck
				if( iPackageLocation != iTruckLocation ){
					if( equals( abEndState, abStartState ) )
						return 1.0;
					return 0.0;
				}
				abExpectedState = loadPackage( aiVariables, abStartState, iPackage, iTruck );
				if( equals( abEndState, abExpectedState ) ){
					return 0.9;
				}
				else if( equals( abEndState, abStartState ) ){
					return 0.1;
				}
				return 0.0;
			}
		}
		else{ //ping action 
			if( equals( abEndState, abStartState ) )
				return 1.0;
			return 0.0;
		}
	}	

	
	public double tr( int iStartState, int iAction, int iEndState ){
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iStartState, iTruck );
			if( iLocation == iDestination ){
				if( iEndState == iStartState )
					return 1.0;
				else
					return 0.0;
			}
			int iCity = 0, iState = iStartState;
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				iState = setTruckLocation( iStartState, iTruck, iCity );
				if( iState == iEndState ){
					if( iCity == iDestination )
						return 0.75; //reached destination city
					else
						return 0.25 / ( m_cCities - 1 ); //reached another city
				}
			}
			return 0.0; //other variables changed too
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( iStartState, iPackage );
			int iExpectedState = 0;
			if( isTerminalLocation( iPackage, iPackageLocation ) ){
				if( iEndState == iStartState )
					return 1.0;
				return 0.0;
			}
			int iTruckLocation = getTruckLocation( iStartState, iTruck );
			if( isOnTruck( iStartState, iPackage, iTruck ) ){
				iExpectedState = unloadPackage( iStartState, iPackage, iTruck );
				if( iEndState == iExpectedState ){
					return 0.9;
				}
				else if( iStartState == iEndState ){
					return 0.1;
				}
				return 0.0;
			}
			else{ //package currently not on truck
				if( iPackageLocation != iTruckLocation ){
					if( iEndState == iStartState )
						return 1.0;
					return 0.0;
				}
				iExpectedState = loadPackage( iStartState, iPackage, iTruck );
				if( iExpectedState == iEndState ){
					return 0.9;
				}
				else if( iStartState == iEndState ){
					return 0.1;
				}
				return 0.0;
			}
		}
		else{ //ping action 
			if( iStartState == iEndState )
				return 1.0;
			return 0.0;
		}
	}
	
	@Override
	public int[][] execute(int iAction, int[][] aiStartState) {
		int[] aiVariables = aiStartState[0];
		boolean[] abStartValues = toBool( aiStartState[1] );
		boolean[] abEndValues = null;
		double dProb = m_rndGenerator.nextDouble();
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abStartValues, iTruck );
			if( iLocation == iDestination ){
				return aiStartState;
			}
			else{
				int iCity = 0;
				for( iCity = 0 ; iCity < m_cCities && dProb > 0.0 ; iCity++ ){
					if( iCity == iDestination )
						dProb -= .75; //reached destination city
					else
						dProb -= 0.25 / ( m_cCities - 1 ); //reached another city
					if( dProb <= 0.0 ){
						abEndValues = setTruckLocation( aiVariables, abStartValues, iTruck, iCity );
					}
				}
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiVariables, abStartValues, iPackage );
			int iTruckLocation = getTruckLocation( aiVariables, abStartValues, iTruck );
			if( isOnTruck( aiVariables, abStartValues, iPackage, iTruck ) ){ //unload action
				if( dProb < 0.9 )
					abEndValues = unloadPackage( aiVariables, abStartValues, iPackage, iTruck );
				else
					return aiStartState;
			}
			else{//package not loaded - load action
				if( ( iPackageLocation != iTruckLocation ) || isTerminalLocation( iPackage, iPackageLocation ) )
					return aiStartState;//cannot load package at terminal position or when truck and package not at the same location
				else{
					if( dProb < 0.9 )
						abEndValues =  loadPackage( aiVariables, abStartValues, iPackage, iTruck );
					else
						return aiStartState;
				}
			}
		}
		else{ //ping action 
			return aiStartState;
		}
		int[] aiEndValues = toInt( abEndValues );
		return new int[][]{ aiVariables, aiEndValues };
	}

	
	public int execute( int iAction, int iStartState ){
		double dProb = m_rndGenerator.nextDouble();
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iStartState, iTruck );
			if( iLocation == iDestination ){
				return iStartState;
			}
			else{
				int iCity = 0;
				for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
					if( iCity == iDestination )
						dProb -= .75; //reached destination city
					else
						dProb -= 0.25 / ( m_cCities - 1 ); //reached another city
					if( dProb <= 0.0 )
						return setTruckLocation( iStartState, iTruck, iCity );
				}
				return -1;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( iStartState, iPackage );
			int iTruckLocation = getTruckLocation( iStartState, iTruck );
			if( isOnTruck( iStartState, iPackage, iTruck ) ){ //unload action
				if( dProb < 0.9 )
					return unloadPackage( iStartState, iPackage, iTruck );
				else
					return iStartState;
			}
			else{//package not loaded - load action
				if( ( iPackageLocation != iTruckLocation ) || isTerminalLocation( iPackage, iPackageLocation ) )
					return iStartState;//cannot load package at terminal position or when truck and package not at the same location
				else{
					if( dProb < 0.9 )
					 return loadPackage( iStartState, iPackage, iTruck );
					else
						return iStartState;
				}
			}
		}
		else{ //ping action 
			return iStartState;
		}
	}


	public Iterator<Entry<int[][], Double>> getNonZeroTransitions( int[][] aiStartState, int iAction ) {
		Map<int[][],Double> mTransitions = new TreeMap<int[][],Double>( new State2Comparator() );
		int[] aiVariables = aiStartState[0];
		boolean[] abStartValues = toBool( aiStartState[1] );
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiVariables, abStartValues, iTruck );
			if( iLocation == iDestination ){
				mTransitions.put( aiStartState, 1.0 );
			}
			else{
				int iCity = 0;
				boolean[] abState = null;
				for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
					abState = setTruckLocation( aiVariables, abStartValues, iTruck, iCity );
					if( iCity == iDestination )
						mTransitions.put( getState2( aiVariables, abState ), .75 ); //reached destination city
					else
						mTransitions.put( getState2( aiVariables, abState ), 0.25 / ( m_cCities - 1 ) ); //reached another city
				}
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiVariables, abStartValues, iPackage );
			int iTruckLocation = getTruckLocation( aiVariables, abStartValues, iTruck );
			boolean[] abExpectedState = null;
			if( isOnTruck( aiVariables, abStartValues, iPackage, iTruck ) ){ //unload action
				abExpectedState = unloadPackage( aiVariables, abStartValues, iPackage, iTruck );
				mTransitions.put( getState2( aiVariables, abExpectedState ), 0.9 );
				mTransitions.put( aiStartState, 0.1 );
			}
			else{//package not loaded - load action
				if( ( iPackageLocation != iTruckLocation ) || isTerminalLocation( iPackage, iPackageLocation ) )
					mTransitions.put( aiStartState, 1.0 );//cannot load package at terminal position or when truck and package not at the same location
				else{
					abExpectedState = loadPackage( aiVariables, abStartValues, iPackage, iTruck );
					mTransitions.put( getState2( aiVariables, abExpectedState ), 0.9 );
					mTransitions.put( aiStartState, 0.1 );
				}
			}
		}
		else{ //ping action 
			mTransitions.put( aiStartState, 1.0 );
		}
		
		return mTransitions.entrySet().iterator();
	}

	
	public Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iStartState, int iAction ) {
		Map<Integer,Double> mTransitions = new TreeMap<Integer,Double>();
		
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( iStartState, iTruck );
			if( iLocation == iDestination ){
				mTransitions.put( iStartState, 1.0 );
			}
			else{
				int iCity = 0, iState = iStartState;
				for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
					iState = setTruckLocation( iStartState, iTruck, iCity );
					if( iCity == iDestination )
						mTransitions.put( iState, .75 ); //reached destination city
					else
						mTransitions.put( iState, 0.25 / ( m_cCities - 1 ) ); //reached another city
				}
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( iStartState, iPackage );
			int iTruckLocation = getTruckLocation( iStartState, iTruck );
			int iExpectedState = -1;
			if( isOnTruck( iStartState, iPackage, iTruck ) ){ //unload action
				iExpectedState = unloadPackage( iStartState, iPackage, iTruck );
				mTransitions.put( iExpectedState, 0.9 );
				mTransitions.put( iStartState, 0.1 );
			}
			else{//package not loaded - load action
				if( ( iPackageLocation != iTruckLocation ) || isTerminalLocation( iPackage, iPackageLocation ) )
					mTransitions.put( iStartState, 1.0 );//cannot load package at terminal position or when truck and package not at the same location
				else{
					iExpectedState = loadPackage( iStartState, iPackage, iTruck );
					mTransitions.put( iExpectedState, 0.9 );
					mTransitions.put( iStartState, 0.1 );
				}
			}
		}
		else{ //ping action 
			mTransitions.put( iStartState, 1.0 );
		}
		
		return mTransitions.entrySet().iterator();
	}
	
	private boolean isPackageSpecified( int[] aiVariables, int iPackage ){
		int iFirstBit = getFirstBitForPackage( iPackage );
		for( int iVar : aiVariables ){
			if( iVar == iFirstBit )
				return true;
		}
		return false;
	}

	@Override
	public boolean isTerminalState(int[][] aiState) {
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iPackage = 0, iPackageLocation = 0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			if( isPackageSpecified( aiVariables, iPackage ) ){
				iPackageLocation = getPackageLocation( aiVariables, abValues, iPackage );
				if( !isTerminalLocation( iPackage, iPackageLocation ) )
					return false;
			}
		}
		return true;
	}
	
	public boolean isTerminalState( int iState ){
		int iPackage = 0, iPackageLocation = 0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iPackageLocation = getPackageLocation( iState, iPackage );
			if( !isTerminalLocation( iPackage, iPackageLocation ) )
				return false;
		}
		return true;
	}
	
	public boolean terminalStatesDefined(){
		return true;
	}

	@Override
	public String getStateName( int[][] aiState ) {
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iPackage = 0, iTruck = 0, iLocation = 0;
		String sState = "<";
		
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iLocation = getPackageLocation( aiVariables, abValues, iPackage );
			if( iLocation < m_cCities )
				sState += "P" + iPackage + "=C" + iLocation;
			else
				sState += "P" + iPackage + "=T" + ( iLocation - m_cCities );
			sState += ", ";
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iLocation = getTruckLocation( aiVariables, abValues, iTruck );
			sState += "T" + iTruck + "=C" + iLocation;
			sState += ", ";
		}
		sState += ">";
		return sState;
	}
	
	public String getStateName( int iState ){
		int iPackage = 0, iTruck = 0, iLocation = 0;
		String sState = "<";
		
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iLocation = getPackageLocation( iState, iPackage );
			if( iLocation < m_cCities )
				sState += "P" + iPackage + "=C" + iLocation;
			else
				sState += "P" + iPackage + "=T" + ( iLocation - m_cCities );
			sState += ", ";
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iLocation = getTruckLocation( iState, iTruck );
			sState += "T" + iTruck + "=C" + iLocation;
			sState += ", ";
		}
		sState += ">";
		return sState;
	}
	
	public String getActionName( int iAction ){
		int iTruck = 0, iPackage = 0, iCity = 0;
		if( isDriveAction( iAction ) ){
			iTruck = getActiveTruck( iAction );
			iCity = getTruckDestination( iAction );
			return "Drive_T" + iTruck + "_C" + iCity;
		}
		else if( isLoadUnloadAction( iAction ) ){
			iTruck = getActiveTruck( iAction );
			iPackage = getActivePackage( iAction );
			return "Load_Unload_P" + iPackage + "_T" + iTruck;
		}
		else if( isPingTruck( iAction ) ){
			iTruck = getActiveTruck( iAction );
			return "Ping_T" + iTruck ;
		}
		else if( isPingPackage( iAction ) ){
			iPackage = getActivePackage( iAction );
			return "Ping_P" + iPackage;
		}
		return "N/A";
	}
	private int factorial( int n ){
		int fact = 1;
		while( n > 0 ){
			fact *= n;
			n--;
		}
		return fact;
	}
	
	public int getStartStateCount() {
		return -1;
	}
	
	private void swap( int[] a, int i, int j ){
		int aux = a[i];
		a[i] = a[j];
		a[j] = aux;
	}
	
	public int[][] chooseStartState2(){
		boolean[] abValues = new boolean[m_cStateVariables];
		int[] aiVariables = new int[m_cStateVariables];
		int iVar = 0;
		int iPackage = 0, iCity = 0, iTruck = 0;
		
		for( iVar = 0 ; iVar < m_cStateVariables ; iVar++ )
			aiVariables[iVar] = iVar;
		
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iCity = m_rndGenerator.nextInt( m_cCities );
			abValues = setPackageLocation( aiVariables, abValues, iPackage, iCity );
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iCity = m_rndGenerator.nextInt( m_cCities );
			abValues = setTruckLocation( aiVariables, abValues, iTruck, iCity );
		}
		return getState2( aiVariables, abValues );
	}

	
	public int chooseStartState(){
		int iPackage = 0, iState = 0, iCity = 0, iTruck = 0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iCity = m_rndGenerator.nextInt( m_cCities );
			iState = setPackageLocation( iState, iPackage, iCity );
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iCity = m_rndGenerator.nextInt( m_cCities );
			iState = setTruckLocation( iState, iTruck, iCity );
		}
		return iState;
	}


	public double probStartState(int[][] aiState) {
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iPackage = 0, iLocation = 0, iTruck = 0;
		double dProb = 1.0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iLocation = getPackageLocation( aiVariables, abValues, iPackage );
			if( iLocation >= m_cCities )
				return 0.0;
			dProb *= 1.0 / m_cCities;
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iLocation = getTruckLocation( aiVariables, abValues, iTruck );
			if( iLocation >= m_cCities )
				return 0.0;
			dProb *= 1.0 / m_cCities;
		}
		return dProb;
	}

	public double probStartState( int iState ){
		int iPackage = 0, iLocation = 0, iTruck = 0;
		double dProb = 1.0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iLocation = getPackageLocation( iState, iPackage );
			if( iLocation >= m_cCities )
				return 0.0;
			dProb *= 1.0 / m_cCities;
		}
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iLocation = getTruckLocation( iState, iTruck );
			if( iLocation >= m_cCities )
				return 0.0;
			dProb *= 1.0 / m_cCities;
		}
		return dProb;
	}

		
//********************************************************************************************

	private boolean isPackageVariable( int iVariable ){
		return iVariable < m_cPackages * m_cBitsPerPackage;
	}
	private boolean isTruckVariable( int iVariable ){
		return iVariable >= m_cPackages * m_cBitsPerPackage && iVariable < m_cPackages * m_cBitsPerPackage + m_cTrucks * m_cBitsPerTruck;
	}

	
	public double getInitialVariableValueProbability( int iVariable, boolean bValue ){
		return 0.5;
	}

	
	public String getObservationName( int iObservation ){
		if( iObservation < m_cCities )
			return "oCity" + iObservation;
		else
			return "oTruck" + ( iObservation - m_cCities );
	}

	
	protected String getObservationName() {
		return "ObserveResult";
	}

	
	protected int[] getObservationRelevantVariables( int iAction ){
		int[] aiRelevantVars = null;
		int i = 0;
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iFirstBit = getFirstBitForTruck( iTruck );
			aiRelevantVars = new int[m_cBitsPerTruck];
			for( i = 0 ; i < m_cBitsPerTruck ; i++ )
				aiRelevantVars[i] = iFirstBit + i;
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iFirstBit = getFirstBitForPackage( iPackage );
			aiRelevantVars = new int[m_cBitsPerTruck + m_cBitsPerPackage];
			for( i = 0 ; i < m_cBitsPerPackage ; i++ )
				aiRelevantVars[i] = iFirstBit + i;
			iFirstBit = getFirstBitForTruck( iTruck );
			for( i = 0 ; i < m_cBitsPerTruck ; i++ )
				aiRelevantVars[m_cBitsPerPackage + i] = iFirstBit + i;
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iFirstBit = getFirstBitForPackage( iPackage );
				aiRelevantVars = new int[m_cBitsPerPackage];
				for( i = 0 ; i < m_cBitsPerPackage ; i++ )
					aiRelevantVars[i] = iFirstBit + i;
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iFirstBit = getFirstBitForTruck( iTruck );
				aiRelevantVars = new int[m_cBitsPerTruck];
				for( i = 0 ; i < m_cBitsPerTruck ; i++ )
					aiRelevantVars[i] = iFirstBit + i;
			}
		}
		else{
			aiRelevantVars = new int[0];
		}
		return aiRelevantVars;
	}

	
	protected int[] getRewardRelevantVariables( int iAction ){
		int[] aiRelevantVars = null;
		int i = 0;
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iFirstBit = getFirstBitForTruck( iTruck );
			aiRelevantVars = new int[m_cBitsPerTruck];
			for( i = 0 ; i < m_cBitsPerTruck ; i++ )
				aiRelevantVars[i] = iFirstBit + i;
		}
		else if( isLoadUnloadAction( iAction ) ){
			aiRelevantVars = new int[m_cBitsPerTruck + m_cBitsPerPackage];
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iFirstBit = getFirstBitForPackage( iPackage );
			for( i = 0 ; i < m_cBitsPerPackage ; i++ )
				aiRelevantVars[i] = iFirstBit + i;
			iFirstBit = getFirstBitForTruck( iTruck );
			for( i = 0 ; i < m_cBitsPerTruck ; i++ )
				aiRelevantVars[m_cBitsPerPackage + i] = iFirstBit + i;
		}
		else{
			aiRelevantVars = new int[0];
		}
		return aiRelevantVars;
	}

	
	protected String getVariableName( int iVariable ){
		String sName = "";
		if( isPackageVariable( iVariable ) ){
			int iPackage = getPackageFromVariable( iVariable );
			sName = "P" + iPackage + "_" + ( iVariable - getFirstBitForPackage( iPackage ) );			
		}
		else if( isTruckVariable( iVariable ) ){
			int iTruck = getTruckFromVariable( iVariable );
			sName = "T" + iTruck + "_" + ( iVariable - getFirstBitForTruck( iTruck ) );			
		}
		return sName;
	}

	private int getPackageFromVariable( int iVariable ){
		int iPackage = 0;
		for( iPackage = 0 ; iPackage <= m_cPackages ; iPackage++ ){
			if( iVariable < getFirstBitForPackage( iPackage ) )
				return iPackage - 1;
		}
		return -1;
	}

	private int getTruckFromVariable( int iVariable ){
		int iTruck = 0;
		for( iTruck = 0 ; iTruck <= m_cTrucks ; iTruck++ ){
			if( iVariable < getFirstBitForTruck( iTruck ) )
				return iTruck - 1;
		}
		return -1;
	}

	
	public double observationGivenRelevantVariables( int iAction, int iObservation, 
			int[] aiRelevantVariables, boolean[] abValues ) {
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
			if( iLocation == iDestination ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( isOnTruck( aiRelevantVariables, abValues, iPackage, iTruck ) ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
				return 0.0;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
				return 0.0;
			}
		}
		else if( isPingAction( iAction ) ){ //ping action
			if( isPingPackage( iAction ) ){
				int iPackage = getActivePackage( iAction );
				int iPackageLocation = getPackageLocation( aiRelevantVariables, abValues, iPackage );
				if( iPackageLocation == iObservation ){
					return 0.8;
				}
				else{
					return 0.2 / ( m_cCities + m_cTrucks - 1 );					
				}
			}
			else if( isPingTruck( iAction ) ){
				int iTruck = getActiveTruck( iAction );
				int iTruckLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
				if( iTruckLocation == iObservation ){
					return 0.8;
				}
				else if( iObservation < m_cCities ){
					return 0.2 / ( m_cCities - 1 );					
				}
				return 0.0;
			}
		}
		return -1.0;
	}

	
	protected boolean relevantTransitionVariable( int iAction, int iVariable ){
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iFirstBit = getFirstBitForTruck( iTruck );
			return iVariable >= iFirstBit && iVariable < iFirstBit + m_cBitsPerTruck;
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iFirstBit = getFirstBitForPackage( iPackage );
			return iVariable >= iFirstBit && iVariable < iFirstBit + m_cBitsPerPackage;
		}
		else if( isPingAction( iAction ) ){ //ping action
			return false;
		}
		return false;
	}

	
	public double rewardGivenRelevantVariables( int iAction, int[] aiRelevantVariables, boolean[] abValues ) {
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
			if( iLocation == iDestination )
				return 0.0;
			return -0.5 * ( iTruck + 1 );
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iTruckLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
			int iPackageLocation = getPackageLocation( aiRelevantVariables, abValues, iPackage );
			if( isOnTruck( aiRelevantVariables, abValues, iPackage, iTruck ) && isTerminalLocation( iPackage, iTruckLocation ) )
				return 10.0;
			//if( isTerminalLocation( iPackage, iPackageLocation ) )
			//	return -10.0;
			return -0.1;
		}
		else{
			return -0.1;
		}
	}
	
	
	public double transitionGivenRelevantVariables( int iAction, int iVariable, boolean bValue, 
			int[] aiRelevantVariables, boolean[] abValues ){
		
		boolean[] abRevisedValues = null;
		
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
			if( iLocation == iDestination ){
				if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue )
					return 1.0;
				else
					return 0.0;
			}
			abRevisedValues = setTruckLocation( aiRelevantVariables, abValues, iTruck, iDestination );
			if( getVariableValue( aiRelevantVariables, abRevisedValues, iVariable ) == bValue )
				return 0.75; //reached destination city
			else
				return 0.25 / ( m_cCities - 1 ); //reached another city
			
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiRelevantVariables, abValues, iPackage );
			if( isTerminalLocation( iPackage, iPackageLocation ) ){
				if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue )
					return 1.0;
				return 0.0;
			}
			int iTruckLocation = getTruckLocation( aiRelevantVariables, abValues, iTruck );
			if( isOnTruck( aiRelevantVariables, abValues, iPackage, iTruck ) ){
				abRevisedValues = unloadPackage( aiRelevantVariables, abValues, iPackage, iTruck );
				if( getVariableValue( aiRelevantVariables, abRevisedValues, iVariable ) == bValue ){ //succeeded to unload
					return 0.9;
				}
				else if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue ){ //did not succeed to unload - package remained on the same truck
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
			else{ //package currently not on truck
				if( iPackageLocation != iTruckLocation ){
					if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue )
						return 1.0;
					return 0.0;
				}
				abRevisedValues = loadPackage( aiRelevantVariables, abValues, iPackage, iTruck );
				if( getVariableValue( aiRelevantVariables, abRevisedValues, iVariable ) == bValue ){ //succeeded to load 
					return 0.9;
				}
				else if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue ){ //did not succeed to load - package remained in the same city
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
		}
		else{ //ping action
			if( getVariableValue( aiRelevantVariables, abValues, iVariable ) == bValue )
				return 1.0;
			return 0.0;
		}
	}

	
	protected double getInitialVariableValueProbability( int iVariable, int iValue ){
		if( iValue < m_cCities )
			return 1.0 / m_cCities;
		return 0.0;
	}

	
	protected int[] getObservationRelevantVariablesMultiValue( int iAction ){
		int[] aiVars = null;
		int iTruck = -1, iPackage = -1;
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			iTruck = getActiveTruck( iAction );
			aiVars = new int[1];
			aiVars[0] = iTruck;
		}
		else if( isLoadUnloadAction( iAction ) || isPingPackage( iAction ) ){
			iPackage = getActivePackage( iAction );
			aiVars = new int[1];
			aiVars[0] = m_cTrucks + iPackage;
		}
		return aiVars;
	}

	
	protected int getRealStateVariableCount() {
		return m_cTrucks + m_cPackages;
	}

	
	protected String getRealVariableName( int iVariable ){
		String sName = null;
		if( iVariable < m_cTrucks )
			sName =  "varTruck" + iVariable;
		else
			sName =  "varPackage" + ( iVariable - m_cTrucks );
		return sName;
	}

	
	protected int[] getRelevantVariablesMultiValue( int iAction, int iVariable ){
		int[] aiVars = null;
		int iTruck = -1, iPackage = -1;
		iTruck = getActiveTruck( iAction );
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			aiVars = new int[1];
			aiVars[0] = iTruck;
		}
		else if( isLoadUnloadAction( iAction ) ){
			aiVars = new int[m_cBitsPerTruck + m_cBitsPerPackage];
			iPackage = getActivePackage( iAction );
			aiVars = new int[2];
			aiVars[0] = iTruck;
			aiVars[1] = m_cTrucks + iPackage;
		}
		else if( isPingPackage( iAction ) ){
			iPackage = getActivePackage( iAction );
			aiVars = new int[1];
			aiVars[0] = m_cTrucks + iPackage;
		}
		return aiVars;
	}

	
	protected int getValueCount( int iVariable ){
		if( iVariable < m_cTrucks )
			return m_cCities;
		else
			return m_cCities + m_cTrucks;
	}

	
	protected String getValueName( int iVariable, int iValue ){
		if( iValue < m_cCities )
			return "valCity" + iValue;
		else
			return "valTruck" + ( iValue - m_cCities );
	}

	
	protected double observationGivenRelevantVariablesMultiValue( int iAction, int iObservation, int[] aiRelevantVariables, int[] aiValues ) {
		int iTruck = -1, iPackage = -1;
		if( isDriveAction( iAction ) ){
			iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			if( aiValues[0] == iDestination ){
				if( iObservation == 1 )
					return 0.9;
				if( iObservation == 0 )
					return 0.1;
			}
			else{
				if( iObservation == 1 )
					return 0.1;
				if( iObservation == 0 )
					return 0.9;
			}
			return 0.0;
		}
		else if( isPingTruck( iAction ) ){
			iTruck = getActiveTruck( iAction );
			if( aiRelevantVariables[0] == iTruck ){
				if( iObservation == aiValues[0] ){
					return 0.75;
				}
				else if( iObservation < m_cCities ){
					return 0.25 / ( m_cCities - 1 );
				}
			}
			return 0.0;
		}
		else if( isLoadUnloadAction( iAction ) ){
			iPackage = getActivePackage( iAction );
			iTruck = getActiveTruck( iAction );
			if( aiRelevantVariables[0] == m_cTrucks + iPackage ){
				if( aiValues[0] == m_cCities + iTruck ){ // package is on the truck
					if( iObservation == 0 ){
						return 0.1;
					}
					else if( iObservation == 1 ){
						return 0.9;
					}
				}
				else{
					if( iObservation == 0 ){
						return 0.9;
					}
					else if( iObservation == 1 ){
						return 0.1;
					}
				}
			}
			return 0.0;
		}
		else if( isPingPackage( iAction ) ){
			iPackage = getActivePackage( iAction );
			if( aiRelevantVariables[0] == m_cTrucks + iPackage ){
				if( iObservation == aiValues[0] ){
					return 0.75;
				}
				else{
					return 0.25 / ( m_cCities + m_cTrucks - 1 );
				}
			}
			return 0.0;
		}
		return 0.0;
	}

	
	protected boolean relevantTransitionRealVariable( int iAction, int iVariable ){
		int iTruck = -1, iPackage = -1;
		iTruck = getActiveTruck( iAction );
		if( isDriveAction( iAction ) ){
			if( iVariable == iTruck )
				return true;
			return false;
		}
		else if( isLoadUnloadAction( iAction ) ){
			iPackage = getActivePackage( iAction );
			if( iVariable == m_cTrucks + iPackage )
				return true;
			return false;
		}
		return false;
	}

	
	protected double rewardGivenRelevantVariablesMultiValue( int iAction, int[] aiRelevantVariables, int[] aiValues ) {
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iTruckLocation = getTruckLocation( aiRelevantVariables, aiValues, iTruck );
			if( iTruckLocation == iDestination )
				return 0.0;
			return -0.5 * ( iTruck + 1 );
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iTruckLocation = getTruckLocation( aiRelevantVariables, aiValues, iTruck );
			int iPackageLocation = getPackageLocation( aiRelevantVariables, aiValues, iPackage );
			if( ( iPackageLocation == m_cCities + iTruck ) && isTerminalLocation( iPackage, iTruckLocation ) )
				return 10.0;
			/*
			else if( isTerminalLocation( iPackage, iPackageLocation ) )
				return -10.0;
				*/
			return -0.1;
		}
		else{
			return -0.1;
		}
	}

	
	protected double transitionGivenRelevantVariablesMultiValue( int iAction, int iVariable, int iValue, int[] aiRelevantVariables, int[] aiValues ) {
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiRelevantVariables, aiValues, iTruck );
			if( iLocation == iDestination ){
				if( getVariableValue( aiRelevantVariables, aiValues, iVariable ) == iValue )
					return 1.0;
				else
					return 0.0;
			}
			else{
				if( iDestination == iValue )
					return 0.75; //reached destination city
				else
					return 0.25 / ( m_cCities - 1 ); //reached another city
			}
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiRelevantVariables, aiValues, iPackage );
			if( isTerminalLocation( iPackage, iPackageLocation ) ){
				if( getVariableValue( aiRelevantVariables, aiValues, iVariable ) == iValue )
					return 1.0;
				return 0.0;
			}
			int iTruckLocation = getTruckLocation( aiRelevantVariables, aiValues, iTruck );
			if( iPackageLocation == m_cCities + iTruck ){
				if( iTruckLocation == iValue ){ //succeeded to unload
					return 0.9;
				}
				else if( m_cCities + iTruck == iValue ){ //did not succeed to unload - package remained on the same truck
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
			else{ //package currently not on truck
				if( iPackageLocation != iTruckLocation ){
					if( getVariableValue( aiRelevantVariables, aiValues, iVariable ) == iValue )
						return 1.0;
					return 0.0;
				}
				if( m_cCities + iTruck == iValue ){ //succeeded to load 
					return 0.9;
				}
				else if( iPackageLocation == iValue ){ //did not succeed to load - package remained in the same city
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
		}
		else{ //ping action
			if( getVariableValue( aiRelevantVariables, aiValues, iVariable ) == iValue )
				return 1.0;
			return 0.0;
		}
	}

	
	protected int[] getRewardRelevantVariablesMultiValue( int iAction ){
		int[] aiVars = null;
		int iTruck = -1, iPackage = -1;
		iTruck = getActiveTruck( iAction );
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			aiVars = new int[1];
			aiVars[0] = iTruck;
		}
		else if( isLoadUnloadAction( iAction ) ){
			aiVars = new int[m_cBitsPerTruck + m_cBitsPerPackage];
			iPackage = getActivePackage( iAction );
			aiVars = new int[2];
			aiVars[0] = iTruck;
			aiVars[1] = m_cTrucks + iPackage;
		}
		else if( isPingPackage( iAction ) ){
			aiVars = new int[0];
		}
		return aiVars;
	}

	
	public boolean changingComponent( int iComponent, int iAction, int iObservation ){
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			if( iComponent - m_cPackages == iTruck )
				return true;
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			if( iComponent == iPackage )
				return true;
		}
		return false;
	}

	
	public int[] getIndependentComponentVariables( int iComponent ){
		int[] aiVariables = null;
		if( iComponent < m_cPackages ){
			aiVariables = new int[m_cBitsPerPackage];
			int iBit = 0;
			for( iBit = 0 ; iBit < m_cBitsPerPackage ; iBit++ ){
				aiVariables[iBit] = getFirstBitForPackage( iComponent ) + iBit;
			}
		}
		else if( iComponent < m_cPackages + m_cTrucks ){
			aiVariables = new int[m_cBitsPerTruck];
			int iBit = 0;
			for( iBit = 0 ; iBit < m_cBitsPerTruck ; iBit++ ){
				aiVariables[iBit] = getFirstBitForTruck( iComponent - m_cPackages ) + iBit;
			}
		}
		return aiVariables;
	}

	
	public int getIndependentComponentsCount() {
		return m_cTrucks + m_cPackages;
	}

	
	public double getInitialComponentValueProbability( int iComponent, int iValue ){
		if( iComponent < m_cPackages ){
			if( iValue < m_cCities + m_cTrucks )
				return 1.0 / ( m_cCities + m_cTrucks );
		}
		else{
			if( iValue < m_cCities )
				return 1.0 / m_cCities;
		}
		return 0;
	}

	
	public int[] getRelevantComponents( int iAction, int iObservation ){
		int[] aiComponents = null;
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			aiComponents = new int[]{ iTruck + m_cPackages };
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			aiComponents = new int[]{ iPackage, iTruck + m_cPackages };
		}
		else if( isPingPackage( iAction ) ){
			int iPackage = getActivePackage( iAction );
			aiComponents = new int[]{ iPackage };
		}
		return aiComponents;
	}

	
	public int[] getRelevantVariablesForComponent( int iAction, int iComponent ) {
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			return getIndependentComponentVariables( iComponent );
		}
		else if( isLoadUnloadAction( iAction ) ){
			int[] aiVariables = new int[m_cBitsPerPackage + m_cBitsPerTruck];
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( iComponent != iPackage )
				return getIndependentComponentVariables( iComponent );
				
			aiVariables = new int[m_cBitsPerPackage + m_cBitsPerTruck];
			int iBit = 0;
			for( iBit = 0 ; iBit < m_cBitsPerPackage ; iBit++ ){
				aiVariables[iBit] = getFirstBitForPackage( iPackage ) + iBit;
			}
			for( iBit = 0 ; iBit < m_cBitsPerTruck ; iBit++ ){
				aiVariables[iBit + m_cBitsPerPackage] = getFirstBitForTruck( iTruck ) + iBit;
			}
			return aiVariables;
		}
		else if( isPingPackage( iAction ) ){
			return getIndependentComponentVariables( iComponent );
		}
		return null;
	}

	private boolean sameValues( int[] aiComponent, boolean[] abComponentValues, 
			int[] aiRelevantVariables, boolean[] abRelevantValues ){
		for( int iVariable = 0 ; iVariable < aiComponent.length ; iVariable++ ){
			if( getVariableValue( aiRelevantVariables, abRelevantValues, aiComponent[iVariable] ) != abComponentValues[iVariable] )
				return false;
		}
		return true;
	}
	
	
	public double transitionGivenRelevantVariables( int iAction, int[] aiComponent, 
			boolean[] abComponentValues, int[] aiRelevantVariables, boolean[] abRelevantValues ){
		
		boolean[] abRevisedValues = null;
		
		if( isDriveAction( iAction ) ){
			int iTruck = getActiveTruck( iAction );
			int iDestination = getTruckDestination( iAction );
			int iLocation = getTruckLocation( aiRelevantVariables, abRelevantValues, iTruck );
			if( iLocation == iDestination ){
				if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) )
					return 1.0;
				else
					return 0.0;
			}
			abRevisedValues = setTruckLocation( aiRelevantVariables, abRelevantValues, iTruck, iDestination );
			if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRevisedValues ) )
				return 0.75; //reached destination city
			else
				return 0.25 / ( m_cCities - 1 ); //reached another city
			
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			int iPackageLocation = getPackageLocation( aiRelevantVariables, abRelevantValues, iPackage );
			if( isTerminalLocation( iPackage, iPackageLocation ) ){
				if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) )
					return 1.0;
				return 0.0;
			}
			int iTruckLocation = getTruckLocation( aiRelevantVariables, abRelevantValues, iTruck );
			if( isOnTruck( aiRelevantVariables, abRelevantValues, iPackage, iTruck ) ){
				abRevisedValues = unloadPackage( aiRelevantVariables, abRelevantValues, iPackage, iTruck );
				if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRevisedValues ) ){ //succeeded to unload
					return 0.9;
				}
				else if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) ){ //did not succeed to unload - package remained on the same truck
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
			else{ //package currently not on truck
				if( iPackageLocation != iTruckLocation ){
					if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) )
						return 1.0;
					return 0.0;
				}
				abRevisedValues = loadPackage( aiRelevantVariables, abRelevantValues, iPackage, iTruck );
				if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRevisedValues ) ){ //succeeded to load 
					return 0.9;
				}
				else if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) ){ //did not succeed to load - package remained in the same city
					return 0.1;
				}
				return 0.0; // package location changed to something else
			}
		}
		else{ //ping action
			if( sameValues( aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues ) )
				return 1.0;
			return 0.0;
		}
	}

	
	public int[] getRelevantComponentsForComponent( int iAction, int iComponent ){
		if( isDriveAction( iAction ) || isPingTruck( iAction ) ){
			return new int[]{ iComponent };
		}
		else if( isLoadUnloadAction( iAction ) ){
			int iPackage = getActivePackage( iAction );
			int iTruck = getActiveTruck( iAction );
			if( iComponent != iPackage )
				return new int[]{ iComponent };
				
			return new int[]{ iComponent, iTruck + m_cPackages };
		}
		else if( isPingPackage( iAction ) ){
			return new int[]{ iComponent };
		}
		return null;
	}

	public int getCitiesCount() {
		return m_cCities;
	}

	public int getTrucksCount() {
		return m_cTrucks;
	}

	public int getPackageCount() {
		return m_cPackages;
	}

	protected double computeImmediateReward( BeliefState bs, int iAction ){
		if( bs instanceof LogisticsBeliefState ){
			LogisticsBeliefState lbs = (LogisticsBeliefState)bs;
			return lbs.computeImmediateReward( iAction );
		}
		return super.computeImmediateReward( bs, iAction );
	}

}
