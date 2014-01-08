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
package cpsr.planning;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cpsr.environment.DataSet;
import cpsr.environment.TrainingDataSet;
import cpsr.environment.components.ActionObservation;
import cpsr.environment.simulation.ISimulator;
import cpsr.model.APSR;
import cpsr.model.CPSR.ProjType;
import cpsr.model.HashedCPSR;
import cpsr.model.MemEffCPSR;
import cpsr.model.MemorylessState;
import cpsr.model.TPSR;
import cpsr.planning.ertapprox.actionensembles.ActionERTQPlanner;
import cpsr.planning.ertapprox.singleensemble.SingleEnsembleERTQPlanner;
import cpsr.planning.exceptions.PSRPlanningException;
import cpsr.stats.PSRObserver;
import cpsr.stats.PSRPlanningExperimentStatPublisher;

/**
 * Runs a standard PSR planning experiment with parameters in a property file.
 * 
 * @author William Hamilton
 */
public class PSRPlanningExperiment 
{

	public static final String DEF_ENSEMBLE_TYPE = "action",  DEF_LEAF_SIZE = "5", DEF_NUM_TREES="30", 
			DEF_TREE_BUILDING_ITER="50", DEF_EPSILON = "0.1", DEF_TREES_PER_ENS = "30", DEF_DISCOUNT_FACTOR = "0.9",
			DEF_PLANNING_ITERATIONS="30", DEF_TEST_RUNS="10000", DEF_MIN_SING_VAL = "0.000000000001", DEF_MODEL_LEARN_TYPE="update",
			DEF_PROJ_TYPE="Spherical", DEF_RAND_START="false", DEF_INIT_RUNS="1000";

	private ISimulator simulator;

	private int svdDim, projDim, policyIter, runsPerIter, maxTestLen, maxRunLength,
	numTreeSplits, leafSize, numTrees, treeIters, maxHistLen, testRuns, initRuns;

	private double epsilon, discount, minSingVal;

	private String planningType, modelLearnType;

	boolean memoryless, randStart;

	private ProjType projType;

	private Properties psrProperties, plannerProperties;

	private DataSet testResults;

	private List<List<List<ActionObservation>>> trainRunResults;
	private List<List<List<Double>>> trainRunRewards;
	private List<Double> modelBuildTimes, policyConstructionTimes;

	private PSRPlanningExperimentStatPublisher publisher;
	private PSRObserver psrObs;

	private boolean histCompress, hashed;

	private int maxBatchSize;

	private double sampleRatio;

	private List<List<Double>> randRewards;

	private int planIters;

	private static int seed = 1234567;


