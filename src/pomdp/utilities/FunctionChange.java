package pomdp.utilities;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.utilities.datastructures.Function;

public class FunctionChange {
	protected Function m_fChangedFunction;
	protected int[] m_aiParameters;
	protected double m_dInitialValue;
	protected double m_dTargetValue;
	protected double m_dChangeStep;
	protected int m_iChangeDirection;
	protected boolean m_bNormalize; 
	protected static int m_cChanges = 0;
	protected int m_iID;
	protected String m_sType;
	protected int m_cSteps;
	
	public FunctionChange( Function fChangeFunction, int[] aiParameters, 
				double dTargetValue, int cSteps, boolean bNormalize, 
				String sType ){
		m_fChangedFunction = fChangeFunction;
		m_aiParameters = aiParameters;
		m_dTargetValue = dTargetValue;
		m_dInitialValue = m_fChangedFunction.valueAt( m_aiParameters );
		if( m_dTargetValue > m_dInitialValue ){
			m_iChangeDirection = 1;
		}
		else{
			m_iChangeDirection = -1;
		}
		m_bNormalize = bNormalize;
		m_iID = m_cChanges++;
		m_sType = sType;
		m_cSteps = cSteps;
	}
	
	public void init(){
		m_dInitialValue = m_fChangedFunction.valueAt( m_aiParameters );
		if( m_dTargetValue > m_dInitialValue ){
			m_iChangeDirection = 1;
		}
		else{
			m_iChangeDirection = -1;
		}
		m_dChangeStep = Math.abs( m_dTargetValue - m_dInitialValue ) / m_cSteps;
	}
	
	public boolean verify(){
		Iterator itNonZero = m_fChangedFunction.getNonZeroEntries( m_aiParameters[0], m_aiParameters[1] );
		Map.Entry e = null;
		int iArg3 = 0, cEntries = m_fChangedFunction.countNonZeroEntries( m_aiParameters[0], m_aiParameters[1] );
		double dEntryValue = 0.0, dSum = 0.0;
		
		while( itNonZero.hasNext() ){
			e = (Entry) itNonZero.next();
			iArg3 = ((Integer)e.getKey()).intValue();
			dEntryValue = ((Double)e.getValue()).doubleValue();
			if( dEntryValue > 1.001 || dEntryValue < -0.001 ){
				Logger.getInstance().logln( "verify: BUGBUG - prob out of range" );
				return false;
			}
			dSum += dEntryValue;
		}			
		
		if( dSum < 0.99 || dSum > 1.01 ){
			Logger.getInstance().logln( "verify: BUGBUG - probs don't sum up to 1" );
			return false;
		}
		return true;
	}
	
	public boolean done(){
		double dCurrentValue = m_fChangedFunction.valueAt( m_aiParameters );
		return ( Math.abs( m_dTargetValue - dCurrentValue ) < m_dChangeStep );
	}
	
	public double executeStep(){
		if( done() )
			return 1.0;
		double dCurrentValue = m_fChangedFunction.valueAt( m_aiParameters );
		boolean bDone = false;
		if( Math.abs( m_dTargetValue - dCurrentValue ) < m_dChangeStep ){
			dCurrentValue = m_dTargetValue;
			bDone = true;
		}
		else
			dCurrentValue += m_dChangeStep * m_iChangeDirection;
		m_fChangedFunction.setValue( m_aiParameters, dCurrentValue );
		if( m_bNormalize ){
			Iterator itNonZero = m_fChangedFunction.getNonZeroEntries( m_aiParameters[0], m_aiParameters[1] );
			Map.Entry e = null;
			int iArg3 = 0, cEntries = m_fChangedFunction.countNonZeroEntries( m_aiParameters[0], m_aiParameters[1] );
			double dChange = m_dChangeStep / ( cEntries - 1 ), dEntryValue = 0.0;
			double dSum = dCurrentValue;
			
			while( itNonZero.hasNext() ){
				e = (Entry) itNonZero.next();
				iArg3 = ((Integer)e.getKey()).intValue();
				if( iArg3 != m_aiParameters[2] ){
					dEntryValue = ((Double)e.getValue()).doubleValue();
					dEntryValue -= m_iChangeDirection * dChange;
					if( dEntryValue < 0.0001 )
						dEntryValue = 0.0001;
					else if( dEntryValue > 1.0001 )
						dEntryValue = 1.0;
					dSum += dEntryValue;
					m_fChangedFunction.setValue( m_aiParameters[0], m_aiParameters[1], iArg3, dEntryValue );  
				}
			}			
			m_fChangedFunction.setValue( m_aiParameters, dCurrentValue + 1.0 - dSum );
		}
		if( bDone )
			return 1.0;
		return ( m_dTargetValue - dCurrentValue ) / ( m_dInitialValue - dCurrentValue );
	}

	public String getXMLString( int iStartStep ){
		String sInfo = "", sParameter = "";
		sInfo = "<FunctionChange type = \"" + m_sType + 
			"\" start = \"" + iStartStep + 
			"\" length = \"" + m_cSteps + 
			//"\" init = \"" + m_dInitialValue + //can't compute when executing offline
			"\" target = \"" + m_dTargetValue + 
			//"\" step = \"" + m_dChangeStep + //can't compute when executing offline
				"\">";
		int iParameter = 0;
		sInfo += "<Parameters>";
		for( iParameter = 0 ; iParameter < m_aiParameters.length ; iParameter++ ){
			sParameter = "<Parameter value = \"" + m_aiParameters[iParameter] + "\"/>";
			sInfo += sParameter;
		}
		sInfo += "</Parameters>";
		sInfo += "</FunctionChange>";
		return sInfo;
	}
}
