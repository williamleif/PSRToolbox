package pomdp.utilities.visualisation;

import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;

public abstract class VisualisationUnit {
	protected POMDP m_pPOMDP;
	public VisualisationUnit(POMDP pomdp){
		m_pPOMDP = pomdp;
	}
	
	public abstract void Show();
	public abstract void Hide();
	public abstract void UpdateState(BeliefState bsCurrent, int iTrueState);
}
