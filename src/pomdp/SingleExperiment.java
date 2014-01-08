package pomdp;



import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.GenericValueIteration;
import pomdp.environments.FieldVisionRockSample;
import pomdp.environments.MasterMind;
import pomdp.environments.ModifiedRockSample;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.ExecutionProperties;
import java.io.*;
import java.util.Calendar;
import java.util.Date;

import pomdp.utilities.Logger;


public class SingleExperiment {

	public static void main( String[] args ) throws IOException{

		/* set defaults */
		double dTargetADR = 10000.0;
		String sExperimentType = "";
		String sModelName = "";
		String collectionName = "";
		String orderingName = "";
		boolean useBlindPolicy = false;

		/* target Running time (in seconds), if we want to stop at a specified time */
		int maxRunningTime = 200;
		int numEvaluations = 20;
		int numIndependentTrials = 50;
		int numBeliefsPerStep = -1, numBeliefsPerStepOrg = 100;
		int numBackupIterations = 1;
		boolean bAllowDuplicates = false;
		boolean bReversedBackupOrder = false;
		boolean bUseFIB = true;


		if (args.length == 13)
		{
			sExperimentType = args[0];
			sModelName = args[1];
			maxRunningTime = Integer.parseInt(args[2]);
			numEvaluations = Integer.parseInt(args[3]);			
			numIndependentTrials = Integer.parseInt(args[4]);		
			collectionName = args[5];
			orderingName = args[6];			
			numBeliefsPerStepOrg = Integer.parseInt(args[7]);
			numBackupIterations = Integer.parseInt(args[8]);
			if (Boolean.parseBoolean(args[9]))
				bReversedBackupOrder = true;			
			else
				bReversedBackupOrder = false;
			if (Boolean.parseBoolean(args[10]))
				bAllowDuplicates = true;
			else
				bAllowDuplicates = false;
			if (Boolean.parseBoolean(args[11]))
				bUseFIB = true;
			else
				bUseFIB = false;
			if (Boolean.parseBoolean(args[12]))
				useBlindPolicy = true;
			else
				useBlindPolicy = false;
		}else{
			System.err.println("Must have exactly 13 arguments");
			System.exit(0);
		}
		
		System.out.println("Started " + sModelName + ", " + collectionName + ", " + orderingName + ", " + bReversedBackupOrder + ", " + bAllowDuplicates);

		String sOutputDir = "logs/" + sExperimentType + "/" + sModelName ;
		String sFileName = sModelName + "_" + collectionName + "_" + orderingName + "_" + numBeliefsPerStep + "_" + numBackupIterations;

		if (useBlindPolicy)
			sFileName += "_blind";

		sFileName += ".txt";

		if( collectionName.equals( "PEMACollection"  ) )
			numBeliefsPerStep = numBeliefsPerStepOrg / 10;	
		else
			numBeliefsPerStep = numBeliefsPerStepOrg;

		if ((args.length != 0) && (args.length < 9))
		{
			Logger.getInstance().logln("Format is: POMDPExperiment TYPE MODEL_NAME RUNNING_TIME NUMBER_EVALUATIONS NUMBER_TRIALS COLLECTION_ALGORITHM ORDERING_ALGORITHM NUMBER_BELIEFS_STEP NUMBER_BACKUP_ITERATIONS");
			return;
		}

		try
		{
			Logger.getInstance().setOutputStream(sOutputDir, sFileName);
		}
		catch( Exception e ){
			System.err.println( e );
		}

		Logger.getInstance().logln("Model:" + sModelName);
		Logger.getInstance().logln("Collection Algorithm:" + collectionName);
		Logger.getInstance().logln("Backup Ordering Algorithm:" + orderingName);
		Logger.getInstance().logln("# of Independent Trials: " + numIndependentTrials);
		Logger.getInstance().logln("# of Beliefs Collected Per Step: " + numBeliefsPerStep);	
		Logger.getInstance().logln("# of Backup Iterations Per Step: " + numBackupIterations);
		Logger.getInstance().logln("Output File: "+ sFileName);

		/* iterate through independent trials */
		for (int trial = 0; trial < numIndependentTrials; trial++)
		{

			System.gc();
			Logger.getInstance().logFull("POMDPExperiment",0,"main","Checking memory after GC");

			POMDP pomdp = null;


			/* load the POMDP model */
			try
			{
				pomdp = new POMDP();
				pomdp.load( ExecutionProperties.getPath() + sModelName + ".POMDP" );
				Logger.getInstance().logln("max is " + pomdp.getMaxR() + " min is "+ pomdp.getMinR());

			}
			catch (Exception e)
			{
				Logger.getInstance().logln( e );
				e.printStackTrace();
				System.exit( 0 );
			}

			Logger.getInstance().logln("POMDP is " + pomdp.getName());



			ValueIteration viAlgorithm = new GenericValueIteration(pomdp, collectionName, orderingName, numBeliefsPerStep, numBackupIterations, useBlindPolicy, bAllowDuplicates, bReversedBackupOrder, bUseFIB);
			int cMaxIterations = 500;
			try
			{
				/* run POMDP solver */
				viAlgorithm.valueIteration(cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, maxRunningTime, numEvaluations);
				Logger.getInstance().log( "----------------");
				Calendar cal = Calendar.getInstance();
				String sTime = cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND);
				System.out.println(sTime + ", Done " + sModelName + ", " + collectionName + ", " + orderingName + " - " + trial);
			}

			catch( Exception e ){
				Logger.getInstance().logln( e );
				e.printStackTrace();
			} 
			catch( Error err ){
				Runtime rtRuntime = Runtime.getRuntime();
				Logger.getInstance().logln( "POMDPSolver: " + err +
						" allocated " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free " + rtRuntime.freeMemory() / 1000000 +
						" max " + rtRuntime.maxMemory() / 1000000 );
				System.err.println("$$$$$$$$$$$$$$$$$$$$$");
				Logger.getInstance().log( "Stack trace: " );
				err.printStackTrace();
				System.err.println(sModelName + ", " + collectionName + ", " + orderingName + ", " + trial);
				System.err.println("$$$$$$$$$$$$$$$$$$$$$");
			}
		}
	}
}
