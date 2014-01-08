package pomdp.utilities.factored;

import java.util.Comparator;

public class NetworkBeliefStateComparator implements Comparator {

	private int m_cMachines;
	
	public NetworkBeliefStateComparator( int cMachines ){
		m_cMachines = cMachines;
	}
	
	protected double diff( double d1, double d2 ){
		if( d1 > d2 )
			return d1 - d2;
		return d2 - d1;
	}
	
	public int compare( NetworkBeliefState bs1, NetworkBeliefState bs2 ){
		int iMachine = 0;
		double dProb1 = 0.0, dProb2 = 0.0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			dProb1 = bs1.getMachineWorkingProb( iMachine );
			dProb2 = bs2.getMachineWorkingProb( iMachine );
			if( diff( dProb1, dProb2 ) > 0.0001 ){
				if( dProb1 > dProb2 )
					return 1;
				else
					return -1;
			}
		}
		return 0;
	}
	
	public int compare( Object o1, Object o2 ){
		if( o1 == o2 )
			return 0;
		if( ( o1 instanceof NetworkBeliefState ) && ( o2 instanceof NetworkBeliefState ) ){
			NetworkBeliefState bs1 = (NetworkBeliefState)o1;
			NetworkBeliefState bs2 = (NetworkBeliefState)o2;
			int iResult = compare( bs1, bs2 );
			//Logger.getInstance().logln( "Comparing to " + bs2 + " result = " + iResult );
			return iResult;
		}
		return 0;
	}

}
