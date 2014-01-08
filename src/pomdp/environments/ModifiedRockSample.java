package pomdp.environments; 

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.ForwardSearchValueIteration;
import pomdp.algorithms.pointbased.ForwardSearchValueIteration.HeuristicType;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.factored.AlgebraicDecisionDiagram;
import pomdp.utilities.factored.FactoredBeliefStateFactory;
import pomdp.utilities.factored.ModifiedRockSampleBeliefState;
import pomdp.utilities.factored.ModifiedRockSampleBeliefStateFactory;
import pomdp.valuefunction.MDPValueFunction;


public class ModifiedRockSample extends FactoredPOMDP {

	protected int[][] m_aiRockLocations; 
	protected int m_cX, m_cY, m_cRocks;
	protected int m_iInitialX, m_iInitialY; 
	protected int m_cXBits, m_cYBits;
	protected boolean m_bReturnToStart = false;
	protected double sensorHalfDistance;
	
	public enum Action{
		North, East, South, West, Check, Sample;
	}

	/**
	 * Prints the rock sample map.
	 * O denotes empty square.
	 * + denotes rock location.
	 *
	 */
	public void printMap(){
		int iX = 0, iY = 0, iRock = 0;
		int[][] aiMap = new int[m_cX][m_cY];
		String sChar = "";
		for( iY = 0 ; iY < m_cY ; iY++ ){
			for( iX = 0 ; iX < m_cX ; iX++ ){
				aiMap[iX][iY] = -1;	
			}
		}
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			iX = m_aiRockLocations[iRock][0];
			iY = m_aiRockLocations[iRock][1];
			aiMap[iX][iY] = iRock;
		}
		for( iY = 0 ; iY < m_cY ; iY++ ){
			for( iX = 0 ; iX < m_cX ; iX++ ){
				if( aiMap[iX][iY] != -1 ){
					sChar = ( (char)('0' + aiMap[iX][iY]) ) + "";
				}
				else
					sChar = "-";
				if( ( iX == m_iInitialX ) && ( iY == m_iInitialY ) )
					sChar = "S";
				Logger.getInstance().log( sChar );
			}
			Logger.getInstance().logln(  );
		}
	}
	
	public String getName(){
		return "ModifiedRockSample_" + m_cX + "_" + m_cY + "_" + m_cRocks;
	}
	
	public ModifiedRockSample( int cX, int cY, int cRocks, int halfSensorDistance, BeliefType btFactored, boolean bUseSpecialADDForG, boolean bUseRelevantVariablesOnly ){
		this( cX, cY, cRocks, halfSensorDistance, computeRockLocations( cX, cY, cRocks ), btFactored, bUseSpecialADDForG, bUseRelevantVariablesOnly );
	}
			
	public ModifiedRockSample( int cX, int cY, int cRocks, int halfSensorDistance, BeliefType btFactored ){
		this( cX, cY, cRocks, halfSensorDistance, computeRockLocations( cX, cY, cRocks ), btFactored, false, true );
	}
	public ModifiedRockSample( int cX, int cY, int cRocks, int halfSensorDistance, BeliefType btFactored, double gamma ){
		
		this( cX, cY, cRocks, halfSensorDistance, computeRockLocations( cX, cY, cRocks ), btFactored, false, true );
		m_dGamma = gamma;
	}	
	public ModifiedRockSample( int cX, int cY, int cRocks, int halfSensorDistance){
		this( cX, cY, cRocks, halfSensorDistance, computeRockLocations( cX, cY, cRocks ), BeliefType.Factored, false, true );
	}
			
	public ModifiedRockSample( int cX, int cY, int cRocks, int halfSensorDistance, int[][] aiRockLocations, BeliefType btFactored, boolean bUseSpecialADDForG, boolean bUseRelevantVariablesOnly ){
		super( log( cX ) + log( cY ) + cRocks, 4 + cRocks + 1, 2, btFactored, bUseSpecialADDForG, bUseRelevantVariablesOnly );
		m_cXBits = log( cX ); 
		m_cYBits = log( cY );
		m_cX = cX;
		m_cY = cY;
		sensorHalfDistance = halfSensorDistance;
		m_iInitialX = 0; //change here to get a different starting place
		m_iInitialY = 0; //change here to get a different starting place
		m_cRocks = cRocks;
		m_cActions = 4 + cRocks + 1; //north east south west + check each rock + sample 
		m_cObservations = 2; //good, not-good
		m_dGamma = 0.95;
		
		
		m_aiRockLocations = aiRockLocations;

		if( btFactored == BeliefType.Independent )
			m_bsFactory = new ModifiedRockSampleBeliefStateFactory( this );
		
		if( btFactored == BeliefType.Factored )
			initADDs();

		printMap();
	}


	public Action intToAction( int iAction ){
		if( iAction == 0 )
			return Action.North;
		if( iAction == 1 )
			return Action.East;
		if( iAction == 2 )
			return Action.South;
		if( iAction == 3 )
			return Action.West;
		if( iAction >= 4 && iAction < m_cRocks + 4 )
			return Action.Check;
		if( iAction == m_cRocks + 4 )
			return Action.Sample;
		return null;
	}
	
	protected int[][] stateToIndex2( int iX, int iY, boolean[] abRocks ){
		int[] aiVariables = new int[m_cStateVariables];
		int[] aiValues = new int[m_cStateVariables];
		int iRock = 0;
		int iVar = 0;
		for( iVar = 0 ; iVar < m_cStateVariables ; iVar++ ){
			aiVariables[iVar] = iVar;
			if( iVar < m_cXBits ){
				aiValues[iVar] = iX % 2;
				iX /= 2;
			}
			else if( iVar < m_cXBits + m_cYBits ){
				aiValues[iVar] = iY % 2;
				iY /= 2;
			}
			else{
				if( abRocks[iVar - m_cXBits - m_cYBits] )
					aiValues[iVar] = 1;
				else
					aiValues[iVar] = 0;
			}
		}
		return new int[][]{ aiVariables, aiValues };
	}
	protected int[][] stateToIndex2( int[] aiVariables, int iX, int iY, boolean[] abRocks ){
		int[] aiValues = new int[aiVariables.length];
		int iVarIdx = 0, iVariable = 0;
		for( iVarIdx = 0 ; iVarIdx < aiVariables.length ; iVarIdx++ ){
			iVariable = aiVariables[iVarIdx];
			if( iVariable < m_cXBits ){
				aiValues[iVarIdx] = iX % 2;
				iX /= 2;
			}
			else if( iVariable < m_cXBits + m_cYBits ){
				aiValues[iVarIdx] = iY % 2;
				iY /= 2;
			}
			else{
				if( abRocks[iVariable - m_cXBits - m_cYBits] )
					aiValues[iVarIdx] = 1;
				else
					aiValues[iVarIdx] = 0;
			}
		}
		return new int[][]{ aiVariables, aiValues };
	}
	
	protected int stateToIndex( int iX, int iY, boolean[] abRocks ){
		int iState = 0;
		int iRock = 0;
		for( iRock = m_cRocks - 1 ; iRock >= 0 ; iRock-- ){
			iState *= 2;
			if( abRocks[iRock] )
				iState += 1;
		}	
		iState = ( iState << m_cYBits ) + iY;
		iState = ( iState << m_cXBits ) + iX;
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
		int[] aiRelevant = null;
		int iStateVariable = 0;
		Action a = intToAction( iAction );
		
		if( ( a == Action.Sample ) || ( m_bReturnToStart && iAction < 4 ) ){
			aiRelevant = new int[m_cXBits + m_cYBits + m_cRocks];
			for( iStateVariable = 0 ; iStateVariable < getStateVariablesCount() ; iStateVariable++ )
				aiRelevant[iStateVariable] = iStateVariable;		
		}
		else if( a == Action.East || a == Action.West ){
			aiRelevant = new int[m_cXBits];
			for( iStateVariable = 0 ; iStateVariable < m_cXBits ; iStateVariable++ )
				aiRelevant[iStateVariable] = iStateVariable;			
		}
		else if( a == Action.North || a == Action.South ){
			aiRelevant = new int[m_cYBits];
			for( iStateVariable = 0 ; iStateVariable < m_cYBits ; iStateVariable++ )
				aiRelevant[iStateVariable] = m_cXBits + iStateVariable;
		}
		else if( a == Action.Check ){
			aiRelevant = new int[m_cXBits + m_cYBits + 1];
			for( iStateVariable = 0 ; iStateVariable < m_cXBits + m_cYBits ; iStateVariable++ )
				aiRelevant[iStateVariable] = iStateVariable;
			aiRelevant[iStateVariable] = m_cXBits + m_cYBits + iAction - 4;
		}
		return aiRelevant;
	}

	protected int[] getObservationRelevantVariables( int iAction ){
		int[] aiRelevant = null;
		int iStateVariable = 0;
		Action a = intToAction( iAction );
		if( a == Action.East || a == Action.West || a == Action.North || a == Action.South || a == Action.Sample ){
			aiRelevant = new int[0];
		}
		else if( a == Action.Check ){
			aiRelevant = new int[m_cXBits + m_cYBits + 1];
			for( iStateVariable = 0 ; iStateVariable < m_cXBits + m_cYBits ; iStateVariable++ )
				aiRelevant[iStateVariable] = iStateVariable;
			aiRelevant[iStateVariable] = m_cXBits + m_cYBits + iAction - 4;
		}
		return aiRelevant;
	}

	protected int[] getRewardRelevantVariables( int iAction ){
		int[] aiRelevant = null;
		int iStateVariable = 0;
		Action a = intToAction( iAction );
		if( a == Action.East || a == Action.West || a == Action.North || a == Action.South || a == Action.Check ){
			aiRelevant = new int[0];
		}
		else if( a == Action.Sample ){
			aiRelevant = new int[getStateVariablesCount()];
			for( iStateVariable = 0 ; iStateVariable < getStateVariablesCount() ; iStateVariable++ )
				aiRelevant[iStateVariable] = iStateVariable;
		}
		return aiRelevant;
	}

	public boolean[] indexToState( int iState ){
		boolean[] abState = new boolean[getStateVariablesCount()];
		int iVariable = 0, iBit = 0;
		boolean bNonTerminal = false;
		for( iBit = 0 ; iBit < m_cXBits ; iBit++ ){
			abState[iVariable] = ( iState % 2 == 1 );
			if( abState[iVariable] )
				bNonTerminal= true;
			iVariable++;
			iState /= 2;
		}
		for( iBit = 0 ; iBit < m_cYBits ; iBit++ ){
			abState[iVariable] = ( iState % 2 == 1 );
			if( abState[iVariable] )
				bNonTerminal= true;
			iVariable++;
			iState /= 2;
		}
		for( iBit = 0 ; iBit < m_cRocks ; iBit++ ){
			abState[iVariable] = ( iState % 2 == 1 );
			if( abState[iVariable] )
				bNonTerminal= true;
			iVariable++;
			iState /= 2;
		}
		
		return abState;
	}
	
	public int getX( int iState ){
		int iBit = 0, iX = 0, iPower = 1;
		for( iBit = 0 ; iBit < m_cXBits ; iBit++ ){
			iX += ( iState % 2 ) * iPower;
			iPower *= 2;
			iState /= 2;
		}
		return iX;
	}
	
	public int getY( int iState ){
		int iBit = 0, iY = 0, iPower = 1;
		for( iBit = 0 ; iBit < m_cXBits ; iBit++ ){
			iState /= 2;
		}
		for( iBit = 0 ; iBit < m_cYBits ; iBit++ ){
			iY += ( iState % 2 ) * iPower;
			iPower *= 2;
			iState /= 2;
		}
		return iY;
	}

	protected int getX( int[] aiVariableIds, boolean[] abVariableValues ){
		int i = 0, iX = 0;
		for( i = aiVariableIds.length - 1 ; i >= 0 ; i-- ){		
			if( aiVariableIds[i] < m_cXBits ){
				iX *= 2;
				if( abVariableValues[i] )
					iX += 1;
				else
					iX += 0;
			}
		}
		return iX;
	}
	
	protected int getY( int[] aiVariableIds, boolean[] abVariableValues ){
		int i = 0, iY = 0;
		for( i = aiVariableIds.length - 1 ; i >= 0 ; i-- ){		
			if( ( aiVariableIds[i] >= m_cXBits ) && ( aiVariableIds[i] < m_cXBits + m_cYBits ) ){
				iY *= 2;
				if( abVariableValues[i] )
					iY += 1;
				else
					iY += 0;
			}
		}
		return iY;
	}

	public boolean[] getRocks( int[] aiVariables, boolean[] abValues ){
		int iRock = 0;
		int iVar = 0;
		boolean[] abRocks = new boolean[m_cRocks];
		for( iVar = 0 ; iVar < aiVariables.length ; iVar++ ){
			iRock = aiVariables[iVar] - m_cXBits - m_cYBits;
			if( iRock >= 0 )
				abRocks[iRock] = abValues[iVar];
		}
		return abRocks;
	}
	
	public boolean[] getRocks( int iState ){
		int iBit = 0;
		boolean[] abRocks = new boolean[m_cRocks];
		for( iBit = 0 ; iBit < m_cXBits ; iBit++ ){
			iState /= 2;
		}
		for( iBit = 0 ; iBit < m_cYBits ; iBit++ ){
			iState /= 2;
		}
		for( iBit = 0 ; iBit < m_cRocks ; iBit++ ){
			abRocks[iBit] = ( iState % 2 == 1 );
			iState /= 2;
		}
		return abRocks;
	}
	
	public boolean getRock( int[] aiVariables, boolean[] abValues, int iRock ){
		int iBit = m_cXBits + m_cYBits + iRock;
		int iVar = 0;
		for( iVar = 0 ; iVar < abValues.length ; iVar++ ){
			if( aiVariables[iVar] == iBit )
				return abValues[iVar];
		}
		return false;
	}
	
	protected double moveTr( int iXStart, int iXEnd, int iYStart, int iYEnd, Action a ){
		switch( a ){
		case North: //move north
			if( iXStart != iXEnd )
				return 0.0;
			if( ( iYStart + 1 == iYEnd ) || ( ( iYStart == m_cY - 1 ) && ( iYEnd == iYStart ) ) )
				return 1.0;
			return 0.0;
		case East: //move east
			if( iYStart != iYEnd )
				return 0.0;
			if( ( iXStart + 1 == iXEnd ) || ( ( iXStart == m_cX - 1 ) && ( iXEnd == iXStart ) ) )
				return 1.0;
			return 0.0;
		case South: //move south
			if( iXStart != iXEnd )
				return 0.0;
			if( ( iYStart - 1 == iYEnd ) || ( ( iYStart == 0 ) && ( iYEnd == iYStart ) ) )
				return 1.0;
			return 0.0;
		case West: //move west
			if( iYStart != iYEnd )
				return 0.0;
			if( ( iXStart - 1 == iXEnd ) || ( ( iXStart == 0 ) && ( iXEnd == iXStart ) ) )
				return 1.0;
			return 0.0;
		}
		return -1;
	}
	

	public double tr( int iStartState, int iAction, int iEndState ){
		double dPr = 1.0;
		int iXStart = getX( iStartState ), iYStart = getY( iStartState );
		int iXEnd = getX( iEndState ), iYEnd = getY( iEndState );
		boolean[] abStartRocks = getRocks( iStartState ), abEndRocks = getRocks( iEndState );
		int iRock = 0;
		Action a = intToAction( iAction );
		
		if( ( a == Action.Check ) || ( a == Action.Sample ) ){
			if( ( iXStart != iXEnd ) || ( iYStart != iYEnd ) ){//only moves modify location
				return 0.0;
			}
			if( a == Action.Sample ){ //sample action
				for( iRock = 0 ; iRock < m_cRocks  ; iRock++ ){
					if( ( m_aiRockLocations[iRock][0] == iXStart ) && ( m_aiRockLocations[iRock][1] == iYStart ) ){
						if( abEndRocks[iRock] == true ) //after sample rock must be used
							return 0.0;
					}
					else{
						if( abEndRocks[iRock] != abStartRocks[iRock] ) //no other rock should change
							return 0.0;
					}
				}
			}
			else{
				if( sameRocks( abStartRocks, abEndRocks ) ) //check operations do not modify rocks
					return 1.0;
				return 0.0;
			}
		}
		else{ //move action
			if( !sameRocks( abStartRocks, abEndRocks ) ) //moves don't change rock state
				return 0.0;
			
			if( m_bReturnToStart && iXStart == 0 && iYStart == 0 ){
				for( iRock = 0 ; iRock < m_cRocks  ; iRock++ ){
					if( abStartRocks[iRock] ){
						if( iXEnd == 0 && iYEnd == 0 ){
							return 1.0;
						}
						else{
							return 0.0;
						}
					}
				}
			}
				
			return moveTr( iXStart, iXEnd, iYStart, iYEnd, a ); 			
		}

		return dPr;
	}
	
	protected boolean sameRocks( boolean[] abStartRocks, boolean[] abEndRocks ){
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ )
			if( abStartRocks[iRock] != abEndRocks[iRock] )
				return false;
		return true;
	}


	@Override
	public double R( int[][] aiState, int iAction ) {
		Action a = intToAction( iAction );
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iXStart = -1, iYStart = -1, iRock = 0, cGoodRocks = 0;
		if( a == Action.Sample ){
			iXStart = getX( aiVariables, abValues );
			iYStart = getY( aiVariables, abValues );
			iRock = getRockAt( iXStart, iYStart );
			if( iRock != -1 ){
				if( getRock( aiVariables, abValues, iRock ) ) {
					
					return 10.0; //sampled a good rock
				}
				else
					return -10.0;// sampled a bad rock
			}
			return -10.0;//tried to sample on an empty location
		}
		
		
		if( m_bReturnToStart ) {
			if( cGoodRocks == 0 )
			iXStart = getX( aiVariables, abValues );
			iYStart = getY( aiVariables, abValues );
			if( a == Action.South ){
				if( iXStart != 0 || iYStart != 1 )
					return 0.0;
			}
			else if( a == Action.West ){
				if( iXStart != 1 || iYStart != 0 )
					return 0.0;
			}
			else{
				return 0.0;
			}
			cGoodRocks = 0;
			for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
				if( getRock( aiVariables, abValues, iRock ) )
					cGoodRocks++;
			}
			if( cGoodRocks == 0 )
				return 10.0;
		}
		return 0.0;
	}

	public double getMaxR(){
		return 10.0;
	}
	
	public double getMinR(){
		return -10.0;
	}
	public double R( int iStartState, int iAction ){
		Action a = intToAction( iAction );
		int iXStart = -1, iYStart = -1, iRock = 0;
		boolean[] abRocks = null;
		if( a == Action.Sample ){
			iXStart = getX( iStartState );
			iYStart = getY( iStartState );
			iRock = getRockAt( iXStart, iYStart );
			if( iRock != -1 ){
				abRocks = getRocks( iStartState );
				if( abRocks[iRock] == true ) {
					return 10.0; //sampled a good rock
				}
				else
					return -10.0;// sampled a bad rock
			}
			return -10.0;//tried to sample on an empty location
		}
		return 0.0;
	}
	
	public double R( int iStartState, int iAction, int iEndState ){
		return R( iStartState, iAction ); 
	}
	
	public double R( int iStartState ){
		return -1;
	}
		
	protected int observe( int iObservation, double dProbability ){
		if( m_rndGenerator.nextDouble() < dProbability )
			return iObservation;
		else
			return 1 - iObservation;
	}
	
	public double getCheckSuccessProbabilityPrecomputed( int iX, int iY, int iRockX, int iRockY ){
		int iManhatanDistance = Math.abs( iRockX - iX ) + Math.abs( iRockY - iY );
		double dProb = 0.5;
		
		switch( iManhatanDistance ){
		case 0:
			dProb = 1;
			break;
		case 1:
			dProb = 1;
			break;
		case 2:
			dProb = 1;
			break;
		case 3:
			dProb = 0.95;
			break;		
		case 4:
			dProb = 0.95;
			break;		
		case 5:
			dProb = 0.85;
			break;		
		case 6:
			dProb = 0.85;
			break;		
		case 7:
			dProb = 0.75;
			break;		
		case 8:
			dProb = 0.75;
			break;		
		}
		
		return dProb;
	}
	
	public double getCheckSuccessProbability(int iX, int iY, int iRockX, int iRockY ){

		double distance = Math.sqrt(Math.pow(Math.abs(iX-iRockX),2) + Math.pow(Math.abs(iY-iRockY),2));		
		double efficiency = Math.exp(-(Math.log(2.0) * distance/sensorHalfDistance));
	    return 0.5 + efficiency/2.0;	
	    
	}
	protected double getCheckSuccessProbability( int iState, int iRock ){
		int iX = getX( iState ), iY = getY( iState );
		int iRockX = m_aiRockLocations[iRock][0];
		int iRockY = m_aiRockLocations[iRock][1];
		return getCheckSuccessProbability( iX, iY, iRockX, iRockY );
	}
	
	protected double getCheckSuccessProbability( int[] aiVariables, boolean[] abValues, int iRock ){
		int iX = getX( aiVariables, abValues ), iY = getY( aiVariables, abValues );
		int iRockX = m_aiRockLocations[iRock][0];
		int iRockY = m_aiRockLocations[iRock][1];
		return getCheckSuccessProbability( iX, iY, iRockX, iRockY );
	}
	

	@Override
	public int observe(int iAction, int[][] aiEndState) {
		int[] aiVariables = aiEndState[0];
		boolean[] abEndValues = toBool( aiEndState[1] );
		Action a = intToAction( iAction );
		double dPr = 1.0;
		int iValue = 0;
		if( a == Action.Check ){
			int iRock = iAction - 4;
			dPr = getCheckSuccessProbability( aiVariables, abEndValues, iRock );
			if( getRock( aiVariables, abEndValues, iRock ) )
				iValue = 1;
			else
				iValue = 0;
		}
		return observe( iValue, dPr );
	}

	
	public int observe( int iAction, int iEndState ){
		Action a = intToAction( iAction );
		double dPr = 1.0;
		int iValue = 0;
		if( a == Action.Check ){
			int iRock = iAction - 4;
			boolean[] abRocks = getRocks( iEndState );
			dPr = getCheckSuccessProbability( iEndState , iRock );
			if( abRocks[iRock] == true )
				iValue = 1;
			else
				iValue = 0;
		}
		return observe( iValue, dPr );
	}
	


	
	public double O( int iAction, int iEndState, int iObservation ){
		Action a = intToAction( iAction );
		double dPr = 0.5;
		if( a == Action.Check ){
			int iRock = iAction - 4;
			boolean[] abRocks = getRocks( iEndState );
			dPr = getCheckSuccessProbability( iEndState , iRock );
			if( abRocks[iRock] == true ){
				if( iObservation == 1 )
					return dPr;
				else
					return 1 - dPr;
			}
			else{
				if( iObservation == 0 )
					return dPr;
				else
					return 1 - dPr;
			}			
		}
		else{
			if( iObservation == 0 )
				dPr = 1.0;
			else
				dPr = 0.0;
			
		}
		return dPr;
	}
	
	public Iterator<Entry<Integer,Double>> getNonZeroTransitions( int iStartState, int iAction ) {
		Map<Integer,Double> mTransitions = null;
		int iEndState = 0;
		double dTr = 0.0;
		mTransitions = new TreeMap<Integer,Double>();
		iEndState = execute( iAction, iStartState );
		dTr = tr( iStartState, iAction, iEndState );
		mTransitions.put( iEndState, dTr );
		return mTransitions.entrySet().iterator();
	}


	@Override
	public int[][] chooseStartState2() {
		int iX = m_iInitialX, iY = m_iInitialY, iRock = 0;
		boolean[] abRocks = new boolean[m_cRocks];
		int cGoodRocks = m_rndGenerator.nextInt( m_cRocks + 1 );
		while( cGoodRocks > 0 ){
			do{
				iRock = m_rndGenerator.nextInt( m_cRocks );
			}while( abRocks[iRock] == true );//find a bad rock
			abRocks[iRock] = true;
			cGoodRocks--;
		}
		
		return stateToIndex2( iX, iY, abRocks );
	}
	
	//begin at leftmost cloumn in the middle with at least one good rock
	public int chooseStartState(){
		int iX = m_iInitialX, iY = m_iInitialY, iRock = 0;
		boolean[] abRocks = new boolean[m_cRocks];
		int cGoodRocks = m_rndGenerator.nextInt( m_cRocks + 1 );
		while( cGoodRocks > 0 ){
			do{
				iRock = m_rndGenerator.nextInt( m_cRocks );
			}while( abRocks[iRock] == true );//find a bad rock
			abRocks[iRock] = true;
			cGoodRocks--;
		}
		
		return stateToIndex( iX, iY, abRocks );
	}

	
	public double probStartState( int iState ){
		int iX = getX( iState );
		int iY = getY( iState );
		if( ( iX == m_iInitialX ) && ( iY == m_iInitialY ) )
			return 1 / Math.pow( 2, m_cRocks );
		return 0.0;
	}
	
	protected int getRockAt( int iX, int iY ){
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ )
			if( ( m_aiRockLocations[iRock][0] == iX ) && ( m_aiRockLocations[iRock][1] == iY ) )
				return iRock;
		return -1;
	}
	
	

	@Override
	public int[][] execute( int iAction, int[][] aiState ) {
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iX = getX( aiVariables, abValues ), iY = getY( aiVariables, abValues );
		boolean[] abRocks = getRocks( aiVariables, abValues );
		Action a = intToAction( iAction );
		
		
		
		boolean bStartLocation = ( iX == m_iInitialX ) && ( iY == m_iInitialY );
		boolean bGoodRocks = false;
		for( boolean b : abRocks ){
			bGoodRocks |= b;
		}
		
		if( a == Action.Check ){
			return aiState;			
		}
		else if( a == Action.Sample ){
			int iRock = getRockAt( iX, iY );
			if( iRock != -1 )
				abRocks[iRock] = false;			
		}
		else if( m_bReturnToStart && !bGoodRocks && bStartLocation ){
			//do not change x and y - cannot move from end state
			return aiState;			
		}
		else{
			if( a == Action.North ){
				if( iY < m_cY - 1 )
					iY++;
			}
			else if( a == Action.South ){
				if( iY > 0 )
					iY--;				
			}
			else if( a == Action.East ){
				if( iX < m_cX - 1 )
					iX++;
			}
			else if( a == Action.West ){
				if( iX > 0 )
					iX--;
			}
		}

		int[][] aiNextState = stateToIndex2( aiVariables, iX, iY, abRocks );
		return aiNextState;
	}

	
	public int execute( int iAction, int iState ){
		int iX = getX( iState ), iY = getY( iState );
		boolean[] abRocks = getRocks( iState );
		Action a = intToAction( iAction );
		boolean bStartLocation = ( iX == m_iInitialX ) && ( iY == m_iInitialY );
		boolean bGoodRocks = false;
		for( boolean b : abRocks ){
			bGoodRocks |= b;
		}
		
		if( a == Action.Check ){
			return iState;			
		}
		else if( a == Action.Sample ){
			int iRock = getRockAt( iX, iY );
			if( iRock != -1 )
				abRocks[iRock] = false;			
		}
		else if( m_bReturnToStart && !bGoodRocks && bStartLocation ){
			//do not change x and y - cannot move from end state
			return iState;			
		}
		else{
			if( a == Action.North ){
				if( iY < m_cY - 1 )
					iY++;
			}
			else if( a == Action.South ){
				if( iY > 0 )
					iY--;				
			}
			else if( a == Action.East ){
				if( iX < m_cX - 1 )
					iX++;
			}
			else if( a == Action.West ){
				if( iX > 0 )
					iX--;
			}
		}
		int iNextState = stateToIndex( iX, iY, abRocks );
		return iNextState;
	}
	
	public boolean isTerminalState( int[][] aiState ){
		int iRock = 0;
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			if( getRock( aiVariables, abValues, iRock ) )
				return false;
		}
		if( m_bReturnToStart ){
			if( getX( aiVariables, abValues ) == 0 && getY( aiVariables, abValues ) == 0 )
				return true;
			return false;
		}
		return true;
	}
	
	public boolean isTerminalState( int iState ){
		if( m_bReturnToStart )
			return iState == 0;
		int cActiveRocks = iState >> ( m_cXBits + m_cYBits );
		if( cActiveRocks == 0 )
			return true;
		return false;
	}
	
	public boolean terminalStatesDefined(){
		return true;
	}
	
	public double getMaxMinR(){
		return 0.0;
	}

	public int getStartStateCount() {
		return (int)Math.pow( 2, m_cRocks ) - 1 /*at least one good rock */;
	}

	@Override
	public String getStateName(int[][] aiState) {
		int[] aiVariables = aiState[0];
		boolean[] abValues = toBool( aiState[1] );
		int iX = getX( aiVariables, abValues ), iY = getY( aiVariables, abValues );
		boolean[] abRocks = getRocks( aiVariables, abValues );
		String sState = "<x=" + iX + ",y=" + iY + ",[";
		for( boolean bRock: abRocks ){
			if( bRock )
				sState += "G";
			else
				sState += "B";
		}
		if( isTerminalState( aiState ) )
			sState += "](T)>";
		else
			sState += "]>";
		return sState;
	}

	public String getStateName( int iState ){
		int iX = getX( iState ), iY = getY( iState );
		boolean[] abRocks = getRocks( iState );
		String sState = "<x=" + iX + ",y=" + iY + ",[";
		for( boolean bRock: abRocks ){
			if( bRock )
				sState += "G";
			else
				sState += "B";
		}
		if( isTerminalState( iState ) )
			sState += "](T)>";
		else
			sState += "]>";
		return sState;
	}
	
	public String getActionName( int iAction ){
		String sActionName = intToAction( iAction ).toString();
		if( sActionName.equals( "Check" ) ){
			sActionName += "Rock" + ( iAction - 4 );
		}
		return sActionName;
	}
	

	protected static int[][] computeRockLocations( int cX, int cY, int cRocks ) {
		int[][] aiRockLocations = new int[cRocks][2];
		Random rnd = new Random( 0 );
		boolean bEmptySquare = false;
		int iRock = 0, iX = 0, iY = 0, iPreviousRock = 0;
		for( iRock = 0 ; iRock < cRocks ; iRock++ ){
			bEmptySquare = false;
			while( !bEmptySquare ){
				iX = rnd.nextInt( cX );
				iY = rnd.nextInt( cY );
				bEmptySquare = true;
				for( iPreviousRock = 0 ; iPreviousRock < iRock ; iPreviousRock++ )
					if( ( aiRockLocations[iPreviousRock][0] == iX ) && ( aiRockLocations[iPreviousRock][1] == iY ) )
						bEmptySquare = false;
			}
			aiRockLocations[iRock][0] = iX;
			aiRockLocations[iRock][1] = iY;
			Logger.getInstance().logln( "Rock " + iRock + " at " + iX + ", " + iY );
		}
		return aiRockLocations;
	}


	public String getObservationName( int iObservation ) {
		if( iObservation == 1 )
			return "Good";
		else
			return "Bad";
	}

	
	private boolean getBit( int iNumber, int iDigit ){
		iNumber = iNumber >> iDigit;
		return iNumber % 2 == 1;
	}
	
	
	public double observationGivenRelevantVariables( int iAction, int iObservation, int[] aiRelevantVariables, boolean[] abValues ) {
		int iX = 0, iY = 0;
		int iRock = 0;
		Action a = intToAction( iAction );
		
		if( iAction < 4 ){ //move action
			if( iObservation == 0 )
				return 1.0;
			else
				return 0.0;
		}
		
		if( a == Action.Check ){
			if( a == Action.Check ){
				iRock = iAction - 4;
				iX = getX( aiRelevantVariables, abValues );
				iY = getY( aiRelevantVariables, abValues );
				int iRockX = m_aiRockLocations[iRock][0];
				int iRockY = m_aiRockLocations[iRock][1];
				double dPr = getCheckSuccessProbability( iX, iY, iRockX, iRockY );
						
				for( int i = 0 ; i < aiRelevantVariables.length ; i++ ){
					if( aiRelevantVariables[i] == m_cXBits + m_cYBits + iRock ){
						if( abValues[i] == true ){
							if( iObservation == 1 )
								return dPr;
							else
								return 1 - dPr;
						}
						else{
							if( iObservation == 0 )
								return dPr;
							else
								return 1 - dPr;
						}			
					}
				}
			}
		}
		
		if( a == Action.Sample ){
			if( iObservation == 0 )
				return 1.0;
			else
				return 0.0;
		}

		return 0.0;
	}	
	
	private boolean hasSameValue( int iVariable, boolean bValue, int[] aiRelevantVariables, boolean[] abValues ) {
		for( int i = 0 ; i < aiRelevantVariables.length ; i++ ){
			if( aiRelevantVariables[i] == iVariable ){
				if( bValue == abValues[i] )
					return true;
				return false;
			}
		}
		return true;
	}

	
	public int[] getIndependentComponentVariables( int iComponent ) {
		int[] aiVariables = null;
		int i = 0;
		if( iComponent == 0 ){
			aiVariables = new int[m_cXBits];
			for( i = 0 ; i < m_cXBits ; i++ ){
				aiVariables[i] = i;
			}
		}
		else if( iComponent == 1 ){
			aiVariables = new int[m_cYBits];
			for( i = 0 ; i < m_cYBits ; i++ ){
				aiVariables[i] = m_cXBits + i;
			}
		}
		else{
			aiVariables = new int[]{ m_cXBits + m_cYBits + iComponent - 2 };
		}
		return aiVariables;
	}

	
	public int getIndependentComponentsCount() {
		return m_cRocks + 2;
	}

	
	public double getInitialComponentValueProbability( int iComponent, int iValue ){
		if( iComponent == 0 ){
			if( iValue == m_iInitialX )
				return 1.0;
			return 0.0;
		}
		else if( iComponent == 1 ){
			if( iValue == m_iInitialY )
				return 1.0;
			return 0.0;
		}
		else{
			return 0.5;
		}
	}
	
	
	public double transitionGivenRelevantVariables( int iAction, int[] aiComponent, boolean[] abComponentValues, 
													int[] aiRelevantVariables, boolean[] abRelevantValues ){
		int iX = 0, iY = 0, iVariable = 0;
		int iRock = 0;
		Action a = intToAction( iAction );
		
		if( iAction < 4 ){ //move action
			if( ( a == Action.North ) || ( a == Action.South ) ){
				iY = getY( aiRelevantVariables, abRelevantValues );
				if( a == Action.North ){
					if( iY < m_cY - 1 )
						iY++;
				}
				if( a == Action.South ){
					if( iY > 0 )
						iY--;
				}
				for( iVariable = 0 ; iVariable < aiComponent.length ; iVariable++ ){
					if( ( aiComponent[iVariable] >= m_cXBits ) && ( aiComponent[iVariable] < m_cXBits + m_cYBits ) ){
						if( getBit( iY, iVariable ) != abComponentValues[iVariable] )
							return 0.0;
					}
					else{
						if( !hasSameValue( aiComponent[iVariable], abComponentValues[iVariable], 
								aiRelevantVariables, abRelevantValues ) )
							return 0.0;
					}
				}
				return 1.0;
			}
			if( ( a == Action.West ) || ( a == Action.East ) ){
				iX = getX( aiRelevantVariables, abRelevantValues );
				if( a == Action.East ){
					if( iX < m_cX - 1 )
						iX++;
				}
				if( a == Action.West ){
					if( iX > 0 )
						iX--;
				}
				for( iVariable = 0 ; iVariable < aiComponent.length ; iVariable++ ){
					if( aiComponent[iVariable] < m_cXBits ){
						if( getBit( iX, iVariable ) != abComponentValues[iVariable] )
							return 0.0;
					}
					else{
						if( !hasSameValue( aiComponent[iVariable], abComponentValues[iVariable], 
								aiRelevantVariables, abRelevantValues ) )
							return 0.0;
					}
				}
				return 1.0;
			}
		}
		
		if( a == Action.Check ){
			for( iVariable = 0 ; iVariable < aiComponent.length ; iVariable++ ){
				if( !hasSameValue( aiComponent[iVariable], abComponentValues[iVariable], 
						aiRelevantVariables, abRelevantValues ) )
					return 0.0;
			}
			return 1.0;
		}
		
		if( a == Action.Sample ){
			for( iVariable = 0 ; iVariable < aiComponent.length ; iVariable++ ){
				if( aiComponent[iVariable] < m_cXBits + m_cYBits ){
					if( !hasSameValue( aiComponent[iVariable], abComponentValues[iVariable], 
							aiRelevantVariables, abRelevantValues ) )
						return 0.0;
				}
				else{
					iRock = aiComponent[iVariable] - m_cXBits - m_cYBits;
					iX = getX( aiRelevantVariables, abRelevantValues );
					iY = getY( aiRelevantVariables, abRelevantValues );
					if( ( m_aiRockLocations[iRock][0] == iX ) && ( m_aiRockLocations[iRock][1] == iY ) ){
						if( abComponentValues[iVariable] == true )
							return 0.0;
					}
					else{
						if( !hasSameValue( aiComponent[iVariable], abComponentValues[iVariable], 
								aiRelevantVariables, abRelevantValues ) )
							return 0.0;
					}					
				}
			}
			return 1.0;
		}

		return 0.0;
	}

	
	public boolean changingComponent( int iComponent, int iAction, int iObservation ){
		Action a = intToAction( iAction );
		
		if( ( a == Action.West ) || ( a == Action.East ) ){
			return iComponent == 0;
		}
		if( ( a == Action.North ) || ( a == Action.South ) ){
			return iComponent == 1;
		}
		if( a == Action.Check ){
			return false;
		}
		if( a == Action.Sample ){
			return iComponent >= 2;
		}
		return false;
	}

	
	public int[] getRelevantComponents( int iAction, int iObservation ){
		Action a = intToAction( iAction );
		if( ( a == Action.West ) || ( a == Action.East ) ){
			return new int[]{ 0 };
		}
		if( ( a == Action.North ) || ( a == Action.South ) ){
			return new int[]{ 1 };
		}
		if( a == Action.Check ){
			int iRock = iAction - 4;
			return new int[]{ 0, 1, iRock + 2 };
		}
		if( a == Action.Sample ){
			int[] aiComponents = new int[m_cRocks + 2];
			for( int i = 0 ; i < m_cRocks + 2 ; i++ ){
				aiComponents[i] = i;
			}
			return aiComponents;
		}
		return null;
	}

	
	public int[] getRelevantVariablesForComponent( int iAction, int iComponent ){
		Action a = intToAction( iAction );
		if( ( a == Action.West ) || ( a == Action.East ) || ( a == Action.North ) || ( a == Action.South ) ){
			return getRelevantVariables( iAction );
		}
		if( a == Action.Check ){
			return getRelevantVariables( iAction );
		}
		if( a == Action.Sample ){
			int iRock = iComponent - 2, i = 0;
			int[] aiVariables = null;
			if( iRock >= 0 )
				aiVariables = new int[m_cXBits + m_cYBits + 1];
			else
				aiVariables = new int[m_cXBits + m_cYBits];				
			for( i = 0 ; i < m_cXBits + m_cYBits ; i++ ){
				aiVariables[i] = i;
			}
			if( iRock >= 0 )
				aiVariables[i] = iRock + m_cXBits + m_cYBits;
			return aiVariables;
		}
		return null;
	}

	public int[] getRelevantComponentsForComponent( int iAction, int iComponent ) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected double computeImmediateReward( BeliefState bs, int iAction ){
		if( bs instanceof ModifiedRockSampleBeliefState){
			ModifiedRockSampleBeliefState mbs = (ModifiedRockSampleBeliefState)bs;
			return mbs.computeImmediateReward( iAction );
		}
		return super.computeImmediateReward( bs, iAction );
	}
	

