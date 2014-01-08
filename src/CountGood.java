import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class CountGood 
{
	public static void main(String args[])
	{
		BufferedReader fileReader;
		
		try {
			fileReader = new BufferedReader(new FileReader("Results/newcoloredgrid/testRewards"));
			
			double goodCount = 0;
			double totalCount = 0;
			while(fileReader.ready())
			{
				String line = fileReader.readLine();
				
				String[] split = line.split(",");
				
				if(split[split.length-1].equals("1.0"))
					goodCount++;
				
				totalCount++;
				
			}
			
			System.out.println(goodCount/totalCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
