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
package cpsr.planning.ertapprox;

import java.util.ArrayList;
import java.util.HashMap;

import afest.datastructures.tree.decision.erts.ERTTrainingPoint;
import afest.datastructures.tree.decision.erts.grower.regression.RegressionERTGrower;
import cpsr.environment.DataSet;
import cpsr.model.IPSR;

/**
 * Class creates and trains ERTEnsemble.
 * Serves as an interface between cpsr and afest packages. 
 * 
 * @author William Hamilton
 */
public abstract class AERTEnsembleGrower {
	protected DataSet data;
	protected IPSR psr;
	protected RegressionERTGrower<String, Double> grower;
	protected ArrayList<ERTTrainingPoint<Double>> training;
	protected boolean featuresSet;
	
	/**
	 * Default constructor not be used!
	 */
	public AERTEnsembleGrower()
	{
		super();
	}
	/**
	 * Constructs ERTEnsembleGrower with specified DataSet and PSR.
	 * Must set training data before attempting to grow ensemble.
	 * @param data Associated DataSet.
	 * @param psr Associated PSR. 
	 */
	public AERTEnsembleGrower(DataSet data, IPSR psr)
	{
		this.data = data;
		this.psr = psr;
		featuresSet = false;
	}
	
	/**
	 * Sets training data for growing ERTEnsembles
	 * @param features The input features.
	 * @param targets Sample targets used in training. 
	 */
	public void setTrainData(HashMap<String, ArrayList<Double>> features, ArrayList<Double> targets)
	{
		training = ERTTrainingPoint.getERTPoints(features, targets);
		featuresSet = true;
	}
	
}
