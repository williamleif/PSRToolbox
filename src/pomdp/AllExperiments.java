package pomdp;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.GenericValueIteration;
import pomdp.environments.FieldVisionRockSample;
import pomdp.environments.MasterMind;
import pomdp.environments.ModifiedRockSample;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;

public class AllExperiments {


	public static void main( String[] args ) throws IOException{


		String[] aCollectionMethods = new String[]{"GapMinCollection","FSVICollection","HSVICollection","PEMACollection", "PBVICollection","RandomCollection"};
		String[] aOrderingMethods = new String[]{"PerseusBackup","FullBackup","NewestPointsBackup",};

		/* set defaults */
		double dTargetADR = 10000.0;
		String[] aModelNames = new String[]{"hallway.1", "RockSample_7_8", "tiger-grid.1", "Wumpus7_0", "tagAvoid", "dialogue"};
		String sExperimentType = "Basic";
		boolean useBlindPolicy = false;

		/* target Running time (in seconds), if we want to stop at a specified time */
		int maxRunningTime = 200;
		int numEvaluations = 20;
		int numIndependentTrials = 50;
		int numBeliefsPerStep = -1, numBeliefsPerStepOrg = 100;
		int numBackupIterations = 1;
		boolean bAllowDuplicates = true;
		boolean bReversedBackupOrder = false;
		int cMaxThreads = 21;

		Vector<Process> vProcesses = new Vector<Process>();
		Vector<Process> vActiveProcesses = new Vector<Process>();
		for( String sModelName : aModelNames){
			for( String orderingName : aOrderingMethods ){
				for( String collectionName : aCollectionMethods ){
					if( collectionName.equals( "PEMACollection"  ) )
						numBeliefsPerStep = numBeliefsPerStepOrg / 10;	
					else
						numBeliefsPerStep = numBeliefsPerStepOrg;

					String sCommandLine = "java -Xmx1G SingleExperiment ";
					sCommandLine += sExperimentType + " ";
					sCommandLine += sModelName + " ";
					sCommandLine += maxRunningTime + " ";
					sCommandLine += numEvaluations + " ";
					sCommandLine += numIndependentTrials + " ";
					sCommandLine += collectionName + " ";
					sCommandLine += orderingName + " ";
					sCommandLine += numBeliefsPerStepOrg + " ";
					sCommandLine += numBackupIterations + " ";
					sCommandLine += bReversedBackupOrder + " ";
					

					try{
						Process p = Runtime.getRuntime().exec( sCommandLine );
						vProcesses.add(p);
						vActiveProcesses.add(p);
						Thread.sleep(1000);
					}
					catch(Exception e){
						System.err.println(e);
					}

					while(vActiveProcesses.size() == cMaxThreads){
						try
						{
							Thread.sleep(60000);//one minute sleep
							Vector<Process> vCurrentActiveThreads = new Vector<Process>();
							for(Process p : vActiveProcesses)
							{
								if(p.exitValue() == -1)
									vCurrentActiveThreads.add(p);
								else
								{
									PrintStream ps = new PrintStream( new FileOutputStream("logs/Basic/done.txt"));
									ps.println(sModelName + ", " + collectionName + ", " + orderingName );
									ps.close();								
								}
							}
							vActiveProcesses = vCurrentActiveThreads;
						}
						catch(Exception e)
						{
							System.err.println(e);
						}
					}
				}
			}
		}
		for(Process p : vProcesses)
		{
			try
			{
				p.waitFor();
			}
			catch(Exception e)
			{
				Logger.getInstance().log(e.toString());
			}
		}

	}

