package cpsr.stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import cpsr.planning.PSRPlanningExperiment;

public class PSRPlanningExperimentStatPublisher implements Serializable
{

	private static final long serialVersionUID = 6839345652049236315L;
	private PSRPlanningExperiment experiment;
	private PSRObserver psrObserver;
	private String simulatorName;
	
	public PSRPlanningExperimentStatPublisher(PSRPlanningExperiment experiment, String simulatorName, PSRObserver psrObserver, 
			Properties psrProperties,Properties planningProperties)
	{
		this.experiment = experiment;
		this.psrObserver = psrObserver;
		this.simulatorName = simulatorName;
	}
	
	
	public void publishResults(String directory)
	{
		File file = new File(directory);
		
		if(!file.mkdirs())
		{
			System.err.println("Directory: " + directory + " may already exist. Possible overwrites");
		}
		
		writeSummary(directory+"/summary");
		writeSingularVals(directory+"/singvals");
		writeTestSizes(directory+"/testsizes");
		writeHistSizes(directory+"/histSizes");
		writeTestRewards(directory+"/testRewards");
	}
	
	public void writeSummary(String filepath)
	{
		BufferedWriter writer;
		
		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));
			
			double averageTestReward = PlanningStats.getAverageDiscountedReward(experiment.getTestResults().getRewards(), 
					experiment.getDiscount());
			writer.write("Performance Summary");
			writer.write("\n-------------------");
			writer.write("\nAverage test episode reward: " + averageTestReward);
			writer.write("\nVariance of test episode rewards: " + PlanningStats.getVarOfDiscountedReward(experiment.getTestResults().getRewards(), experiment.getDiscount(), averageTestReward));
			writer.write("\nAverage test transition reward: " + averageTestReward/PlanningStats.getAverageLengthOfRun(experiment.getTestResults().getRewards()));
			writer.write("\nAverage length of test run: " + PlanningStats.getAverageLengthOfRun(experiment.getTestResults().getRewards()));
			writer.write("\nAverage random episode reward: " + PlanningStats.getAverageDiscountedReward(experiment.getRandRewards(), experiment.getDiscount()));
			writer.write("\nAverage random transition reward: " + PlanningStats.getAverageDiscountedReward(experiment.getRandRewards(), experiment.getDiscount())
					/PlanningStats.getAverageLengthOfRun(experiment.getRandRewards()));
			writer.write("\nAverage random of test run: " + PlanningStats.getAverageLengthOfRun(experiment.getRandRewards()));
			
			writer.write("\n\nExperiment Summary");
			writer.write("\n----------------------");
			writer.write("\nSimulator: " + simulatorName);
			writer.write("\nPlanning Rounds: " + experiment.getPolicyIter());
			writer.write("\nDiscount Factor: " + experiment.getDiscount());
			writer.write("\nEpsilon: " + experiment.getEpsilon());
			writer.write("\nRuns Per Iterations: " + experiment.getRunsPerIter());
			writer.write("\nAverage Model Build Time: " + Basic.mean(experiment.getModelBuildTimes()));
			writer.write("\nAverage Policy Construction Time: " + Basic.mean(experiment.getPolicyConstructionTimes()));
			
			writer.write("\n\nPSR Summary");
			writer.write("\n----------------");
			writer.write("\nInit_Runs: " + experiment.getInitRuns());
			writer.write("\nMax Batch Size: " + experiment.getMaxBatchSize());
			writer.write("\nSample Ratio: " + experiment.getSampleRatio());
			writer.write("\nLearning Style: " + experiment.getModelLearnType());
			writer.write("\nSVD Max Dimension: " + experiment.getSvdDim());
			writer.write("\nSVD Min Singular Value: " + experiment.getMinSingVal());
			writer.write("\nProjection Dimension: " + experiment.getProjDim());
			writer.write("\nProjection Type: " + experiment.getProjType());
			writer.write("\nMax Test Length: " + experiment.getMaxTestLen());
			writer.write("\nMax History Length: " + experiment.getMaxHistLen());
			writer.write("\nHistory Compression: " + experiment.isHistCompress());
			writer.write("\nHashed: " + experiment.isHashed());
			writer.write("\nMemoryless: " + experiment.isMemoryless());
			writer.write("\nRandom Start: " + experiment.isRandStart());

			writer.write("\n\nPolicy Construction Summary");
			writer.write("\n-------------------------------");
			writer.write("\nPlanning iterations: " + experiment.getPlanIters());
			writer.write("\nEnsemble Type: " + experiment.getPlanningType());
			writer.write("\nTrees Per Ensemble: " + experiment.getNumTrees());
			writer.write("\nTree Building Iterations: " + experiment.getTreeIters());
			writer.write("\nNum Splits: " + experiment.getNumTreeSplits());
			writer.write("\nLeaf Size: " + experiment.getLeafSize());
			writer.close();
		}
		catch(IOException ex)
		{
			System.err.println("Failed to write summary to: " + filepath);
		}
	}
	
	public void writeSingularVals(String filepath)
	{
		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));

			for(List<Double> singVals : psrObserver.getSingVals())
			{
				for(int i = 0; i < singVals.size(); i++)
				{
					if(i != 0)
						writer.write(",");
					
					writer.write(singVals.get(i).toString());
				}
				writer.write("\n");
			} 
			writer.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		
	}
	
	public void writeHistSizes(String filepath)
	{
		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));

			for(Integer entry : psrObserver.getHistorySetSizes())
			{
				writer.write(entry.toString() +"\n");
			}
			writer.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		
	}
	
	
	public void writeTestSizes(String filepath)
	{
		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));

			for(Integer entry : psrObserver.getTestSetSizes())
			{
				writer.write(entry.toString() +"\n");
			}
			writer.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}	
	}
	
	public void writeTestRewards(String filepath)
	{

		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));

			for(List<Double> epReward : experiment.getTestResults().getRewards())
			{
				for(int i = 0; i < epReward.size(); i++)
				{
					if(i != 0)
						writer.write(",");
					
					writer.write(epReward.get(i).toString());
				}
				writer.write("\n");
			} 
			writer.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	

	
	
}
