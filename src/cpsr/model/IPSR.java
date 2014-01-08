package cpsr.model;

import java.util.HashSet;

import cpsr.environment.DataSet;
import cpsr.environment.components.Action;
import cpsr.environment.components.ActionObservation;
import cpsr.stats.PSRObserver;

public interface IPSR {

	/**
	 * Returns boolean specifying whether the PSR has been built.
	 * 
	 * @return Boolean specifying whether the PSR has been built. 
	 */
    boolean isBuilt();
	
	void build();

	/**
	 * Returns DataSet associated with the PSR.
	 * 
	 * @return DataSet associated with the PSR. 
	 */
	DataSet getDataSet();

	/**
	 * Updates the PSR representation using action-observation pair. 
	 * 
	 * @param ao Action-observation pair used in update
	 */
	void update(ActionObservation ao);

	/**
	 * Resets PSR to start state.
	 */
	void resetToStartState();
	
	void addPSRObserver(PSRObserver observer);

	/**
	 * Returns action Set
	 * 
	 * @return Set of valid actions.
	 */
	HashSet<Action> getActionSet();


}