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
package cpsr.stats;

import java.io.*;
import java.util.ArrayList;

/**
 * Abstract class for producing stats that can then be 
 * processed in matlab.
 * The stats are all different forms of prediction error.
 * 
 * @author whamil3
 *
 */
public class GetStats 
{

	public void writeKStepStats(ArrayList<ArrayList<Double>> preds, String testFile, String outFile)
	{
		ArrayList<ArrayList<Double>> multErrors = new ArrayList<ArrayList<Double>>();

		for(int k = 1; k <=10; k++)
		{
			ArrayList<ArrayList<Double>> ideals = getKStepIdealsFromFile(testFile+k+".txt");
			ArrayList<Double> errors = new ArrayList<Double>();

			int i = 0;
			for(ArrayList<Double> stepPreds : preds)
			{
				double error = 0;
				int j = 0;
				for(Double obPred : stepPreds)
				{
					error += (obPred-ideals.get(i).get(j))*(obPred-ideals.get(i).get(j));
					j++;
				}
				errors.add(error);
				i++;
			}
			multErrors.add(errors);
		}

		writeListToFile(multErrors, outFile);
	}

	private ArrayList<ArrayList<Double>> getKStepIdealsFromFile(String testFile)
	{
		ArrayList<ArrayList<Double>> ideals = new ArrayList<ArrayList<Double>>();
		double[] runCounts = new double[10];
		try
		{

			BufferedReader reader = new BufferedReader(new FileReader(testFile));

			for(int i = 0; i < 10; i++)
			{
				ideals.add(new ArrayList<Double>());
				for(int k = 0; k <= 64001; k++)
				{
					ideals.get(i).add(0.0);
				}
			}
			int count = 0;

			while(reader.ready())
			{
				Integer obsInt = Integer.parseInt(reader.readLine());

				if(obsInt == -1)
				{
					count = 0;
					continue;
				}
				if(count > 9)
				{
					continue;
				}
				runCounts[count]++;
				ideals.get(count).set(obsInt, ideals.get(count).get(obsInt)+1.0);
				count++;
			}

		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}

		int j = 0;
		for(ArrayList<Double> stepData : ideals)
		{
			for(int i = 0; i < stepData.size(); i++)
			{
				stepData.set(i, (stepData.get(i)/((double)runCounts[j])));
			}
			j++;
		}
		return ideals;
	}

	private void writeListToFile(ArrayList<ArrayList<Double>> list, String outFile)
	{
		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

			for(int i = 0; i < list.size(); i++)
			{
				for(ArrayList<Double> run: list)
				{
					writer.write(run.get(i).toString()+" ");
				}
				writer.newLine();
			}
			writer.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}

}



