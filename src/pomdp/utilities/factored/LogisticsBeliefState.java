package pomdp.utilities.factored;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.environments.Logistics;
import pomdp.environments.ModifiedRockSample;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;

public class LogisticsBeliefState extends BeliefState implements PathProbabilityEstimator{
	private double[][] m_adPackageLocation;
	private double[][] m_adTruckLocation;
	private int m_cCities, m_cTrucks, m_cPackages;
	private LogisticsBeliefState[][] m_aSuccessors;
	private Logistics m_pLogisitics;
	private int m_iID;
	private static int m_cBeliefStates = 0;
	private static int m_cBeliefUpdateRequests = 0, m_cRealBeliefUpdates = 0;
	
	public LogisticsBeliefState( Logistics pomdp, LogisticsBeliefStateFactory bsf ){
		super( pomdp.getStateCount(), pomdp.getActionCount(), pomdp.getObservationCount(), -1, false, bsf );
		m_bCacheBeliefStates = true;
		m_pLogisitics = pomdp;
		m_cCities = pomdp.getCitiesCount();
		m_cTrucks = pomdp.getTrucksCount();
		m_cPackages = pomdp.getPackageCount();
		initPackageLocations( null );
		initTruckLocations( null );
		m_iID = m_cBeliefStates++;
		
		if( m_cBeliefStates % 1000 == 0 )
			Logger.getInstance().logFull( "LogisticsBeliefState", 0, "<init>", "Created " + m_cBeliefStates + " belief states (" + m_cRealBeliefUpdates * 1.0 / m_cBeliefUpdateRequests + ")" );
		
		if( m_bCacheBeliefStates ){
			m_aSuccessors = new LogisticsBeliefState[m_cActions][m_cObservations];
			m_aCachedObservationProbabilities = new double[m_cActions][m_cObservations];
			for( double[] a : m_aCachedObservationProbabilities ){
				for( int i = 0 ; i < a.length ; i++ ){
					a[i] = Double.NEGATIVE_INFINITY;
				}
			}
		}
		//Logger.getInstance().logln( this );
	}
	
	private double normalize( double[][] adProbs ){
		int i = 0, j = 0;
		double dSum = 0.0, dTotal = 0.0;
		for( i = 0 ; i< adProbs.length ; i++ ){
			dSum = 0.0;
			for( j = 0 ; j < adProbs[i].length ; j++ ){
				dSum += adProbs[i][j];
			}
			dTotal += dSum;
			for( j = 0 ; j < adProbs[i].length ; j++ ){
				adProbs[i][j] /= dSum;
			}
		}
		return dTotal;
	}
	