	/**
	 * Constructs a planning experiment.
	 * 
	 * @param pPSRConfigFile 
	 * @param pPlanningConfigFile
	 * @param pSimulator
	 */
	public PSRPlanningExperiment(String pPSRConfigFile, String pPlanningConfigFile, ISimulator pSimulator)
	{
		psrProperties = new Properties();
		plannerProperties = new Properties();

		simulator = pSimulator;


		try
		{
			psrProperties.load(new FileReader(pPSRConfigFile));
			plannerProperties.load(new FileReader(pPlanningConfigFile));

			//getting PSR parameters.
			memoryless = Boolean.parseBoolean(psrProperties.getProperty("Memoryless", "false"));
			svdDim = Integer.parseInt(psrProperties.getProperty("SVD_Dimension", "-1"));
			projDim = Integer.parseInt(psrProperties.getProperty("Projection_Dimension", "-1"));
			maxTestLen = Integer.parseInt(psrProperties.getProperty("Max_Test_Length", "-1"));
			maxHistLen = Integer.parseInt(psrProperties.getProperty("Max_History_Length", "-1"));
			minSingVal = Double.parseDouble(psrProperties.getProperty("Min_Singular_Val", DEF_MIN_SING_VAL));
			projType = ProjType.valueOf(psrProperties.getProperty("Projection_Type", DEF_PROJ_TYPE));
			randStart = Boolean.parseBoolean(psrProperties.getProperty("Rand_Start", DEF_RAND_START));
			histCompress = Boolean.parseBoolean(psrProperties.getProperty("Hist_Compress", "false"));
			hashed = Boolean.parseBoolean(psrProperties.getProperty("Hashed", "false"));


			//getting planning parameters
			planningType = plannerProperties.getProperty("Ensemble_Type", DEF_ENSEMBLE_TYPE);
			epsilon = Double.parseDouble(plannerProperties.getProperty("Epsilon", DEF_EPSILON));
			numTreeSplits = Integer.parseInt(plannerProperties.getProperty("Num_Tree_Splits", "-1"));
			runsPerIter = Integer.parseInt(plannerProperties.getProperty("Runs_Per_Iteration"));
			leafSize = Integer.parseInt(plannerProperties.getProperty("Leaf_Size", DEF_LEAF_SIZE));
			numTrees = Integer.parseInt(plannerProperties.getProperty("Trees_Per_Ensemble", DEF_TREES_PER_ENS));
			treeIters = Integer.parseInt(plannerProperties.getProperty("Tree_Building_Iterations", DEF_TREE_BUILDING_ITER));
			policyIter = Integer.parseInt(plannerProperties.getProperty("Planning_Iterations", DEF_PLANNING_ITERATIONS));
			testRuns = Integer.parseInt(plannerProperties.getProperty("Test_Runs", DEF_TEST_RUNS));
			discount = Double.parseDouble(plannerProperties.getProperty("Discount_Factor", DEF_DISCOUNT_FACTOR));
			maxRunLength = Integer.parseInt(plannerProperties.getProperty("Max_Run_Length", "-1"));
			modelLearnType = plannerProperties.getProperty("Model_Learn_Type", DEF_MODEL_LEARN_TYPE);
			initRuns =  Integer.parseInt(plannerProperties.getProperty("Init_Runs", DEF_INIT_RUNS));
			maxBatchSize = Integer.parseInt(plannerProperties.getProperty("Max_Batch_Size", "1000"));
			sampleRatio = Double.parseDouble(plannerProperties.getProperty("Sample_Ratio", "-1.0"));
			planIters = Integer.parseInt(plannerProperties.getProperty("Plan_Iters", Integer.toString(runsPerIter)));
			if(maxRunLength == -1) maxRunLength = Integer.MAX_VALUE;

		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

		if(numTreeSplits == -1 || runsPerIter == 0 || maxTestLen == -1 || maxHistLen == -1 || svdDim == -1)
		{
			throw new IllegalArgumentException("Missing required parameter");
		}
	}

	/**
	 * Runs an experiment and returns the DataSet of test results. 
	 * 
	 * @return Test results;
	 */
	public DataSet runExperiment()
	{
		APSR psrModel;
		APSRPlanner planner = null;
		String psrType;

		trainRunResults = new ArrayList<List<List<ActionObservation>>>();
		trainRunRewards = new ArrayList<List<List<Double>>>();
		modelBuildTimes = new ArrayList<Double>();
		policyConstructionTimes = new ArrayList<Double>();
		psrObs = new PSRObserver();
		publisher = new PSRPlanningExperimentStatPublisher(this, simulator.getName(), psrObs, psrProperties, plannerProperties);


		simulator.setMaxRunLength(maxRunLength);

		TrainingDataSet trainData = new TrainingDataSet(maxTestLen, planIters);

		trainData.newDataBatch(maxBatchSize);

		simulator.simulateTrainingRuns(initRuns, trainData);

		randRewards = trainData.getRewards();

		if(sampleRatio != -1.0)
			trainData.importanceSample(sampleRatio);

		if(memoryless)
		{
			psrModel = new MemorylessState(trainData);
		}
		else
		{
			if(projDim == -1)
			{
				psrType = "TPSR";
			}
			else 
			{
				psrType = "CPSR";
			}

			psrModel = initPSRModel(psrType, trainData);
			psrModel.addPSRObserver(psrObs);
		}

		for(int i = 0; i < policyIter; i++)
		{			
			long startBuildTime = System.currentTimeMillis();
			if(modelLearnType.equals("update") && i != 0)
			{
				psrModel.update();
			}
			else
			{
				psrModel.build();
			}

			if(planningType.equals("action"))
			{
				planner = new ActionERTQPlanner(psrModel);
			}
			else if(planningType.equals("single"))
			{
				planner = new SingleEnsembleERTQPlanner(psrModel, trainData);
			}
			else
			{
				throw new PSRPlanningException("Unrecognized planner type.");
			}
			long endBuildTime = System.currentTimeMillis();
			modelBuildTimes.add((double)(endBuildTime-startBuildTime)/1000);

			long startPolicyTime = System.currentTimeMillis();

			System.out.println(planIters);
			if(i == 0)
			{
				planner.learnQFunction(trainData, planIters, treeIters, numTreeSplits, leafSize,
						numTrees, discount);
			}
			else
			{
				planner.learnQFunction(trainData, planIters+i*Math.min(runsPerIter, planIters), treeIters, numTreeSplits, leafSize,
						numTrees, discount);
			}

			long endPolicyTime = System.currentTimeMillis();
			policyConstructionTimes.add((double)(endPolicyTime-startPolicyTime)/1000);

			System.out.println("Finished iteration: " + (i+1));


			if(i != policyIter-1)
			{
				trainData.newDataBatch(maxBatchSize);
				simulator.simulateTrainingRuns(runsPerIter, new EpsilonGreedyPlanner(simulator.getRandomPlanner(seed), planner, epsilon, seed), trainData);
			}


		}

		testResults = simulator.simulateTestRuns(testRuns, planner);		

		return testResults;
	}

	public static String getDefEnsembleType() {
		return DEF_ENSEMBLE_TYPE;
	}

	public static String getDefLeafSize() {
		return DEF_LEAF_SIZE;
	}

	public static String getDefNumTrees() {
		return DEF_NUM_TREES;
	}

	public static String getDefTreeBuildingIter() {
		return DEF_TREE_BUILDING_ITER;
	}

	public static String getDefEpsilon() {
		return DEF_EPSILON;
	}

	public static String getDefTreesPerEns() {
		return DEF_TREES_PER_ENS;
	}

	public static String getDefDiscountFactor() {
		return DEF_DISCOUNT_FACTOR;
	}

	public static String getDefPlanningIterations() {
		return DEF_PLANNING_ITERATIONS;
	}

	public static String getDefTestRuns() {
		return DEF_TEST_RUNS;
	}

	public static String getDefMinSingVal() {
		return DEF_MIN_SING_VAL;
	}

	public static String getDefModelLearnType() {
		return DEF_MODEL_LEARN_TYPE;
	}

	public static String getDefProjType() {
		return DEF_PROJ_TYPE;
	}

	public static String getDefRandStart() {
		return DEF_RAND_START;
	}

	public static String getDefInitRuns() {
		return DEF_INIT_RUNS;
	}

	public ISimulator getSimulator() {
		return simulator;
	}

	public int getInitRuns() {
		return initRuns;
	}

	public boolean isMemoryless() {
		return memoryless;
	}

	public boolean isRandStart() {
		return randStart;
	}

	public PSRPlanningExperimentStatPublisher getPublisher() {
		return publisher;
	}

	public PSRObserver getPsrObs() {
		return psrObs;
	}

	public boolean isHistCompress() {
		return histCompress;
	}

	public boolean isHashed() {
		return hashed;
	}

	public int getMaxBatchSize() {
		return maxBatchSize;
	}

	public double getSampleRatio() {
		return sampleRatio;
	}

	public int getPlanIters() {
		return planIters;
	}

	public static int getSeed() {
		return seed;
	}

	public void publishResults(String resultsDir)
	{
		publisher.publishResults(resultsDir);
	}

	private APSR initPSRModel(String psrType, TrainingDataSet trainData)
	{
		APSR psr = null;
		if(psrType.equals("CPSR"))
		{
			if(hashed)
			{
				if(maxHistLen != -1)
				{
					psr = new HashedCPSR(trainData, minSingVal, svdDim, maxHistLen, projDim, histCompress);
				}
				else
				{
					psr = new HashedCPSR(trainData, minSingVal, svdDim, projDim, histCompress);
				}
			}
			else
			{
				if(maxHistLen != -1)
				{
					psr = new MemEffCPSR(trainData, minSingVal, svdDim, projDim, maxHistLen, projType, randStart);
				}
				else
				{
					psr = new MemEffCPSR(trainData, minSingVal, svdDim, projDim, projType,  randStart);
				}
			}
		}
		else
		{
			if(maxHistLen != -1)
			{
				psr = new TPSR(trainData, minSingVal, svdDim, maxHistLen);
			}
			else
			{
				psr = new TPSR(trainData, minSingVal, svdDim);
			}
		}

		return psr;
	}

	public int getSvdDim() {
		return svdDim;
	}

	public int getProjDim() {
		return projDim;
	}

	public ProjType getProjType(){
		return projType;
	}

	public int getPolicyIter() {
		return policyIter;
	}

	public int getRunsPerIter() {
		return runsPerIter;
	}

	public int getMaxTestLen() {
		return maxTestLen;
	}

	public int getMaxRunLength() {
		return maxRunLength;
	}

	public int getNumTreeSplits() {
		return numTreeSplits;
	}

	public int getLeafSize() {
		return leafSize;
	}

	public int getNumTrees() {
		return numTrees;
	}

	public int getTreeIters() {
		return treeIters;
	}

	public int getMaxHistLen() {
		return maxHistLen;
	}

	public int getTestRuns() {
		return testRuns;
	}

	public double getEpsilon() {
		return epsilon;
	}

	public double getDiscount() {
		return discount;
	}

	public double getMinSingVal() {
		return minSingVal;
	}

	public String getPlanningType() {
		return planningType;
	}

	public String getModelLearnType() {
		return modelLearnType;
	}

	public Properties getPsrProperties() {
		return psrProperties;
	}

	public Properties getPlannerProperties() {
		return plannerProperties;
	}

	public DataSet getTestResults() {
		return testResults;
	}

	public List<List<List<ActionObservation>>> getTrainRunResults() {
		return trainRunResults;
	}

	public List<List<List<Double>>> getTrainRunRewards() {
		return trainRunRewards;
	}

	public List<Double> getModelBuildTimes() {
		return modelBuildTimes;
	}

	public List<Double> getPolicyConstructionTimes() {
		return policyConstructionTimes;
	}

	public List<List<Double>> getRandRewards() 
	{
		return randRewards;
	}


}
