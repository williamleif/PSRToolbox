package pomdp.environments;

import pomdp.utilities.Logger;

public class FieldVisionRockSample extends ModifiedRockSample {
	
	private static final long serialVersionUID = 7526472295622776147L;
	
	public FieldVisionRockSample( int cX, int cY, int cRocks, int halfSensorDistance, BeliefType btFactored ){
		super( cX, cY, cRocks, halfSensorDistance, computeRockLocations( cX, cY, cRocks ), btFactored, false, true );
		
		m_cActions = 5; //north east south west + sample 
		m_cObservations = (int)Math.pow(2,m_cRocks);	
	}
	
	public String getName(){
		return "FieldVisionRockSample" + m_cX + "_" + m_cY + "_" + m_cRocks;
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
		//if( iAction >= 4 && iAction < m_cRocks + 4 )
		//	return Action.Check;
		if( iAction == 4 )
			return Action.Sample;
		Logger.getInstance().logln(iAction);
		return null;
	}
	

	
	
	@Override
	public int observe(int iAction, int[][] aiEndState) {
		Action a = intToAction( iAction );
		double dPr = 1.0;
		int iValue = 0;
		
		int[] rockObservations = new int[m_cRocks];
		int[] aiVariables = aiEndState[0];
		boolean[] abEndValues = toBool( aiEndState[1] );
		
		
		for (int rock = 0; rock < m_cRocks; rock++)
		{
			dPr = getCheckSuccessProbability( aiVariables, abEndValues, rock );
			if(getRock( aiVariables, abEndValues, rock))
				iValue = 1;
			else
				iValue = 0;
			rockObservations[rock] = observe( iValue, dPr);
		}

		return createObservationFromRockObservations(rockObservations);
	}
	
	
	
	
	
	public int observe(int iAction, int iEndState){
		Action a = intToAction( iAction );
		double dPr = 1.0;
		int iValue = 0;
		
		boolean[] abRocks = getRocks( iEndState );
		int[] rockObservations = new int[m_cRocks];
		for (int rock = 0; rock < m_cRocks; rock++)
		{

			dPr = getCheckSuccessProbability(iEndState, rock);
			if( abRocks[rock] == true )
				iValue = 1;
			else
				iValue = 0;
			
			rockObservations[rock] = observe( iValue, dPr);
		}
			return createObservationFromRockObservations(rockObservations);
	}





	public double O( int iAction, int iEndState, int iObservation ){
		
		double totalProb = 1.0;
		boolean[] stateRocks = getRocks( iEndState );
		boolean[] ORocks = getRocksFromO( iObservation );
		for (int rock = 0; rock < m_cRocks; rock++)
		{
			double rockProb = getCheckSuccessProbability( iEndState , rock);
			
			/* if the observation has rock flipped, then flip probability */
			if (stateRocks[rock] != ORocks[rock])
				rockProb = 1.0 - rockProb;
			
			totalProb *= rockProb;
			
		}
		return totalProb;
	}
	

	
	
	public boolean[] getRocksFromO(int o){
		int iBit = 0;
		boolean[] abRocks = new boolean[m_cRocks];
		
		for( iBit = 0 ; iBit < m_cRocks ; iBit++ ){
			abRocks[iBit] = ( o % 2 == 1 );
			o /= 2;
		}
		return abRocks;
	}
	
	public int createObservationFromRockObservations(int[] rockObs)
	{
		int O = 0;

		for(int iBit = m_cRocks - 1; iBit >= 0; iBit--){
			O += rockObs[iBit];
			O *=2;
		}
		/* last one does not need to double */
		O /= 2;
		return O;
	}
	
	
	/* not implemented */
	public int[] getRelevantVariables( int iAction ){
		return null;
	}

	protected int[] getObservationRelevantVariables( int iAction ){
		return null;
	}
	
	protected int[] getRewardRelevantVariables( int iAction ){
		return null;
	}
	public String getObservationName( int iObservation ) {
		return "";
	}
	
	public double observationGivenRelevantVariables( int iAction, int iObservation, int[] aiRelevantVariables, boolean[] abValues ) {
		return 0;
	}
	
	public double transitionGivenRelevantVariables( int iAction, int[] aiComponent, boolean[] abComponentValues, 
			int[] aiRelevantVariables, boolean[] abRelevantValues ){
		return 0;
	}

	public int[] getRelevantVariablesForComponent( int iAction, int iComponent ){
		return null;
	}

}