	private void initPackageLocations( double[][] adPackageLocation ){
		m_adPackageLocation = new double[m_cPackages][m_cCities + m_cTrucks];
		int iLocation = 0, iPackage = 0;
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			for( iLocation = 0 ; iLocation < m_cCities + m_cTrucks ; iLocation++ ){
				if( adPackageLocation != null ){
					m_adPackageLocation[iPackage][iLocation] = adPackageLocation[iPackage][iLocation];
				}
				else{
					if( iLocation < m_cCities )
						m_adPackageLocation[iPackage][iLocation] = 1.0 / m_cCities;
				}
			}
		}		
	}
	
	private void initTruckLocations( double[][] adTruckLocation ){
		m_adTruckLocation = new double[m_cTrucks][m_cCities];
		int iLocation = 0, iTruck = 0;
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			for( iLocation = 0 ; iLocation < m_cCities ; iLocation++ ){
				if( adTruckLocation != null ){
					m_adTruckLocation[iTruck][iLocation] = adTruckLocation[iTruck][iLocation];
				}
				else{
					m_adTruckLocation[iTruck][iLocation] = 1.0 / m_cCities;
				}
			}
		}		
	}
	
	private LogisticsBeliefState( LogisticsBeliefState bs ){
		this( bs.m_pLogisitics, (LogisticsBeliefStateFactory)bs.m_bsFactory );
		initTruckLocations( bs.m_adTruckLocation );
		initPackageLocations( bs.m_adPackageLocation );
	}
	
	public double valueAt( int iState )
	{
		int iLocation = 0, iTruck = 0, iPackage = 0;
		double dProb = 1.0;
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			iLocation = m_pLogisitics.getTruckLocation( iState, iTruck );
			dProb *= m_adTruckLocation[iTruck][iLocation];
		}
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			iLocation = m_pLogisitics.getPackageLocation( iState, iPackage );
			dProb *= m_adPackageLocation[iPackage][iLocation];
		}
		return dProb;
	}
	
	public synchronized BeliefState nextBeliefState( int iAction, int iObservation ){
		LogisticsBeliefState bsNext = null;
		m_cBeliefUpdateRequests++;
		if( m_bCacheBeliefStates ){
			if( m_aCachedObservationProbabilities[iAction][iObservation] == Double.NEGATIVE_INFINITY ){
				bsNext = computeNextBeliefState( iAction, iObservation );
				m_aSuccessors[iAction][iObservation] = bsNext;
			}
			else{
				bsNext = m_aSuccessors[iAction][iObservation];
			}
		}
		else{
			bsNext = computeNextBeliefState( iAction, iObservation );
		}
		return bsNext;
	}
	
	private LogisticsBeliefState computeNextBeliefState( int iAction, int iObservation ){
		LogisticsBeliefState bsNew = new LogisticsBeliefState( this );
		double dProbOGivenA = bsNew.applyActionAnObservation( this, iAction, iObservation );
		m_aCachedObservationProbabilities[iAction][iObservation] = dProbOGivenA;
		m_cRealBeliefUpdates++;
		return bsNew;
	}
	
	public double probabilityOGivenA( int iAction, int iObservation ){
		/*
		LogisticsBeliefState bsNew = new LogisticsBeliefState( this );
		double dProb = bsNew.applyActionAnObservation( this, iAction, iObservation );
		return dProb;
		*/
		if( m_aCachedObservationProbabilities[iAction][iObservation] == Double.NEGATIVE_INFINITY )
			nextBeliefState( iAction, iObservation );
		return m_aCachedObservationProbabilities[iAction][iObservation];
	}
	
	
	private double applyActionAnObservation( LogisticsBeliefState bs, int iAction, int iObservation ){
		double dProbOGivenA = 0.0;
		if( m_pLogisitics.isDriveAction( iAction ) ){
			int iTruck = m_pLogisitics.getActiveTruck( iAction );
			int iDestination = m_pLogisitics.getTruckDestination( iAction );
			int iNewLocation = 0, iPreviousLocation = 0;
			double dProbSuccessObservation = 0.0;
			if( iObservation > 1 ) //impossible observation
				return 0.0;
			
			double dTr = 0.0, dPrNewLocation = 0.0;
			
			for( iNewLocation = 0 ; iNewLocation < m_cCities ; iNewLocation++ ){
				dPrNewLocation = 0.0;
				if( iNewLocation == iDestination ){
					if( iObservation == 1 )
						dProbSuccessObservation = 0.9;
					else
						dProbSuccessObservation = 0.1;
					dTr = 0.75;
					dPrNewLocation += bs.m_adTruckLocation[iTruck][iDestination] * dProbSuccessObservation;					
				}
				else{
					if( iObservation == 1 )
						dProbSuccessObservation = 0.1;
					else
						dProbSuccessObservation = 0.9;
					dTr = 0.25 / ( m_cCities - 1 );
				}
				for( iPreviousLocation = 0 ; iPreviousLocation < m_cCities ; iPreviousLocation++ ){
					if( iPreviousLocation != iDestination ){
						dPrNewLocation += bs.m_adTruckLocation[iTruck][iPreviousLocation] * dTr * dProbSuccessObservation;
					}
				}
				m_adTruckLocation[iTruck][iNewLocation] = dPrNewLocation;
				dProbOGivenA += dPrNewLocation;
			}
		}
		else if( m_pLogisitics.isLoadUnloadAction( iAction ) ){
			int iPackage = m_pLogisitics.getActivePackage( iAction );
			int iActiveTruck = m_pLogisitics.getActiveTruck( iAction );
			int iCity = 0, iTruck = 0, iTerminalCity = m_pLogisitics.getTerminalLocation( iPackage );
			double dProbOnTruckObservation = 0.0;
			if( iObservation > 1 )
				return 0.0;
			
			int iPackagePreviousLocation = 0, iPackageNewLocation = 0;
			double dObservationProb = 0.0;
			double dProbNewLocation = 0.0;
			
			//first iterate over the cities
			for( iPackageNewLocation = 0 ; iPackageNewLocation < m_cCities ; iPackageNewLocation++ ){
				dProbNewLocation = 0.0;
				
				if( iObservation == 0 )
					dObservationProb = 0.9;
				else
					dObservationProb = 0.1;
				
				//first iterate over the cities
				for( iPackagePreviousLocation = 0 ; iPackagePreviousLocation < m_cCities ; iPackagePreviousLocation++ ){
					if( iPackageNewLocation == iPackagePreviousLocation ){
						if( iPackageNewLocation == iTerminalCity ){
							//package is in the terminal city - it cannot be moved
							dProbNewLocation += bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * dObservationProb;
						}
						else{
							//package was in a non terminal city and remained there
							//package was in the city and the truck was not in the city
							dProbNewLocation += bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * 
								( 1 - bs.m_adTruckLocation[iActiveTruck][iPackagePreviousLocation] ) * dObservationProb;
							//package and truck were at the city but the operation failed
							dProbNewLocation += bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] *
								bs.m_adTruckLocation[iActiveTruck][iPackagePreviousLocation] * 0.1 * dObservationProb;
						}
					}
					else{
						//nothing here - package cannot get from city i to city j using load/unload
					}
				}
				//now iterate over the trucks
				for( iPackagePreviousLocation = m_cCities ; iPackagePreviousLocation < m_cCities + m_cTrucks ; iPackagePreviousLocation++ ){
					if( iPackagePreviousLocation - m_cCities == iActiveTruck ){
						//package was on the specific truck
						//truck was in the city, package was on the truck, and operation succeeded
						dProbNewLocation += bs.m_adTruckLocation[iActiveTruck][iPackageNewLocation] * 
							bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * 0.9 * dObservationProb;
					}
					else{
						//package was on another truck - it could not get to the city using this operation
					}
				}
				
				m_adPackageLocation[iPackage][iPackageNewLocation] = dProbNewLocation;
				dProbOGivenA += dProbNewLocation;
			}
			//now iterate over the trucks
			for( iPackageNewLocation = m_cCities ; iPackageNewLocation < m_cCities + m_cTrucks ; iPackageNewLocation++ ){
				dProbNewLocation = 0.0;
				
				if( iPackageNewLocation - m_cCities == iActiveTruck ){
					if( iObservation == 1 )
						dObservationProb = 0.9;
					else
						dObservationProb = 0.1;
				}
				else{
					if( iObservation == 0 )
						dObservationProb = 0.9;
					else
						dObservationProb = 0.1;
				}
				
				//first iterate over the cities
				for( iPackagePreviousLocation = 0 ; iPackagePreviousLocation < m_cCities ; iPackagePreviousLocation++ ){
					if( iPackageNewLocation - m_cCities == iActiveTruck ){
						if( iPackagePreviousLocation == iTerminalCity ){
							//package was in the terminal city - it cannot be moved
						}
						else{
							//package and truck were both in the city and the load succeeded
							dProbNewLocation += bs.m_adTruckLocation[iActiveTruck][iPackagePreviousLocation] *
								bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * 0.9 * dObservationProb;
						}
					}					
					else{
						//if the package is on another truck it couldn't have been in a city previously
					}
				}
				//now iterate over the trucks
				for( iPackagePreviousLocation = m_cCities ; iPackagePreviousLocation < m_cCities + m_cTrucks ; iPackagePreviousLocation++ ){
					if( iPackageNewLocation == iPackagePreviousLocation ){
						if( iPackageNewLocation - m_cCities == iActiveTruck ){
							//new and previous location is the active truck - the operation failed
							dProbNewLocation += bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * 0.1 * dObservationProb;
						}
						else{
							//package is not on the active truck - the operation does not affect the result
							dProbNewLocation += bs.m_adPackageLocation[iPackage][iPackagePreviousLocation] * dObservationProb;							
						}
					}
					else{
						//the package cannot move between trucks
					}
				}
				m_adPackageLocation[iPackage][iPackageNewLocation] = dProbNewLocation;
				dProbOGivenA += dProbNewLocation;

			}
			
			/*
			if( iObservation == 1 )
				dProbOnTruckObservation = 0.9;
			else
				dProbOnTruckObservation = 0.1;
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				
				if( iCity == iTerminalCity ){
					//package cannot be moved from its destination location
					m_adPackageLocation[iPackage][iCity] = bs.m_adPackageLocation[iPackage][iCity] * ( 1 - dProbOnTruckObservation );
				}
				else{
					m_adPackageLocation[iPackage][iCity] = 
						bs.m_adPackageLocation[iPackage][iCity] * bs.m_adTruckLocation[iActiveTruck][iCity] * 0.1 * ( 1 - dProbOnTruckObservation ) +
						//package and truck in the city but the load failed
						bs.m_adPackageLocation[iPackage][iCity] * ( 1 - bs.m_adTruckLocation[iActiveTruck][iCity] ) * ( 1 - dProbOnTruckObservation ) +
						//truck not in the city
						bs.m_adPackageLocation[iPackage][m_cCities + iActiveTruck] * bs.m_adTruckLocation[iActiveTruck][iCity] * 0.9 * ( 1 - dProbOnTruckObservation );
						//package was on the truck and successfully unloaded
				}
				dProbOGivenA += m_adPackageLocation[iPackage][iCity];
			}
			for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
				if( iTruck == iActiveTruck ){
					double dSumTruckAndPackageInSameCity = 0.0;
					for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
						if( iCity != iTerminalCity )
							dSumTruckAndPackageInSameCity += bs.m_adPackageLocation[iPackage][iCity] * bs.m_adTruckLocation[iActiveTruck][iCity];
					}
					m_adPackageLocation[iPackage][m_cCities + iTruck] = 
						//truck and package were at the same city and package was successfully loaded
						dSumTruckAndPackageInSameCity * 0.9 * dProbOnTruckObservation + 
						//package was on truck and was not unloaded
						bs.m_adPackageLocation[iPackage][m_cCities + iActiveTruck] * 0.1 * dProbOnTruckObservation;
				}
				else{
					m_adPackageLocation[iPackage][m_cCities + iTruck] = 
						//if package was on another truck, it stays there
						bs.m_adPackageLocation[iPackage][m_cCities + iTruck];
				}
				dProbOGivenA += m_adPackageLocation[iPackage][m_cCities + iTruck];
			}
			*/
		}
		else{ //ping action
			if( m_pLogisitics.isPingPackage( iAction ) ){
				if( iObservation >= m_cCities + m_cTrucks )
					return 0.0;
				int iPackage = m_pLogisitics.getActivePackage( iAction );
				int iPackageLocation = 0;
				for( iPackageLocation = 0 ; iPackageLocation < m_cCities + m_cTrucks ; iPackageLocation++ ){
					if( iObservation == iPackageLocation ){
						m_adPackageLocation[iPackage][iPackageLocation] = 
							bs.m_adPackageLocation[iPackage][iPackageLocation] * 0.8;
					}
					else{
						m_adPackageLocation[iPackage][iPackageLocation] = 
							bs.m_adPackageLocation[iPackage][iPackageLocation] * 0.2 / ( m_cCities + m_cTrucks - 1 );
					}
					dProbOGivenA += m_adPackageLocation[iPackage][iPackageLocation];
				}
			}
			else if( m_pLogisitics.isPingTruck( iAction ) ){
				if( iObservation >= m_cCities )
					return 0.0;
				int iTruck = m_pLogisitics.getActiveTruck( iAction );
				int iTruckLocation = 0;
				for( iTruckLocation = 0 ; iTruckLocation < m_cCities ; iTruckLocation++ ){
					if( iObservation == iTruckLocation ){
						m_adTruckLocation[iTruck][iTruckLocation] = bs.m_adTruckLocation[iTruck][iTruckLocation] * 0.8;
					}
					else{
						m_adTruckLocation[iTruck][iTruckLocation] = bs.m_adTruckLocation[iTruck][iTruckLocation] *  
							0.2 / ( m_cCities - 1 );						
					}
					dProbOGivenA += m_adTruckLocation[iTruck][iTruckLocation];
				}
			}
		}
		normalize( m_adPackageLocation );
		normalize( m_adTruckLocation );
		return dProbOGivenA;
	}

	
	private double applyActionAnObservationOld( LogisticsBeliefState bs, int iAction, int iObservation ){
		double dProbOGivenA = 0.0;
		if( m_pLogisitics.isDriveAction( iAction ) ){
			int iTruck = m_pLogisitics.getActiveTruck( iAction );
			int iDestination = m_pLogisitics.getTruckDestination( iAction );
			int iLocation = 0;
			double dProbSuccessObservation = 0.0;
			if( iObservation > 1 ) //impossible observation
				return 0.0;
			if( iObservation == 1 )
				dProbSuccessObservation = 0.9;
			else
				dProbSuccessObservation = 0.1;
			for( iLocation = 0 ; iLocation < m_cCities ; iLocation++ ){
				if( iLocation == iDestination ){
					m_adTruckLocation[iTruck][iLocation] = ( bs.m_adTruckLocation[iTruck][iLocation] + 
						( 1 - bs.m_adTruckLocation[iTruck][iLocation] ) * 0.75 ) * dProbSuccessObservation;
				}
				else{
					m_adTruckLocation[iTruck][iLocation] = ( 1 - bs.m_adTruckLocation[iTruck][iLocation] ) * 
						0.25 / ( m_cCities - 1 ) * 
						( 1 - dProbSuccessObservation );
				}
				dProbOGivenA += m_adTruckLocation[iTruck][iLocation];
			}
		}
		else if( m_pLogisitics.isLoadUnloadAction( iAction ) ){
			int iPackage = m_pLogisitics.getActivePackage( iAction );
			int iActiveTruck = m_pLogisitics.getActiveTruck( iAction );
			int iCity = 0, iTruck = 0, iTerminalCity = m_pLogisitics.getTerminalLocation( iPackage );
			double dProbOnTruckObservation = 0.0;
			if( iObservation > 1 )
				return 0.0;
			if( iObservation == 1 )
				dProbOnTruckObservation = 0.9;
			else
				dProbOnTruckObservation = 0.1;
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				
				if( iCity == iTerminalCity ){
					//package cannot be moved from its destination location
					m_adPackageLocation[iPackage][iCity] = bs.m_adPackageLocation[iPackage][iCity] * ( 1 - dProbOnTruckObservation );
				}
				else{
					m_adPackageLocation[iPackage][iCity] = 
						bs.m_adPackageLocation[iPackage][iCity] * bs.m_adTruckLocation[iActiveTruck][iCity] * 0.1 * ( 1 - dProbOnTruckObservation ) +
						//package and truck in the city but the load failed
						bs.m_adPackageLocation[iPackage][iCity] * ( 1 - bs.m_adTruckLocation[iActiveTruck][iCity] ) * ( 1 - dProbOnTruckObservation ) +
						//truck not in the city
						bs.m_adPackageLocation[iPackage][m_cCities + iActiveTruck] * bs.m_adTruckLocation[iActiveTruck][iCity] * 0.9 * ( 1 - dProbOnTruckObservation );
						//package was on the truck and successfully unloaded
				}
				dProbOGivenA += m_adPackageLocation[iPackage][iCity];
			}
			for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
				if( iTruck == iActiveTruck ){
					double dSumTruckAndPackageInSameCity = 0.0;
					for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
						if( iCity != iTerminalCity )
							dSumTruckAndPackageInSameCity += bs.m_adPackageLocation[iPackage][iCity] * bs.m_adTruckLocation[iActiveTruck][iCity];
					}
					m_adPackageLocation[iPackage][m_cCities + iTruck] = 
						//truck and package were at the same city and package was successfully loaded
						dSumTruckAndPackageInSameCity * 0.9 * dProbOnTruckObservation + 
						//package was on truck and was not unloaded
						bs.m_adPackageLocation[iPackage][m_cCities + iActiveTruck] * 0.1 * dProbOnTruckObservation;
				}
				else{
					m_adPackageLocation[iPackage][m_cCities + iTruck] = 
						//if package was on another truck, it stays there
						bs.m_adPackageLocation[iPackage][m_cCities + iTruck];
				}
				dProbOGivenA += m_adPackageLocation[iPackage][m_cCities + iTruck];
			}
		}
		else{ //ping action
			if( m_pLogisitics.isPingPackage( iAction ) ){
				if( iObservation >= m_cCities + m_cTrucks )
					return 0.0;
				int iPackage = m_pLogisitics.getActivePackage( iAction );
				int iPackageLocation = 0;
				for( iPackageLocation = 0 ; iPackageLocation < m_cCities + m_cTrucks ; iPackageLocation++ ){
					if( iObservation == iPackageLocation ){
						m_adPackageLocation[iPackage][iPackageLocation] = 
							bs.m_adPackageLocation[iPackage][iPackageLocation] * 0.8;
					}
					else{
						m_adPackageLocation[iPackage][iPackageLocation] = 
							bs.m_adPackageLocation[iPackage][iPackageLocation] * 0.2 / ( m_cCities + m_cTrucks - 1 );
					}
					dProbOGivenA += m_adPackageLocation[iPackage][iPackageLocation];
				}
			}
			else if( m_pLogisitics.isPingTruck( iAction ) ){
				if( iObservation >= m_cCities )
					return 0.0;
				int iTruck = m_pLogisitics.getActiveTruck( iAction );
				int iTruckLocation = 0;
				for( iTruckLocation = 0 ; iTruckLocation < m_cCities ; iTruckLocation++ ){
					if( iObservation == iTruckLocation ){
						m_adTruckLocation[iTruck][iTruckLocation] = bs.m_adTruckLocation[iTruck][iTruckLocation] * 0.8;
					}
					else{
						m_adTruckLocation[iTruck][iTruckLocation] = bs.m_adTruckLocation[iTruck][iTruckLocation] * 
							0.2 / ( m_cCities - 1 );						
					}
					dProbOGivenA += m_adTruckLocation[iTruck][iTruckLocation];
				}
			}
		}
		normalize( m_adPackageLocation );
		normalize( m_adTruckLocation );
		return dProbOGivenA;
	}
	
	public String toString(){
		String s = "";
		int iTruck = 0, iCity = 0, iPackage = 0;
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			s += "T" + iTruck + " - ";
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				s += m_adTruckLocation[iTruck][iCity] + " ";
			}
			s += "\n";
		}
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			s += "P" + iPackage + " - ";
			for( iCity = 0 ; iCity < m_cCities + m_cTrucks ; iCity++ ){
				s += m_adPackageLocation[iPackage][iCity] + " ";
			}
			s += "\n";
		}
		return s;
	}
	
	public boolean equals( LogisticsBeliefState lbs ){
		int iTruck = 0, iCity = 0, iPackage = 0;
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			for( iCity = 0 ; iCity < m_cCities ; iCity++ ){
				if( Math.abs( m_adTruckLocation[iTruck][iCity] - lbs.m_adTruckLocation[iTruck][iCity] ) > 0.001 )
					return false;
			}
		}
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			for( iCity = 0 ; iCity < m_cCities + m_cTrucks ; iCity++ ){
				if( Math.abs( m_adPackageLocation[iPackage][iCity] - lbs.m_adPackageLocation[iPackage][iCity] ) > 0.001 )
					return false;
			}
		}
		return true;
	}
	
	public boolean equals( Object oOther ){
		if( oOther == this )
			return true;
		if( oOther instanceof LogisticsBeliefState ){
			LogisticsBeliefState lbs = (LogisticsBeliefState)oOther;
			return equals( lbs );
		}
		return false;
	}

	public double computeImmediateReward( int iAction ) {
		double dSumRewards = 0.0;
		if( m_pLogisitics.isDriveAction( iAction ) ){
			int iTruck = m_pLogisitics.getActiveTruck( iAction );
			int iDestination = m_pLogisitics.getTruckDestination( iAction );
			int iLocation = 0;
			for( iLocation = 0 ; iLocation < m_cCities ; iLocation++ ){
				if( iLocation == iDestination )
					dSumRewards += m_adTruckLocation[iTruck][iLocation] * -0.5;
				else
					dSumRewards += m_adTruckLocation[iTruck][iLocation] * -1 * ( iTruck + 1 );				
			}
		}
		else if( m_pLogisitics.isLoadUnloadAction( iAction ) ){
			int iPackage = m_pLogisitics.getActivePackage( iAction );
			int iTruck = m_pLogisitics.getActiveTruck( iAction );
			int iLocation = 0;
			for( iLocation = 0 ; iLocation < m_cCities ; iLocation++ ){
				if( m_pLogisitics.isTerminalLocation( iPackage, iLocation ) )
					dSumRewards += m_adPackageLocation[iPackage][m_cCities + iTruck] * m_adTruckLocation[iTruck][iLocation] * 10.0 +
						( 1 - m_adPackageLocation[iPackage][m_cCities + iTruck] ) * m_adTruckLocation[iTruck][iLocation] * -0.1;
				else
					dSumRewards += m_adTruckLocation[iTruck][iLocation] * -0.1;				
			}
		}
		else{//ping
			dSumRewards = -0.1;
		}
		return dSumRewards;
	}

	@Override
	public double valueAt( Vector<Pair<Integer, Boolean>> vAssignment ) {
		int iTruck = 0, iPackage = 0;
		Collection<Integer> cLocations = null;
		double dProbTruck = 0.0, dProbPackage = 0.0;
		double dProb = 1.0;
		if( vAssignment.isEmpty() )
			return 1.0;
		for( iTruck = 0 ; iTruck < m_cTrucks ; iTruck++ ){
			cLocations =  m_pLogisitics.getTruckLocation( vAssignment, iTruck );
			if( !cLocations.isEmpty() ){
				dProbTruck = 0.0;
				for( int iLocation : cLocations ){
					if( ( iLocation >= 0 ) && ( iLocation < m_cCities ) ){
						dProbTruck += m_adTruckLocation[iTruck][iLocation];
					}
				}
				dProb *= dProbTruck;
			}
		}
		for( iPackage = 0 ; iPackage < m_cPackages ; iPackage++ ){
			cLocations =  m_pLogisitics.getPackageLocation( vAssignment, iPackage );
			if( !cLocations.isEmpty() ){
				dProbPackage = 0.0;
				for( int iLocation : cLocations ){
					if( ( iLocation >= 0 ) && ( iLocation < m_cCities + m_cTrucks ) ){
						dProbPackage += m_adPackageLocation[iPackage][iLocation];
					}
				}
				dProb *= dProbPackage;
			}
		}
		return dProb;
	}

	@Override
	public void clearZeroEntries() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int countEntries() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<Entry<Integer, Double>> getDominatingNonZeroEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Entry<Integer, Double>> getNonZeroEntries() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNonZeroEntriesCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setValueAt(int state, double value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
