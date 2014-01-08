package pomdp.environments;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.algorithms.PolicyStrategy;
import pomdp.utilities.FunctionChange;
import pomdp.utilities.datastructures.Function;

public class SlowlyChangingPOMDP extends POMDP {
	
	public final static int TRANSITION_CHANGES = 1;
	public final static int OBSERVATION_CHANGES = 2;
	public final static int REWARD_CHANGES = 4;
	
	protected int m_iChangesMode;
	protected int m_cSteps;
	protected Map m_mChanges;
	protected Vector m_vOpenChanges;
	
	public SlowlyChangingPOMDP(){
		super();
		m_iChangesMode = TRANSITION_CHANGES | OBSERVATION_CHANGES;
		m_cSteps = 0;
		m_vOpenChanges = new Vector();
		m_mChanges = null;
	}
	
	public void setChangesMode( int iMode ){
		m_iChangesMode = iMode;
	}
	
	protected FunctionChange createNewChange(){
		int cTypes = 0, iType = 0;
		boolean bTransitionChanges = ( m_iChangesMode & TRANSITION_CHANGES ) == TRANSITION_CHANGES;
		boolean bObservationChanges = ( m_iChangesMode & OBSERVATION_CHANGES ) == OBSERVATION_CHANGES;
		boolean bRewardChanges = ( m_iChangesMode & REWARD_CHANGES ) == REWARD_CHANGES;
		int iStartState = 0, iAction = 0, iEndState = 0, iObservation = 0, cEntries = 0, iEntry = 0;
		Iterator itNonZero = null;
		Map.Entry e = null;
		double dNewValue = 0.0, dValue = 0.0;
		int[] aiParameters = null;
		FunctionChange fcNewChange = null;
		
		if( bTransitionChanges )
			cTypes++;
		if( bObservationChanges )
			cTypes++;
		if( bRewardChanges )
			cTypes++;
		
		iType = m_rndGenerator.nextInt( cTypes );
		if( bTransitionChanges ){
			if( iType == 0 ){
				iStartState = m_rndGenerator.nextInt( m_cStates );
				iAction = m_rndGenerator.nextInt( m_cActions );
				itNonZero = m_fTransition.getNonZeroEntries( iStartState, iAction );
				cEntries = m_fTransition.countNonZeroEntries( iStartState, iAction );
				iEntry = m_rndGenerator.nextInt( cEntries );
				if( cEntries == 1 ) //no place to shift the probs to
					return null;
				while( iEntry >= 0 ){
					e = (Entry) itNonZero.next();
					iEndState = ((Integer) e.getKey()).intValue();
					dValue = ((Double) e.getValue()).doubleValue();
					iEntry--;
				}
				aiParameters = new int[3];
				aiParameters[0] = iStartState;
				aiParameters[1] = iAction;
				aiParameters[2] = iEndState;
				dValue = m_fTransition.valueAt( aiParameters );
				dNewValue = m_rndGenerator.nextDouble( 0.1, 0.9 );
				fcNewChange = new FunctionChange( m_fTransition, aiParameters, 
						dNewValue, 50, true, "Transition" );
			}
			else{
				iType--;
			}
		}
		else{
			iType--;
		}
		if( ( fcNewChange == null ) && bObservationChanges ){
			if( iType <= 0 ){
				iAction = m_rndGenerator.nextInt( m_cActions );
				iEndState = m_rndGenerator.nextInt( m_cStates );
				itNonZero = m_fObservation.getNonZeroEntries( iAction, iEndState );
				cEntries = m_fObservation.countNonZeroEntries( iAction, iEndState );
				iEntry = m_rndGenerator.nextInt( cEntries );
				if( cEntries == 1 ) //no place to shift the probs to
					return null;
				while( iEntry >= 0 ){
					e = (Entry) itNonZero.next();
					iObservation = ((Integer) e.getKey()).intValue();
					iEntry--;
				}
				aiParameters = new int[3];
				aiParameters[0] = iAction;
				aiParameters[1] = iEndState;
				aiParameters[2] = iObservation;
				dValue = m_fObservation.valueAt( aiParameters );
				dNewValue = m_rndGenerator.nextDouble();
				fcNewChange =  new FunctionChange( m_fObservation, aiParameters, 
						dNewValue, 50, true, "Observation" );
			}
		}
		else{
			iType--;
		}
		if( ( fcNewChange == null ) && bRewardChanges ){
			iStartState = m_rndGenerator.nextInt( m_cStates );
			iAction = m_rndGenerator.nextInt( m_cActions );
			aiParameters = new int[2];
			aiParameters[0] = iStartState;
			aiParameters[1] = iAction;
			dValue = R( iStartState, iAction );
			dNewValue = dValue + ( m_rndGenerator.nextDouble( 2 ) - 1.0 );
			fcNewChange = new FunctionChange( m_fReward, aiParameters, 
					dNewValue, 50, true, "Reward" );
		}
		return fcNewChange;
	}
		
