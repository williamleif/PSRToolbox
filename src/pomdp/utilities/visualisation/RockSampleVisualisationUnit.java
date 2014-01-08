package pomdp.utilities.visualisation;

import pomdp.environments.ModifiedRockSample;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;

public class RockSampleVisualisationUnit extends VisualisationUnit {
	private ModifiedRockSample m_pRockSamplePOMDP;
	
	public RockSampleVisualisationUnit(POMDP pomdp) {
		super(pomdp);
		m_pRockSamplePOMDP = (ModifiedRockSample)pomdp;
	}

	@Override
	public void Show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void Hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void UpdateState(BeliefState bsCurrent, int iTrueState) {
		
		int iMaxX = m_pRockSamplePOMDP.getMaxX();
		int iMaxY = m_pRockSamplePOMDP.getMaxY();
		int iX = m_pRockSamplePOMDP.getX(iTrueState);
		int iY = m_pRockSamplePOMDP.getY(iTrueState);
		boolean[] abRockStates = m_pRockSamplePOMDP.getRocks( iTrueState );
		int iRock = 0;
		int iRockX = m_pRockSamplePOMDP.getRockXLocation(iRock);
		int iRockY = m_pRockSamplePOMDP.getRockYLocation(iRock);
	}
}
