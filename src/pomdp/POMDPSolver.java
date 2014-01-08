/*
 * Created on May 3, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pomdp;

import pomdp.algorithms.AlgorithmsFactory;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.FieldVisionRockSample;
import pomdp.environments.Logistics;
import pomdp.environments.MasterMind;
import pomdp.environments.ModifiedRockSample;
import pomdp.environments.NetworkManagement;
import pomdp.environments.POMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.Logger;
import pomdp.utilities.visualisation.RockSampleVisualisationUnit;
import pomdp.utilities.visualisation.VisualisationUnit;
import pomdp.valuefunction.MDPValueFunction;


public class POMDPSolver {
	
	public static void main( String[] args ){
		JProf.getCurrentThreadCpuTimeSafe();

		String sPath = ExecutionProperties.getPath();
		//String sModelName = "RockSample_7_8";
		//String sModelName = "FieldVisionRockSample5";
		//String sModelName = "underwaterNav";
		//String sModelName = "Hallway2.1";
		//String sModelName = "dialogue";
		//String sModelName = "tagAvoid";
		//String sModelName = "RockSample";
		//String sModelName = "RockSample7";
		//String sModelName = "MasterMind";
		String sModelName = "Wumpus7_0";
		//String sModelName = "tagAvoid";
		//String sModelName = "tiger-grid";
		String sMethodName = "PBVI";
	
						
		if( args.length > 0 )
			sModelName = args[0];
		if( args.length > 1 )
			sMethodName = args[1];			
		if( args.length > 2 )
			sMethodName = args[1];			
		
		POMDP pomdp = null;
		
		/* target Averaged Discounted Reward, if we want to stop if we get to some level of ADR */
		double dTargetADR = 100.0;
		
		/* target Running time (in seconds), if we want to stop at a specified time */
		int maxRunningTime = 45;
		int numEvaluations = 3;
		/* load the POMDP model */
		try{
			if( sModelName.equals( "RockSample5" ) ){
				int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat );
			}
			else if( sModelName.equals( "RockSample5-99" ) ){
				int cX = 5, cY = 5, cRocks = 5, halfSensorDistance = 4;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Flat, .99 );
			}
			else if( sModelName.equals( "RockSample" ) ){
				int cX = 8, cY = 8, cRocks = 8, halfSensorDistance = 4;
				pomdp = new ModifiedRockSample( cX, cY ,cRocks, halfSensorDistance, BeliefType.Factored );
				pomdp.setVisualisationUnit(new RockSampleVisualisationUnit(pomdp));
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
//			else if( sModelName.equals( "Network")){
//				int cMachines = 4;
//				pomdp = new NetworkManagement( cMachines );
//			}
//			else if( sModelName.equals( "Logistics")){
//				int cPackages = 4, cCities = 4, cTrucks = 1;
//				pomdp = new Logistics( cCities, cTrucks, cPackages, BeliefType.Factored );
//			}
			/* flat POMDP, will load from file */
			else{
				pomdp = new POMDP();
				pomdp.load( sPath + sModelName + ".POMDP" );
				Logger.getInstance().logln("max is " + pomdp.getMaxR() + " min is "+ pomdp.getMinR());
			}
		}
		catch( Exception e ){
			Logger.getInstance().logln( e );
			e.printStackTrace();
			System.exit( 0 );
		}

		pomdp.computeAverageDiscountedReward(2,100,new RandomWalkPolicy(pomdp.getActionCount()));
		
		try{
			Logger.getInstance().setOutputStream( pomdp.getName() + "_" + sMethodName + ".txt" );
		}
		catch( Exception e ){
			System.err.println( e );
		}

		/* special exception for QMDP */
		if( sMethodName.equals( "QMDP" ) ){
			MDPValueFunction vfQMDP = pomdp.getMDPValueFunction();
			vfQMDP.persistQValues( true );
			vfQMDP.valueIteration( 100, 0.001 );
			double dDiscountedReward = pomdp.computeAverageDiscountedReward( 1000, 100, vfQMDP );
			Logger.getInstance().log( "POMDPSolver", 0, "main", "ADR = " + dDiscountedReward );
			System.exit( 0 );
		}
		
		
		ValueIteration viAlgorithm = AlgorithmsFactory.getAlgorithm( sMethodName, pomdp );
		
		int cMaxIterations = 400;
		try{
			/* run POMDP solver */
			viAlgorithm.valueIteration( cMaxIterations, ExecutionProperties.getEpsilon(), dTargetADR, maxRunningTime, numEvaluations);
			
			/* compute the averaged return */
			double dDiscountedReward = pomdp.computeAverageDiscountedReward( 2000, 150, viAlgorithm );
			Logger.getInstance().log( "POMDPSolver", 0, "main", "ADR = " + dDiscountedReward );
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
			Logger.getInstance().log( "Stack trace: " );
			err.printStackTrace();
		}
	}
}