	public static void mainThreads( String[] args ) throws IOException{


		String[] aCollectionMethods = new String[]{"GapMinCollection","FSVICollection","HSVICollection","PEMACollection", "PBVICollection","RandomCollection"};
		//String[] aCollectionMethods = new String[]{"GapMinCollection"};
		String[] aOrderingMethods = new String[]{"PerseusBackup","FullBackup","NewestPointsBackup",};
		//String[] aOrderingMethods = new String[]{"PerseusBackup"};//,"FullBackup","NewestPointsBackup",};
		//String[] aOrderingMethods = new String[]{"FullBackup"};//,"FullBackup","NewestPointsBackup",};
		//String[] aOrderingMethods = new String[]{"NewestPointsBackup"};//,"FullBackup","NewestPointsBackup",};

		/* set defaults */
		double dTargetADR = 10000.0;
		String[] aModelNames = new String[]{"hallway.1", "RockSample_7_8", "tiger-grid.1", "Wumpus7_0", "tagAvoid", "dialogue"};
		String sExperimentType = "Basic";
		//String sModelName = "hallway.1";
		//String sModelName = "RockSample_7_8";
		//String sModelName = "tiger-grid.1";
		//String sModelName = "Wumpus7_0";
		//String sModelName = "tagAvoid";
		//String sModelName = "dialogue";
		//String sModelName = "underwaterNav";
		//String collectionName = "PBVICollection";
		//String collectionName = "FSVICollection";
		//String orderingName = "FullBackup";
		boolean useBlindPolicy = false;

		/* target Running time (in seconds), if we want to stop at a specified time */
		int maxRunningTime = 200;
		int numEvaluations = 20;
		int numIndependentTrials = 50;
		int numBeliefsPerStep = -1, numBeliefsPerStepOrg = 100;
		int numBackupIterations = 1;
		boolean bAllowDuplicates = true;
		boolean bReversedBackupOrder = false;
		int cMaxThreads = 21;

		//FileOutputStream fos = new FileOutputStream("logs/Basic/" + sModelName + "/" + sModelName + ".txt"); 
		//fos.close();
		Vector<Thread> vThreads = new Vector<Thread>();
		Vector<Thread> vActiveThreads = new Vector<Thread>();
		for( String sModelName : aModelNames){
			for( String orderingName : aOrderingMethods ){
				for( String collectionName : aCollectionMethods ){
					if( collectionName.equals( "PEMACollection"  ) )
						numBeliefsPerStep = numBeliefsPerStepOrg / 10;	
					else
						numBeliefsPerStep = numBeliefsPerStepOrg;



					RunExperiment re = new RunExperiment();

					re.cBeliefPoints = numBeliefsPerStep;
					re.cBackupIterations = numBackupIterations;
					re.cBeliefsPerStep = numBeliefsPerStep;
					re.cRunningTimes = maxRunningTime;
					re.cEvaluations = numEvaluations;
					re.cIndependentTrials = numIndependentTrials;
					re.dTargetADR = dTargetADR; 
					re.bAllowDuplicates = bAllowDuplicates;
					re.bUseBlindPolicy = useBlindPolicy;
					re.bReversedBackupOrder = bReversedBackupOrder;
					re.sModelName = sModelName;
					re.sCollectionName = collectionName;
					re.sOrderingName = orderingName;
					re.sExperimentType = sExperimentType;

					try{
						re.start();
						Thread.sleep(1000);
					}
					catch(Exception e){
						System.err.println(e);
					}
					vThreads.add(re);
					vActiveThreads.add(re);

					while(vActiveThreads.size() == cMaxThreads){
						try
						{
							Thread.sleep(60000);//one minute sleep
							Vector<Thread> vCurrentActiveThreads = new Vector<Thread>();
							for(Thread t : vActiveThreads)
							{
								if(t.isAlive())
									vCurrentActiveThreads.add(t);
								else
								{
									PrintStream ps = new PrintStream( new FileOutputStream("logs/Basic/done.txt"));
									ps.println(sModelName + ", " + collectionName + ", " + orderingName );
									ps.close();								
								}
							}
							vActiveThreads = vCurrentActiveThreads;
						}
						catch(Exception e)
						{
							System.err.println(e);
						}
					}
				}
			}
		}
		for(Thread t : vThreads)
		{
			try
			{
				t.join();
			}
			catch(Exception e)
			{
				Logger.getInstance().log(e.toString());
			}
		}

	}
}

class RunExperiment extends Thread{


	public int cBeliefPoints, cBackupIterations, cBeliefsPerStep, cRunningTimes, cEvaluations, cIndependentTrials;
	public double dTargetADR; 
	public boolean bAllowDuplicates, bUseBlindPolicy, bReversedBackupOrder;
	public String sModelName, sCollectionName, sOrderingName, sExperimentType;