	public int execute( int iAction, int iState ){
		FunctionChange fc = null;
		Iterator itChanges = null;
		double dChangeCompletePhase = 0.0;
		
		if( m_bExploration ){
			if( m_mChanges != null ){
				fc = (FunctionChange) m_mChanges.get( new Integer( m_cSteps ) );
				if( fc != null ){
					fc.init();
					m_vOpenChanges.add( fc );
				}
			}
			itChanges = m_vOpenChanges.iterator();
			while( itChanges.hasNext() ){
				fc = (FunctionChange) itChanges.next();
				dChangeCompletePhase = fc.executeStep();
				if( dChangeCompletePhase == 1.0 )
					itChanges.remove();
			}
			m_cSteps++;
		}
		return super.execute( iAction, iState );
	}
	
	protected void setEnvironmentType( PolicyStrategy policy ){
		policy.setEnvironmentType( false );
	}
	
	public void loadChanges( String sFileName ) throws IOException, Exception{
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document docChanges = builder.parse( new FileInputStream( sFileName ) );
		Element eChange = null, eParameter = null;
		int[] aiParameters = null;
		double dTargetValue = 0.0;
		int iValue = 0;
		NodeList nlChanges = null, nlParameters = null;
		int iChange = 0, iParameter = 0, iStartStep = 0, cSteps = 0;
		String sType = "";
		FunctionChange fc = null;
		Function f = null;
		boolean bNormalize = false;
		
		m_mChanges = new TreeMap();
		
		nlChanges = docChanges.getChildNodes().item( 0 ).getChildNodes();
		for( iChange = 0 ; iChange < nlChanges.getLength() ; iChange++ ){
			eChange = (Element)nlChanges.item( iChange );
			iStartStep = Integer.parseInt( eChange.getAttribute( "start" ) );
			cSteps = Integer.parseInt( eChange.getAttribute( "length" ) );
			dTargetValue = Double.parseDouble( eChange.getAttribute( "target" ) );
			sType = eChange.getAttribute( "type" );
			if( sType.equals( "Transition" ) ){
				f = m_fTransition;
				bNormalize = true;
			}
			else if( sType.equals( "Observation" ) ){
				f = m_fObservation;
				bNormalize = true;
			}
			else if( sType.equals( "Reward" ) ){
				f = m_fReward;
				bNormalize = true;
			}
			nlParameters = eChange.getChildNodes().item( 0 ).getChildNodes();
			aiParameters = new int[nlParameters.getLength()];
			for( iParameter = 0 ; iParameter < nlParameters.getLength() ; iParameter++ ){
				eParameter = (Element) nlParameters.item( iParameter );
				iValue = Integer.parseInt( eParameter.getAttribute( "value" ) );
				aiParameters[iParameter] = iValue;
			}
			fc = new FunctionChange( f, aiParameters, dTargetValue, cSteps, bNormalize, sType );
			m_mChanges.put( new Integer( iStartStep ), fc );
		}		
	}
	
	public void createChangesFile( String sFileName, int cSteps, int iChangeSteps ) throws IOException{
		int iStep = 0;
		FunctionChange fc = null;
		String sInfo = "";
		FileOutputStream fosOutputChanges = new FileOutputStream( sFileName );
		sInfo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		fosOutputChanges.write( sInfo.getBytes() );
		sInfo = "<FunctionChanges>";
		fosOutputChanges.write( sInfo.getBytes() );
		
		for( iStep = 0 ; iStep < cSteps ; iStep++ ){
			if( m_rndGenerator.nextInt( iChangeSteps ) == 0 ){
				fc = createNewChange(); 
				if( fc != null ){
					sInfo = fc.getXMLString( iStep );
					fosOutputChanges.write( sInfo.getBytes() );
				}
			}
		}
		
		sInfo = "</FunctionChanges>";
		fosOutputChanges.write( sInfo.getBytes() );
	}
}
