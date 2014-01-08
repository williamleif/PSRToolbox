package cpsr.environment.simulation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import cpsr.environment.ModelQualityExperiment;
import cpsr.stats.PSRObserver;

public class ModelQualityExperimentPublisher {

	private ModelQualityExperiment experiment;
	private PSRObserver psrObserver;
	private String simulatorName;

	public ModelQualityExperimentPublisher(ModelQualityExperiment experiment, String simulatorName, PSRObserver psrObserver, 
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
		writeLikes(directory+"/likes");
	}

	public void writeSummary(String filepath)
	{
		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));
			writer.write("\n\nExperiment Summary");
			writer.write("\n----------------------");
			writer.write("\nSimulator: " + simulatorName);
			writer.write("\nRuns Per Iterations: " + experiment.getRunsPerIter());
			writer.write("\nModel Build Time: " + experiment.getRunTime());

			writer.write("\n\nPSR Summary");
			writer.write("\n----------------");
			writer.write("\nLearning Style: " + experiment.getModelLearnType());
			writer.write("\nSVD Max Dimension: " + experiment.getSvdDim());
			writer.write("\nSVD Min Singular Value: " + experiment.getMinSingVal());
			writer.write("\nProjection Dimension: " + experiment.getProjDim());
			writer.write("\nProjection Type: " + experiment.getProjType());
			writer.write("\nMax Test Length: " + experiment.getMaxTestLen());
			writer.write("\nMax History Length: " + experiment.getMaxHistLen());
			writer.write("\nHistory Compression: " + experiment.isHistCompress());
			writer.write("\nHashed: " + experiment.isHashed());
			writer.write("\nRandom Start: " + experiment.isRandStart());

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

	public void writeLikes(String filepath)
	{
		List<double[]> likeInfos = experiment.getLikeInfo();

		BufferedWriter writer;

		try
		{
			writer = new BufferedWriter(new FileWriter(filepath));

			for(double[] info : likeInfos)
			{
				writer.write(info[0] +"," +info[1]+"\n");
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
}