	public void run(){
		System.out.println("Started " + sModelName + ", " + sCollectionName + ", " + sOrderingName);
		/* iterate through independent trials */
		String sOutputDir = "logs/" + sExperimentType + "/" + sModelName ;
		String sFileName = sModelName + "_" + sCollectionName + "_" + sOrderingName + "_" + cBeliefsPerStep + "_" + cBackupIterations;

		if (bUseBlindPolicy)
			sFileName += "_blind";

		sFileName += ".txt";
		try{
			Logger.getInstance().setOutputStream(sOutputDir, sFileName);
			//System.out.println(sModelName + ", " + sCollectionName + ", " + sOrderingName + " output file set to " + sFileName);
		}
		catch(IOException e){
			System.err.println(e);
		}

		Logger.getInstance().log("Model:" + sModelName);
		Logger.getInstance().log("Collection Algorithm:" + sCollectionName);
		Logger.getInstance().log("Backup Ordering Algorithm:" + sOrderingName);
		Logger.getInstance().log("# of Independent Trials: " + cIndependentTrials);
		Logger.getInstance().log("# of Beliefs Collected Per Step: " + cBeliefsPerStep);	
		Logger.getInstance().log("# of Backup Iterations Per Step: " + cBackupIterations);
		Logger.getInstance().log("Output File: "+ sFileName);

		try
		{
			Logger.getInstance().setOutputStream(sOutputDir, sFileName);
		}
		catch( Exception e ){
			System.err.println( e );
		}

		for (int trial = 0; trial < cIndependentTrials; trial++)
		{
			runTrial();
			System.out.println("Done " + sModelName + ", " + sCollectionName + ", " + sOrderingName + " - " + trial);

		}
		System.out.println("Done all " + sModelName + ", " + sCollectionName + ", " + sOrderingName);
	}

	public void runTrial(){
		System.gc();
		Logger.getInstance().logFull("POMDPExperiment",0,"main","Checking memory after GC");

		POMDP pomdp = null;


		/* load the POMDP model */
		try
		{
			if( sModelName.equals( "RockSample5" ) ){
				int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
			}
			else if( sModelName.equals( "RockSample5-99" ) ){
				int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat, .99);
			}
			else if( sModelName.equals( "RockSample7" ) ){
				int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
			}
			else if( sModelName.equals( "RockSample7-99" ) ){
				int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat, .99);
			}
			else if( sModelName.equals( "FieldVisionRockSample5" ) ){
				int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
				pomdp = new FieldVisionRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
			}
			else if( sModelName.equals( "FieldVisionRockSample7" ) ){
				int cX = 7, cY = 7, cRocks = 8, halfSensorDistance = 20;
				pomdp = new FieldVisionRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
			}
			else if( sModelName.equals( "MasterMind" ) ){
				pomdp = new MasterMind( 4, 0.3 );
			}
			/* flat POMDP, will load from file */
			else
			{
				pomdp = new POMDP();
				pomdp.load( ExecutionProperties.getPath() + sModelName + ".POMDP" );
				Logger.getInstance().log("max is " + pomdp.getMaxR() + " min is "+ pomdp.getMinR());
			}
		}
		catch (Exception e)
		{
			Logger.getInstance().log( e.toString() );
			e.printStackTrace();
			//System.exit( 0 );
		}

		Logger.getInstance().log("POMDP is " + pomdp.getName());



		ValueIteration viAlgorithm = new GenericValueIteration(pomdp, sCollectionName, sOrderingName, cBeliefsPerStep, cBackupIterations, bUseBlindPolicy, bAllowDuplicates, bReversedBackupOrder, true);
		int cMaxIterations = 500;
		try
		{
			/* run POMDP solver */
			viAlgorithm.valueIteration(cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, cRunningTimes, cEvaluations);
			Logger.getInstance().log( "----------------");
		}

		catch( Exception e ){
			Logger.getInstance().log( e.toString() );
			e.printStackTrace();
		} 
		catch( Error err ){
			Runtime rtRuntime = Runtime.getRuntime();
			Logger.getInstance().log( "POMDPSolver: " + err +
					" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
					" free " + rtRuntime.freeMemory() / 1000000 +
					" max " + rtRuntime.maxMemory() / 1000000 );
			Logger.getInstance().log( "Stack trace: " );
			err.printStackTrace();
		}
	}
}	

