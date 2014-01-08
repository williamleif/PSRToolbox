package pomdp.utilities.concurrent;

import java.util.StringTokenizer;
import java.util.Vector;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.algorithms.pointbased.ForwardSearchValueIteration;
import pomdp.algorithms.pointbased.HeuristicSearchValueIteration;
import pomdp.algorithms.pointbased.PerseusValueIteration;
import pomdp.algorithms.pointbased.PointBasedValueIteration;
import pomdp.algorithms.pointbased.PrioritizedValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class MultipleAlgortihmsTest {
	
	public static void getAllCombinations( String[] asMethods, Vector<String> vAll, int iMethod, String sCurrent ){
		if( iMethod == asMethods.length ){
			if( sCurrent.length() > 0 )
				vAll.add( sCurrent );
			return;
		}
		getAllCombinations( asMethods, vAll, iMethod + 1, sCurrent + asMethods[iMethod] + ";" );
		getAllCombinations( asMethods, vAll, iMethod + 1, sCurrent );
	}
	
	public static void runMultipleAlgorithms( String sModelName, String sPath, String sMethods ){
		try{
			POMDP pomdp = new POMDP();
			pomdp.load( sPath + sModelName + ".POMDP" );
			ThreadPool.createInstance( pomdp );
			//MDPValueFunction.persistQValues( true );
			int cThreads = 1, iThread = 0;
			for( int i = 0 ; i < sMethods.length() ; i++ ){
				if( sMethods.charAt( i ) == ';' ){
					cThreads++;
				}
			}
			StringTokenizer st = new StringTokenizer( sMethods, ";" );
			Task[] aThreads = new Task[cThreads];
			Task aEvaluateTask = null;
			ValueIteration viAlgorithm = null;
			LinearValueFunctionApproximation vValueFunction = null;
			double dEpsilon = 0.0001;
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				String sMethodName = st.nextToken();
				viAlgorithm = null;
				if( sMethodName.equals( "Pruning" ) ){
					aThreads[iThread] = new VectorPruningTask( pomdp, vValueFunction );
				}
				else if( sMethodName.equals( "Evaluate" ) ){
					aThreads[iThread] = new PolicyEvaluationTask( pomdp, vValueFunction, sMethods, 15, 5000, 1000, 150 );
					aEvaluateTask = aThreads[iThread];
				}
				else{
					if( sMethodName.equals( "PVI" ) ){
						/*
						vBeliefPoints = CreateBeliefSpaces.createHeuristicSpace( pomdp, iThread, 400 );
						//doing it here because otherwise there's a bug when doing Bellman error updates or backups - somehow the successors are incorrect
						for( BeliefState bs : vBeliefPoints ){
							for( int iAction = 0 ; iAction < pomdp.getActionCount() ; iAction++ )
								bs.getSortedSuccessors( iAction );
						}
						*/
						viAlgorithm = new PrioritizedValueIteration( pomdp );
					}
					else if( sMethodName.equals( "Perseus" ) ){
						viAlgorithm = new PerseusValueIteration( pomdp );
					}
					else if( sMethodName.equals( "PBVI" ) ){
						viAlgorithm = new PointBasedValueIteration( pomdp );
					}
					else if( sMethodName.equals( "PBVIR" ) ){
						viAlgorithm = new PointBasedValueIteration( pomdp, true );
					}
					else if( sMethodName.equals( "HSVI" ) ){
						viAlgorithm = new HeuristicSearchValueIteration( pomdp, true );
					}
					else if( sMethodName.equals( "HSVIR" ) ){
						viAlgorithm = new HeuristicSearchValueIteration( pomdp, 0.1, true );
					}
					else if( sMethodName.equals( "FSVI" ) ){
						viAlgorithm = new ForwardSearchValueIteration( pomdp );
					}
					if( vValueFunction == null )
						vValueFunction = viAlgorithm.getValueFunction();
					else
						viAlgorithm.setValueFunction( vValueFunction );
					aThreads[iThread] = new ValueIterationTask( viAlgorithm, 100, dEpsilon );
				}
			}
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				ThreadPool.getInstance().addTask( aThreads[iThread] );
			}
			try{
				if( aEvaluateTask != null ){
					ThreadPool.getInstance().waitForTask( aEvaluateTask );
					for( iThread = 0 ; iThread < cThreads ; iThread++ ){
						aThreads[iThread].terminate();
					}
				}
			}
			catch( Error e ){
				System.err.println( e + " moving to the next combination" );
			}
			for( iThread = 0 ; iThread < cThreads ; iThread++ ){
				ThreadPool.getInstance().waitForTask( aThreads[iThread] );
			}
			
			Runtime rt = Runtime.getRuntime();
			Logger.getInstance().logln( "free " + rt.freeMemory() / 1000000 + ", total " + rt.totalMemory() / 1000000 + ", max " + rt.maxMemory() / 1000000 );
			pomdp = null;
			ThreadPool.getInstance().clear();
			/*
			rt.gc();
			long lBefore = System.currentTimeMillis(), lAfter = 0;
			int i = 1000;
			while( lAfter - lBefore < 10000 ){
				lAfter = System.currentTimeMillis();
				if( lAfter - lBefore > i ){
					Logger.getInstance().logln( "free " + rt.freeMemory() / 1000000 + ", total " + rt.totalMemory() / 1000000 + ", max " + rt.maxMemory() / 1000000 );
					i += 1000;
				}
			}
			*/
		}
		catch( Exception e ){
			e.printStackTrace();
			System.exit( 0 );
		}
	}
	
	public static void main(String[] args ){
		String[] asMethods = { /*"HSVIR", "PBVIR", */"FSVI", "HSVI", "PBVI", "PVI",  };
		//String[] asMethods = { "PBVIR", "HSVIR" };
		String sPath = ExecutionProperties.getPath();
		String sModelName = "Hallway";
		String sMethodName = "HSVI";
		String sAdditional = "Evaluate";
		//String sAdditional = "Evaluate";
		Vector<String> vAllCombinations = new Vector<String>();
		getAllCombinations( asMethods, vAllCombinations, 0, "" );
		
		//for( String sMethods : vAllCombinations )
		//	runMultipleAlgorithms( sModelName, sPath, sMethods + sAdditional );
		
		
		for( int iMethod = 0 ; iMethod < asMethods.length ; iMethod++ ){
			for( int cDuplicates = 1 ; cDuplicates <= 5 ; cDuplicates++ ){
				String sMethods = "";
				//for( int i = 0 ; i < cDuplicates ; i++ )
					sMethods += asMethods[iMethod] + ";";
				sMethods += sAdditional;
				runMultipleAlgorithms( sModelName, sPath, sMethods );					
			}
		}
		
		Logger.getInstance().logln( "All threads terminated - exiting!" );
		System.exit( 0 );

	}

}