//	public BeliefState newBeliefState(){
//		return new ModifiedRockSampleBeliefState( this );
//	}

	
	protected void learnReward( int iAction ){
		if( iAction < m_cActions - 1 ){
			super.learnReward( iAction );
			return;
		}
		super.learnReward( iAction );
		AlgebraicDecisionDiagram addReward = newAlgebraicDecisionDiagram( m_cStateVariables, true );
		int iRock = 0, iX = 0, iY = 0;
		int[] aiVariables = new int[m_cXBits + m_cYBits + 1];
		boolean[] abValues = new boolean[m_cXBits + m_cYBits + 1];
		int iBit = 0;
		for( iBit = 0 ; iBit < m_cXBits + m_cYBits ; iBit++ ){
			aiVariables[iBit] = iBit;
		}
		abValues[m_cXBits + m_cYBits] = true;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			aiVariables[m_cXBits + m_cYBits] = m_cXBits + m_cYBits + iRock;
			iX = m_aiRockLocations[iRock][0];
			iY = m_aiRockLocations[iRock][1];
			for( iBit = 0 ; iBit < m_cXBits ; iBit++ ){
				abValues[iBit] = iX % 2 == 1;
				iX /= 2;
			}
			for( iBit = 0 ; iBit < m_cYBits ; iBit++ ){
				abValues[iBit + m_cXBits] = iY % 2 == 1;
				iY /= 2;
			}
			addReward.addPartialPath( aiVariables, abValues, 10.0, false );
		}
		
		addReward.finalizePaths( -10.0 );
		addReward.reduce();
		m_adReward[iAction] = addReward;
		Logger.getInstance().logFull( "ModifiedRockSample", 0, "learnReward", "Done learning reward for action " + getActionName( iAction ) +
				" vertexes " + addReward.getVertexCount() );
	}
	
	
	public void createPartialActionDiagrams(){
		if( m_btFactored != BeliefType.Independent ){
			super.createPartialActionDiagrams();
		}
	}
	protected void learnIndependentCompoenentDiagrams() {
	}
	public void learnObservations(){
	}

	public int getInitialX() {
		return m_iInitialX;
	}

	public int getInitialY() {
		return m_iInitialY;
	}

	public int getRocksCount() {
		return m_cRocks;
	}

	public int getXBits() {
		return m_cXBits;
	}

	public int getYBits() {
		return m_cYBits;
	}

	public int getMaxY() {
		return m_cX;
	}

	public int getMaxX() {
		return m_cY;
	}

	public int getRockXLocation(int iRock) {
		return m_aiRockLocations[iRock][0];
	}

	public int getRockYLocation(int iRock) {
		return m_aiRockLocations[iRock][1];
	}
}
