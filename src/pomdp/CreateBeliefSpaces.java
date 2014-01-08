package pomdp;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import pomdp.algorithms.PolicyStrategy;
import pomdp.environments.ModifiedRockSample;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.RandomGenerator;
import pomdp.valuefunction.MDPValueFunction;

public class CreateBeliefSpaces {
	
	public static Vector<BeliefState> createRandomSpace(POMDP pomdp, int cBeliefPoints){
		
		PolicyStrategy pvRandom = new RandomWalkPolicy(pomdp.getActionCount());
		Vector<BeliefState> vPoints = new Vector<BeliefState>();
			
		double dMaxADR = 0.0;
		while(vPoints.size() < cBeliefPoints){
			double dADR = pomdp.computeDiscountedReward(cBeliefPoints - vPoints.size(), pvRandom, vPoints, true, null);
			if( dADR > dMaxADR )
				dMaxADR = dADR;			
		}
		//Logger.getInstance().log( "CreateBeliefSpaces", 0, "createRandomSpace", "The maximal ADR observed over " + cBeliefPoints + " points is " + dMaxADR );
		return vPoints;
	}
	
	
	public static Vector<BeliefState> computeOutlyingBeliefPoints( POMDP pomdp, int cPoints ){
		Vector<BeliefState> vBeliefPoints = new Vector<BeliefState>();
		BeliefStateFactory bsf = pomdp.getBeliefStateFactory();
		BeliefState bsNext = null;
		int iPoint = 0;
		String sDistances = "";
		
		vBeliefPoints.add( bsf.getInitialBeliefState() );
		
		for( iPoint = 0 ; iPoint < cPoints ; iPoint++ ){
			bsNext = bsf.computeFarthestSuccessor( vBeliefPoints );
			//Logger.getInstance().logln( bsNext );
			if( bsNext != null ){
				String sDist = bsf.distance( vBeliefPoints, bsNext ) + "";
				if( sDist.length() > 4 )
					sDist = sDist.substring( 0, 4 );
				sDistances += sDist + ",";
				vBeliefPoints.add( bsNext );
			}
			
			//if( iPoint % ( cPoints / 10 ) == 0 ){
			if( iPoint > 0 && iPoint % 10 == 0 ){
				Iterator it = vBeliefPoints.iterator();
				double cP = vBeliefPoints.size();
				int cPreds = 0, cMaxPreds = 0;
				double dAvg = 0.0;
				double dR = 0.0, dMaxR = 0.0;
				BeliefState bsCur = null;
				
				while( it.hasNext() ){
					bsCur = (BeliefState) it.next();
					cPreds = bsCur.countPredecessors();
					dAvg += cPreds / cP;
					if( cPreds > cMaxPreds )
						cMaxPreds = cPreds;
					dR = pomdp.immediateReward( bsCur );
					if( dR > dMaxR ){
						dMaxR = dR;
					}
				}
				
				
				Runtime rtRuntime = Runtime.getRuntime();
				Logger.getInstance().logln( iPoint + ")" +
						" |B| = " + vBeliefPoints.size() + 
						" distances " + sDistances + 
						" avg preds = " + dAvg +
						" max preds " + cMaxPreds +
						//" max R = " + dMaxR +
						" |B| = " + pomdp.getBeliefStateFactory().getBeliefStateCount() +
						" max memory " + rtRuntime.maxMemory() / 1000000 + 
						" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free memory " + rtRuntime.freeMemory() / 1000000 );
				sDistances = "";
			}
		}
		
		return vBeliefPoints;
	}

	
	public static Vector<BeliefState> createHeuristicSpace( POMDP pomdp, int iSeed, int cBeliefPoints ){
		int cTrials = 0;
		double dMaxADR = 0.0;
		MDPValueFunction pvQMDP = pomdp.getMDPValueFunction();
		Vector<BeliefState> vPoints = new Vector<BeliefState>();
		pomdp.initRandomGenerator( iSeed * 11113 );
		pvQMDP.valueIteration( 20000, 0.0001 );
		while( vPoints.size() < cBeliefPoints ){
			double dADR = pomdp.computeMDPDiscountedReward( cBeliefPoints, pvQMDP, true, vPoints );
			if( dADR > dMaxADR )
				dMaxADR = dADR;
			cTrials++;	
		}
		while( vPoints.size() > cBeliefPoints ){
			vPoints.remove( vPoints.size() - 1 );
		}
/*		
		for( BeliefState bsCurrent : vPoints ){
			Logger.getInstance().logln( bsCurrent );
			Logger.getInstance().log( "Successors: " );
			for( BeliefState bsSucc : bsCurrent.getSuccessors() ){
				Logger.getInstance().log( " bs" + bsSucc.getId() + ", " );
			}
			Logger.getInstance().logln();
			Logger.getInstance().log( "Predecessors: " );
			for( BeliefState bsPred : bsCurrent.getPredecessors() ){
				Logger.getInstance().log( " bs" + bsPred.getId() + ", " );
			}
			Logger.getInstance().logln();
		}
	*/	
		
		Logger.getInstance().log( "CreateBeliefSpaces", 0, "createHeuristicSpace", "The maximal ADR observed over " + cBeliefPoints + " points is " + dMaxADR );
		return vPoints;
	}
	
//	protected static void createRandomSpaces( POMDP pomdp, String sPath, int cSpaces, int cBeliefPoints ) throws IOException, TransformerException, ParserConfigurationException{
//		double dDiscountedReward = 0.0, dSumDiscountedReward = 0.0, dMaxDiscountedReward =  0.0, dMinDiscountedReward = 1000.0;
//		int cTrials = 0;
//		int cReportPoints = 1000;
//		Runtime rtRuntime = Runtime.getRuntime();
//		PolicyStrategy pvRandom = new RandomWalkPolicy( pomdp.getActionCount() );
//		MDPValueFunction pvQMDP = pomdp.getMDPValueFunction();
//		int[] aiActionCount = new int[pomdp.getActionCount()];
//		/*
//		ValueIteration vfHSVI = new HeuristicSearchValueIteration( pomdp.m_fTransition, pomdp.m_fReward, 
//				pomdp.m_fObservation, pomdp.m_cStates, 
//				pomdp.m_cActions, pomdp.m_cObservations, pomdp.m_dGamma, 0.01 );
//				*/
//		pvQMDP.valueIteration( 20000, 0.0001 );
//		//vfHSVI.valueIteration( null, 25, 0.01, pomdp, 18.0 );
//		for( int iSeed = 0 ; iSeed < cSpaces ; iSeed++ ){
//			pomdp.getBeliefStateFactory().clear();
//			pomdp.initRandomGenerator( iSeed );
//			cReportPoints = 0;
//			dMaxDiscountedReward = -1000.0;
//			dMinDiscountedReward = 1000.0;
//			Logger.getInstance().logln( "Creating belief space with random seed = " + iSeed );
//			while( pomdp.getBeliefStateFactory().getBeliefStateCount() < cBeliefPoints ){
//				//pomdp.simulate( cBeliefPoints, 4, -1 );
//				dDiscountedReward = pomdp.computeMDPDiscountedReward( 50, pvQMDP, false, null );
//				//dDiscountedReward = pomdp.computeDiscountedReward( 50, pvQMDP, null, true, aiActionCount );
//				dSumDiscountedReward += dDiscountedReward;
//				cTrials++;
//				if( dDiscountedReward > dMaxDiscountedReward )
//					dMaxDiscountedReward = dDiscountedReward;
//				if( dDiscountedReward < dMinDiscountedReward )
//					dMinDiscountedReward = dDiscountedReward;
//				
//				if( pomdp.getBeliefStateFactory().getBeliefStateCount() >= cReportPoints ){
//					Logger.getInstance().logln( "Collected " + pomdp.getBeliefStateFactory().getBeliefStateCount() + 
//							" points, discounted reward = " + dSumDiscountedReward / cTrials +
//							" over " + cTrials + " trials " +
//							" max = " + dMaxDiscountedReward +
//							" min = " + dMinDiscountedReward );
//					cReportPoints += 10;
//				}
//					
//			}
//			
//			Logger.getInstance().logln( "Collected discounted reward = " + dSumDiscountedReward / cTrials +
//					" over " + cTrials + " trials " +
//					" max = " + dMaxDiscountedReward +
//					" min = " + dMinDiscountedReward );
//			
//			pomdp.getBeliefStateFactory().saveBeliefSpace( sPath + "/" + pomdp.getName() + "/BeliefSpace" + iSeed +/* "-" + cBeliefPoints +*/ ".xml" );
//			Logger.getInstance().logln( "Done seed = " + iSeed +
//					" |B| = " + pomdp.getBeliefStateFactory().getBeliefStateCount() +
//					" max memory " + rtRuntime.maxMemory() / 1000000 + 
//					" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
//					" free memory " + rtRuntime.freeMemory() / 1000000 );
//			pomdp.getBeliefStateFactory().clear();
//			Logger.getInstance().logln( "After memory release " + 
//					" max memory " + rtRuntime.maxMemory() / 1000000 + 
//					" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
//					" free memory " + rtRuntime.freeMemory() / 1000000 );
//		}
//	}
	
	
	protected static void createIncreasingSizeRandomSpaces( POMDP pomdp, String sPath, int iInitialSize, int iFinalSize, int iIncrementSize, int cDuplicateSpaces, boolean bRandom ) throws IOException, TransformerException, ParserConfigurationException{
		double dDiscountedReward = 0.0, dSumDiscountedReward = 0.0, dMaxDiscountedReward =  0.0, dMinDiscountedReward = 1000.0;
		int cTrials = 0;
		int cReportPoints = 1000;
		Runtime rtRuntime = Runtime.getRuntime();
		PolicyStrategy pvRandom = new RandomWalkPolicy( pomdp.getActionCount() );
		MDPValueFunction pvQMDP = pomdp.getMDPValueFunction();
		int[] aiActionCount = new int[pomdp.getActionCount()];
		int cBeliefPoints = iInitialSize;
		/*
		ValueIteration vfHSVI = new HeuristicSearchValueIteration( pomdp.m_fTransition, pomdp.m_fReward, 
				pomdp.m_fObservation, pomdp.m_cStates, 
				pomdp.m_cActions, pomdp.m_cObservations, pomdp.m_dGamma, 0.01 );
				*/
		pvQMDP.valueIteration( 20000, 0.0001 );
		//vfHSVI.valueIteration( null, 25, 0.01, pomdp, 18.0 );
		
		int iBeliefSpace = 0;
		
		while( cBeliefPoints <= iFinalSize ){
			for( int iSeed = 0 ; iSeed < cDuplicateSpaces ; iSeed++ ){
				pomdp.getBeliefStateFactory().clear();
				pomdp.initRandomGenerator( iSeed );
				cReportPoints = 0;
				dMaxDiscountedReward = -1000.0;
				dMinDiscountedReward = 1000.0;
				Logger.getInstance().logln( "Creating belief space of size " + cBeliefPoints + " with random seed = " + iSeed );
				while( pomdp.getBeliefStateFactory().getBeliefStateCount() < cBeliefPoints ){
					//pomdp.simulate( cBeliefPoints, 4, -1 );
					if( bRandom ){
						dDiscountedReward = pomdp.computeDiscountedReward( cBeliefPoints - pomdp.getBeliefStateFactory().getBeliefStateCount(), pvRandom, null, true, aiActionCount );
					}
					else{
						dDiscountedReward = pomdp.computeMDPDiscountedReward( 500, pvQMDP, true, null );
					}
					//dDiscountedReward = pomdp.computeDiscountedReward( 50, pvQMDP, null, true, aiActionCount );
					dSumDiscountedReward += dDiscountedReward;
					cTrials++;
					if( dDiscountedReward > dMaxDiscountedReward )
						dMaxDiscountedReward = dDiscountedReward;
					if( dDiscountedReward < dMinDiscountedReward )
						dMinDiscountedReward = dDiscountedReward;
					
					if( pomdp.getBeliefStateFactory().getBeliefStateCount() >= cReportPoints ){
						Logger.getInstance().logln( "Collected " + pomdp.getBeliefStateFactory().getBeliefStateCount() + 
								" points, discounted reward = " + dSumDiscountedReward / cTrials +
								" over " + cTrials + " trials " +
								" max = " + dMaxDiscountedReward +
								" min = " + dMinDiscountedReward );
						cReportPoints += 10;
					}
						
				}
				
				Logger.getInstance().logln( "Collected discounted reward = " + dSumDiscountedReward / cTrials +
						" over " + cTrials + " trials " +
						" max = " + dMaxDiscountedReward +
						" min = " + dMinDiscountedReward );
				
				if( bRandom ){
					pomdp.getBeliefStateFactory().saveBeliefSpace( sPath + "/" + pomdp.getName() + "/RandomBeliefSpace" + iBeliefSpace + ".xml" );
				}
				else{
					pomdp.getBeliefStateFactory().saveBeliefSpace( sPath + "/" + pomdp.getName() + "/BeliefSpace" + iBeliefSpace + ".xml" );
				}
				iBeliefSpace++;
				
				Logger.getInstance().logln( "Done seed = " + iSeed +
						" |B| = " + pomdp.getBeliefStateFactory().getBeliefStateCount() +
						" max memory " + rtRuntime.maxMemory() / 1000000 + 
						" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free memory " + rtRuntime.freeMemory() / 1000000 );
				pomdp.getBeliefStateFactory().clear();
				Logger.getInstance().logln( "After memory release " + 
						" max memory " + rtRuntime.maxMemory() / 1000000 + 
						" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
						" free memory " + rtRuntime.freeMemory() / 1000000 );
			}
			cBeliefPoints += iIncrementSize;
		}
	}
	
	
	protected static void createOutlyingSpace( POMDP pomdp, String sPath, String sModelName, int cBeliefPoints ) throws IOException, TransformerException, ParserConfigurationException{
		Runtime rtRuntime = Runtime.getRuntime();
		Logger.getInstance().logln( "Creating outlying belief space" );
		Vector vBeliefPoints = computeOutlyingBeliefPoints( pomdp, cBeliefPoints );
		pomdp.getBeliefStateFactory().saveBeliefPoints( sPath + "/" + sModelName + "/OutlyingBeliefSpace-L2-" + cBeliefPoints + ".xml", vBeliefPoints );
		Logger.getInstance().logln( "Done " +
				" |B| = " + pomdp.getBeliefStateFactory().getBeliefStateCount() +
				" max memory " + rtRuntime.maxMemory() / 1000000 + 
				" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
				" free memory " + rtRuntime.freeMemory() / 1000000 );
		pomdp.getBeliefStateFactory().clear();
		Logger.getInstance().logln( "After memory release " + 
				" max memory " + rtRuntime.maxMemory() / 1000000 + 
				" actual memory " + ( rtRuntime.totalMemory() - rtRuntime.freeMemory() ) / 1000000 +
				" free memory " + rtRuntime.freeMemory() / 1000000 );
	}
	
