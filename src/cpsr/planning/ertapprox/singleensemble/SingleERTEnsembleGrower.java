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
package cpsr.planning.ertapprox.singleensemble;

import afest.datastructures.tree.decision.erts.grower.regression.RegressionERTGrower;
import afest.datastructures.tree.decision.erts.informationfunctions.RelativeVarianceReduction;
import cpsr.environment.DataSet;
import cpsr.model.IPSR;
import cpsr.planning.ertapprox.AERTEnsembleGrower;
import cpsr.planning.exceptions.PSRPlanningException;

public class SingleERTEnsembleGrower extends AERTEnsembleGrower {
	
	/**
	 * Constructs SingleERTEnsembleGrower from DataSet and PSR
	 * 
	 * @param data Associated DataSet.
	 * @param psr Associated PSR.
	 */
	public SingleERTEnsembleGrower(DataSet data, IPSR psr)
	{
		super(data, psr);
	}
	/**
	 * Grows a SingleERTEnsemble.
	 * 
	 * @param k  k number of splits to create at each inner node.  If k is null, then sqrt(number of attribute) will be used.
	 * @param treesInEnsemble Number of trees in ensemble. 
	 * @param nMin nMin a leaf is created if |set| <= n_min.
	 * @param trainData DataSet associated with this single ensemble.
	 * @return
	 */
	public SingleERTEnsemble growSingleERTEnsemble(int k, int treesInEnsemble, int nMin) 
	{
		if(featuresSet)
		{
			RegressionERTGrower<String, Double> grower = new RegressionERTGrower<String, Double>(k, nMin, new RelativeVarianceReduction<String, Double>());
			return new SingleERTEnsemble(grower.growForest(training, treesInEnsemble), data);
		}
		else
		{
			throw new PSRPlanningException("Must set features before growing ensembles");
		}
	
	}
}
