package pomdp.utilities.distance;


public class LInfDistance extends LDistance {

	protected static LInfDistance m_lifDistance = null;
	
	public LInfDistance(){
		super();
	}
	
	protected double applyDistanceMetric( double dAccumulated, double dValue1, double dValue2 ){
		double dDiff = Math.abs( dValue1 - dValue2 );
		if( dDiff > dAccumulated )
			return dDiff;
		return dAccumulated;
	}

	protected double applyFinal( double dAccumulated ){
		return dAccumulated;
	}

	public static DistanceMetric getInstance() {
		if( m_lifDistance == null )
			m_lifDistance = new LInfDistance();
		return m_lifDistance;
	}
}
