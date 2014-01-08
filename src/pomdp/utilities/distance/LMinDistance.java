package pomdp.utilities.distance;


public class LMinDistance extends LDistance {

	protected static LMinDistance m_lmDistance = null;
	
	protected LMinDistance(){
		super();
	}
	
	protected double getInitialDistance(){
		return 1.0;
	}
	
	protected double applyDistanceMetric( double dAccumulated, double dValue1, double dValue2 ){
		if( ( dValue1 == 0.0 ) && ( dValue2 == 0.0 ) )
			return dAccumulated;
		double dDiff = Math.abs( dValue1 - dValue2 );
		if( dDiff < dAccumulated )
			return dDiff;
		return dAccumulated;
	}

	protected double applyFinal( double dAccumulated ){
		return dAccumulated;
	}

	public static DistanceMetric getInstance() {
		if( m_lmDistance == null )
			m_lmDistance = new LMinDistance();
		return m_lmDistance;
	}
}