	protected static Vector<BeliefState> createOutlyingSpace( POMDP pomdp, int cBeliefPoints ){
		Runtime rtRuntime = Runtime.getRuntime();
		Logger.getInstance().logln( "Creating outlying belief space" );
		Vector<BeliefState> vBeliefPoints = computeOutlyingBeliefPoints( pomdp, cBeliefPoints );
		Logger.getInstance().logln( "Done creating outlying belief space" );
		return vBeliefPoints;
	}
	
	public static void main( String[] args ){
		String sPath = ExecutionProperties.getPath();
		int cBeliefPoints = 200;
		int cMachines = 11;
		//String sModelName = "NetworkManagement" + cMachines;
		String sModelName = "ModifiedRockSample";
		if( args.length > 0 )
			sModelName = args[0];
		if( args.length > 1 )
			cBeliefPoints = Integer.parseInt( args[1] );
		Runtime rtRuntime = Runtime.getRuntime();
				
		try{
			//MDPValueFunction.setPersistanceFile( sPath + "/Models/" + sModelName + "QMDP.xml" );
			//POMDP pomdp = new NetworkManagement( cMachines );//POMDP();
			POMDP pomdp = null;
			if( sModelName.equals( "ModifiedRockSample" ) ){
				pomdp = new ModifiedRockSample( 8, 8, 3, 20);
			}
			else{
				pomdp = new POMDP();
				pomdp.load( sPath + "/Models/" + sModelName + ".POMDP" );
			}
			//createRandomSpaces( pomdp, sPath, 5, cBeliefPoints );
			//createOutlyingSpace( pomdp, sPath, sModelName, 1000 );
			createIncreasingSizeRandomSpaces( pomdp, sPath, 100, 100, 10, 5, false );
			createIncreasingSizeRandomSpaces( pomdp, sPath, 100, 100, 10, 5, true );
		}
		catch( Exception e ){
			e.printStackTrace();
			Logger.getInstance().logln( e );
		} 
		catch( Error err ){
			Logger.getInstance().logln( err );
			err.printStackTrace();
			Logger.getInstance().logln(
					" free memory " + rtRuntime.freeMemory() / 1000000 +
					" total memory " + rtRuntime.totalMemory() / 1000000 +
					" max memory " + rtRuntime.maxMemory() / 1000000 +
				"" );
		}
	}
}
