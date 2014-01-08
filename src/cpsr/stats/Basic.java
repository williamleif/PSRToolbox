package cpsr.stats;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import cpsr.environment.DataSet;

/**
 * Class that performs basic stats ops.
 * @author whamil3
 *
 */
public class Basic 
{

	/**
	 * @param pData Vector of doubles.
	 * @return Mean of vector.
	 */
	public static double mean(List<Double> pData)
	{
		double lMean = 0;
		for(Double lEntry : pData)
		{
			lMean+=lEntry;
		}
		
		return lMean/((double)pData.size());
	}
	
	/**
	 * @param pData Vector of doubles.
	 * @param mean Mean of vector.
	 * @return Standard deviation of vector.
	 */
	public static double std(List<Double> pData, double mean)
	{
		double lVar = 0;
		
		for(Double lEntry : pData)
		{
			lVar += (lEntry-mean)*(lEntry-mean);
		}
		
		return lVar/((double)pData.size());
	}
	
	/**
	 * @param pData Vector of doubles.
	 * @return  Sum of entries in vector.
	 */
	public static double sum(List<Double> pData)
	{
		double lSum = 0;
		
		for(Double lEntry : pData)
		{
			lSum += lEntry;
		}
		
		return lSum;
	}
	
	/**
	 * @param pData Vector of doubles.
	 * @return Standard deviation of vector entries.
	 */
	public static double std(List<Double> pData)
	{
		return std(pData, mean(pData));
	}
	
	/**
	 * Serializes a DataSet to Results folder.
	 * @param pData The dataset.
	 * @param pName Name of serialized folder.
	 */
	public static void serializeDataSet(DataSet pData, String pName)
	{
		ObjectOutputStream lObWriter;
		
		try
		{
			lObWriter = new ObjectOutputStream(new FileOutputStream("Results/"+pName));
			lObWriter.writeObject(pData);
			lObWriter.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static DataSet readSerializedDataSet(String pName)
	{
		ObjectInputStream lObReader;
		DataSet lData = null;
		
		try
		{
			lObReader = new ObjectInputStream(new FileInputStream("Results/"+pName));
			lData = (DataSet)lObReader.readObject();
			lObReader.close();
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		
		return lData;
		
	}
	
	
}
