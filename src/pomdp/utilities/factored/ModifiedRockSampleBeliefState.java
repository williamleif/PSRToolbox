package pomdp.utilities.factored;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import pomdp.environments.ModifiedRockSample;
import pomdp.environments.ModifiedRockSample.Action;
import pomdp.utilities.BeliefState;

public class ModifiedRockSampleBeliefState extends BeliefState {
	private ModifiedRockSample m_mrsPOMDP;
	
	private int m_iX, m_iY, m_cRocks;
	private double[] m_adRockProbabilities;
	
	public ModifiedRockSampleBeliefState( ModifiedRockSample mrsPOMDP ) {
		super( mrsPOMDP.getStateCount(), mrsPOMDP.getActionCount(), mrsPOMDP.getObservationCount(), -1, mrsPOMDP.getBeliefStateFactory().isCachingBeliefStates(), null );
		m_mrsPOMDP = mrsPOMDP;
		m_iX = mrsPOMDP.getInitialX();
		m_iY = mrsPOMDP.getInitialY();
		m_adRockProbabilities = new double[mrsPOMDP.getRocksCount()];
		m_cRocks = mrsPOMDP.getRocksCount();
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			m_adRockProbabilities[iRock] = 0.5;
		}
	}
	
	public ModifiedRockSampleBeliefState( ModifiedRockSampleBeliefState bs ) {
		this( bs.m_mrsPOMDP );
		m_iX = bs.m_iX;
		m_iY = bs.m_iY;
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			m_adRockProbabilities[iRock] = bs.m_adRockProbabilities[iRock];
		}
	}

	public double[] getVariableProbabilities(){
		double[] adVariableProbabilities = new double[m_mrsPOMDP.getStateVariablesCount()];
		int iBit = 0, iCurrentBit = 0;
		for( iBit = 0 ; iBit < m_mrsPOMDP.getXBits() ; iBit++ ){
			if( ( m_iX >> iBit ) % 2 == 1 ){
				adVariableProbabilities[iCurrentBit] = 1.0;
			}
			iCurrentBit++;
		}
		for( iBit = 0 ; iBit < m_mrsPOMDP.getYBits() ; iBit++ ){
			if( ( m_iY >> iBit ) % 2 == 1 ){
				adVariableProbabilities[iCurrentBit] = 1.0;
			}
			iCurrentBit++;
		}
		for( iBit = 0 ; iBit < m_cRocks ; iBit++ ){
			adVariableProbabilities[iCurrentBit] = m_adRockProbabilities[iBit];
			iCurrentBit++;
		}
		
		return adVariableProbabilities;
	}

	public double valueAt( int iState ){
		int iX = m_mrsPOMDP.getX( iState );
		int iY = m_mrsPOMDP.getY( iState );
		boolean[] abRocks = m_mrsPOMDP.getRocks( iState );
		double dProb = 1.0;
		if( iX != m_iX )
			return 0.0;
		if( iY != m_iY )
			return 0.0;
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			if( abRocks[iRock] )
				dProb *= m_adRockProbabilities[iRock];
			else
				dProb *= 1 - m_adRockProbabilities[iRock];
		}
		return dProb;
	}
	

	protected double applyActionAndObservation( int iAction, int iObservation ){
		ModifiedRockSample.Action a = m_mrsPOMDP.intToAction( iAction );
		double dProbOGivenA = 1.0;
		if( a == ModifiedRockSample.Action.North ){
			if( m_iY < m_mrsPOMDP.getMaxY() )
				m_iY++;
			if( iObservation == 1 )
				dProbOGivenA = 0.0;
		}
		if( a == ModifiedRockSample.Action.South ){
			if( m_iY > 0 )
				m_iY--;
		}
		if( a == ModifiedRockSample.Action.East ){
			if( m_iX < m_mrsPOMDP.getMaxX() )
				m_iX++;
			if( iObservation == 1 )
				dProbOGivenA = 0.0;
		}
		if( a == ModifiedRockSample.Action.West ){
			if( m_iX > 0 )
				m_iX--;
		}
		if( a == ModifiedRockSample.Action.Check ){
			int iRock = iAction - 4;
			int iRockX = m_mrsPOMDP.getRockXLocation( iRock );
			int iRockY = m_mrsPOMDP.getRockYLocation( iRock );
			double dPr = m_mrsPOMDP.getCheckSuccessProbability( m_iX, m_iY, iRockX, iRockY );
			double dProbGivenTrue = m_adRockProbabilities[iRock];
			double dProbGivenFalse = 1 - m_adRockProbabilities[iRock];			
			if( iObservation == 1 ){
				dProbGivenTrue *= dPr;
				dProbGivenFalse *= ( 1 - dPr );
			}
			else{
				dProbGivenTrue *= ( 1 - dPr );
				dProbGivenFalse *= dPr;
			}
			m_adRockProbabilities[iRock] = 	dProbGivenTrue / ( dProbGivenTrue + dProbGivenFalse );	
			dProbOGivenA = dProbGivenTrue + dProbGivenFalse;
		}
		if( a == ModifiedRockSample.Action.Sample ){
			int iRock = 0;
			int iRockX = 0;
			int iRockY = 0;
			for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
				iRockX = m_mrsPOMDP.getRockXLocation( iRock );
				iRockY = m_mrsPOMDP.getRockYLocation( iRock );
				if( ( iRockX == m_iX ) && ( iRockY == m_iY ) ){
					m_adRockProbabilities[iRock] = 0.0;
				}				
			}
			if( iObservation == 1 )
				dProbOGivenA = 0.0;
		}
		return dProbOGivenA;
	}
	
	public BeliefState nextBeliefState( int iAction, int iObservation ){	
		ModifiedRockSampleBeliefState bsNew = new ModifiedRockSampleBeliefState( this );
		bsNew.applyActionAndObservation( iAction, iObservation );
		return bsNew;
	}
	
	/**(non-Javadoc)
	 * @see pomdp.utilities.BeliefState#probabilityOGivenA(int, int)
	 */
	public double probabilityOGivenA( int iAction, int iObservation ){	
		ModifiedRockSampleBeliefState bsNew = new ModifiedRockSampleBeliefState( this );
		double dProbGivenAandO = bsNew.applyActionAndObservation( iAction, iObservation );
		return dProbGivenAandO;
	}
	
	public Collection<Entry<Integer,Double>> getNonZeroEntries(){
		return null;
	}
	
	public boolean equals( ModifiedRockSampleBeliefState ibs ){
		if( m_iX != ibs.m_iX )
			return false;
		if( m_iY != ibs.m_iY )
			return false;
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			if( Math.abs( m_adRockProbabilities[iRock] - ibs.m_adRockProbabilities[iRock] ) > 0.0001 )
				return false;
		}
		return true;
	}
	
	public boolean equals( Object oOther ){
		if( oOther == this )
			return true;
		if( oOther instanceof ModifiedRockSampleBeliefState ){
			ModifiedRockSampleBeliefState ibs = (ModifiedRockSampleBeliefState)oOther;
			return equals( ibs );
		}
		return false;
	}

	public double computeImmediateReward( int iAction ) {
		ModifiedRockSample.Action a = m_mrsPOMDP.intToAction( iAction );
		double dReward = 0.0;
		if( a == ModifiedRockSample.Action.Sample ){
			int iRock = 0;
			int iRockX = 0;
			int iRockY = 0;
			for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
				iRockX = m_mrsPOMDP.getRockXLocation( iRock );
				iRockY = m_mrsPOMDP.getRockYLocation( iRock );
				if( ( iRockX == m_iX ) && ( iRockY == m_iY ) ){
					return m_adRockProbabilities[iRock] * 10.0 + ( 1 - m_adRockProbabilities[iRock] ) * -10.0;
				}				
			}
		}
		return -10.0;
	}
	
	public String toString(){
		String sBelief = "[<" + m_iX + "," + m_iY + ">, ", sValue = "";
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			sValue = m_adRockProbabilities[iRock] + "";
			if( sValue.length() > 4 )
				sValue = sValue.substring( 0, 4 );
			sBelief += "R" + iRock + "=" + sValue + ",";
		}
		sBelief += "]";
		return sBelief;
	}

	@Override
	public void clearZeroEntries() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int countEntries() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Entry<Integer, Double>> getDominatingNonZeroEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNonZeroEntriesCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setValueAt(int state, double value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	public void initRandomValues() {
		m_iX = m_mrsPOMDP.getRandomGenerator().nextInt( m_mrsPOMDP.getMaxX() );
		m_iY = m_mrsPOMDP.getRandomGenerator().nextInt( m_mrsPOMDP.getMaxY() );
		int iRock = 0;
		for( iRock = 0 ; iRock < m_cRocks ; iRock++ ){
			m_adRockProbabilities[iRock] = m_mrsPOMDP.getRandomGenerator().nextDouble();
		}
	}
}
