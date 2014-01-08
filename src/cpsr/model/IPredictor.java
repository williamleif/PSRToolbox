package cpsr.model;

import java.util.ArrayList;

import cpsr.environment.components.Action;
import cpsr.environment.components.Observation;

public interface IPredictor {

	/**
	 * Returns the probability of seeing a particular observation 
	 * given that a particular action is taken.
	 * 
	 * @param act The next action to be taken.
	 * @param ob The observation. 
	 * @return The probability of seeing observation after taking action. 
	 */
	public abstract double getImmediateProb(Action act, Observation ob);

	/**
	 * Returns the probability of seeing a sequence of observations
	 * given that a particular sequence of actions is taken.
	 * 
	 * @param acts The list of actions.
	 * @param obs The list of observations.
	 * @return The probability associated with this test. 
	 */
	public abstract double getImmediateProb(ArrayList<Action> acts,
			ArrayList<Observation> obs);

	/**
	 * Returns the probability of seeing a specified action-observation pair
	 * k steps in the future.
	 * 
	 * @param act An action.
	 * @param ob An observation.
	 * @param k Steps ahead to predict.
	 * @return Probability of action occuring k-steps in future. 
	 */
	public abstract double getKStepPredictionProb(Action act, Observation ob,
			int k);

	/**
	 * Returns the probability of seeing a specified test
	 * k steps in the future.
	 * 
	 * @param acts A vector of actions.
	 * @param obs A vector of observations.
	 * @param k Steps ahead to predict.
	 * @return Probability of action occurring k-steps in future. 
	 */
	public abstract double getKStepPredictionProb(ArrayList<Action> acts,
			ArrayList<Observation> obs, int k);

}