/*
 *   Copyright 2012 William Hamilton
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package cpsr.environment.simulation;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.planning.APSRPlanner;
import cpsr.planning.RandomPlanner;

/**
 * Top level interface for simulating environments.
 * @author whamil3
 *
 */
public interface ISimulator
{
	/**
	 * Performs simulation using random actions.
	 * 
	 * @param runs
	 * 
	 * @return The data set generated from the simulation.
	 */
	public void simulateTrainingRuns(int runs, TrainingDataSet trainData);
	
	public void simulateTrainingRuns(int runs, APSRPlanner planner, TrainingDataSet trainData);
	
	public DataSet simulateTestRuns(int runs, APSRPlanner planner);
	
	/**
	 * @return A random planner for this domain.
	 */
	public RandomPlanner getRandomPlanner(int pSeed);
	
	/**
	 * Sets the max run length
	 * @param pMaxRunLength
	 */
	public void setMaxRunLength(int pMaxRunLength);
	
	public String getName();
	
}
