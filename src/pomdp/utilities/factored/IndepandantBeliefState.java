package pomdp.utilities.factored;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.environments.FactoredPOMDP;
import pomdp.environments.FactoredPOMDP.BeliefType;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.JProf;
import pomdp.utilities.factored.AlgebraicDecisionDiagram.AbstractionFilter;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class IndepandantBeliefState extends BeliefState {

	private int m_cStateVariables;
	private int m_cComponents;
	private AlgebraicDecisionDiagram[] m_addComponents;
	private AlgebraicDecisionDiagram m_addProduct;
	private FactoredPOMDP m_pPOMDP;
	private int[][] m_aiComponentVariables;
	private static boolean m_bUseProduct = false;
	private static boolean m_bComponentUpdate = false;
	
	public IndepandantBeliefState( FactoredPOMDP pPOMDP, int id ){
		super( pPOMDP.getStateCount(), pPOMDP.getActionCount(), pPOMDP.getObservationCount(), id, pPOMDP.getBeliefStateFactory().isCachingBeliefStates(), pPOMDP.getBeliefStateFactory() );
		m_pPOMDP = pPOMDP;
		m_cStateVariables = m_pPOMDP.getStateVariablesCount();
		m_cComponents = m_pPOMDP.getIndependentComponentsCount();
		m_addComponents = new AlgebraicDecisionDiagram[m_cComponents];
		m_aiComponentVariables = new int[m_cComponents][];
		m_addProduct = null;
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			m_aiComponentVariables[iComponent] = m_pPOMDP.getIndependentComponentVariables( iComponent );
		}
	}
	
	public double innerProduct( FactoredAlphaVector av ){
		if( m_bUseProduct )
			return m_addProduct.innerProduct( av.m_addValues );
		else{
			//return av.m_addValues.innerProduct( m_addComponents, m_aiComponentVariables );
			//throw new NotImplementedException();
			double dSum = 0.0;
			int iComponent = 0;
			AlgebraicDecisionDiagram addRestricted = null;
			for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
				addRestricted = av.m_addValues.existentialAbstraction( new ComponentFilter( m_aiComponentVariables[iComponent] ) );
				dSum += addRestricted.product( m_addComponents[iComponent] ).getValueSum();
			}
			return dSum;
		}
	}

	protected class ComponentFilter implements AbstractionFilter{

		private int[] m_aiVariables;
		
		public ComponentFilter( int[] aiVariables ){
			m_aiVariables = aiVariables;
		}
		
		private boolean contains( int iVariable ){
			for( int i : m_aiVariables ){
				if( i == iVariable )
					return true;
			}
			return false;
		}
		
		@Override
		public boolean abstractVariable(int variable) {
			return contains( variable );
		}

		@Override
		public int countAbstractionVariablesBetween(int var1, int var2) {
			int i = 0;
			int c = 0;
			for( i = var1 + 1 ; i < var2 ; i++ ){
				if( contains( i ) )
					c++;
			}
			return c;
		}

		@Override
		public int countVariablesBetween(int var1, int var2) {
			return var2 - var1 - 1;
		}

		@Override
		public int firstVariableAfter(int variable) {
			int i = 0;
			for( i = variable + 1 ; i < m_cStateVariables ; i++ ){
				if( contains( i ) )
					return i;
			}
			return -1;
		}

		@Override
		public int getFirstVariableId() {
			return 0;
		}

		@Override
		public int getLastVariableId() {
			return m_cStateVariables - 1;
		}

		@Override
		public int lastVariable() {
			return m_cStateVariables - 1;
		}

		@Override
		public boolean sumMissingLevels() {
			return true;
		}
	}
	
	public double valueAt( int iState ){
		int[] aiComponentVariables = null;
		boolean[] abValues = null;
		double dProb = 0.0, dTotalProb = 1.0;
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			aiComponentVariables = m_pPOMDP.getIndependentComponentVariables( iComponent );
			abValues = toBits( iState, aiComponentVariables );
			dProb = m_addComponents[iComponent].valueAt( aiComponentVariables, abValues );
			dTotalProb *= dProb;
		}
		return dTotalProb;
	}

	public double valueAt( boolean[] abValues ){
		int[] aiComponent = null;
		boolean[] abComponentValues = null;
		int iComponent = 0, iVariable = 0, cComponentVariables = 0;
		double dProb = 0.0, dTotalProb = 1.0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			aiComponent = m_pPOMDP.getIndependentComponentVariables( iComponent );
			cComponentVariables = aiComponent.length;
			abComponentValues = new boolean[cComponentVariables];
			while( iVariable < aiComponent[cComponentVariables - 1] ){
				abComponentValues[iVariable - aiComponent[0]] = abValues[iVariable];
				iVariable++;
			}
			dProb = m_addComponents[iComponent].valueAt( aiComponent, abComponentValues );
			dTotalProb *= dProb;
		}
		return dTotalProb;
	}

	private int find( int i, int[] a ){
		for( int j = 0 ; j < a.length ; j++ ){
			if( a[j] == i )
				return j;
		}
		return -1;
	}

	public double valueAt( int[] aiVariables, boolean[] abValues ){
		int[] aiComponent = null;
		boolean[] abComponentValues = null;
		int iComponent = 0, iVariable = 0, cComponentVariables = 0;
		double dProb = 0.0, dTotalProb = 1.0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			aiComponent = m_pPOMDP.getIndependentComponentVariables( iComponent );
			iVariable = find( aiComponent[0], aiVariables );
			if( iVariable != -1 ){
				cComponentVariables = aiComponent.length;
				abComponentValues = new boolean[cComponentVariables];
				while( ( iVariable < aiVariables.length ) && ( aiVariables[iVariable] <= aiComponent[cComponentVariables - 1] ) ){
					abComponentValues[aiVariables[iVariable] - aiComponent[0]] = abValues[iVariable];
					iVariable++;
				}
				dProb = m_addComponents[iComponent].valueAt( aiComponent, abComponentValues );
				dTotalProb *= dProb;
			}
		}
		return dTotalProb;
	}


	public void setValueAt( int iState, double dValue ){
		throw new NotImplementedException();
	}
	
	public AlgebraicDecisionDiagram[] getIndependentComponentProbabilities(){
		return m_addComponents;
	}
	
	public String toString_2(){
		int i = 0;
		String sResult = "[";
		for( i = 0 ; i < m_cComponents ; i++ ){
			sResult += i + "=" + m_addComponents[i] + ",";
		}
		sResult += "]";
		return sResult;	
	}
	
	
	//why do I need reversed order?
	private void toBits( int iValue, boolean[] abValues ){
		int i = 0;
		for( i = 0 ; i < abValues.length  ; i++ ){
		//for( i = abValues.length - 1 ; i >= 0 ; i-- ){
			if( iValue % 2 == 1 )
				abValues[i] = true;
			else
				abValues[i] = false;
			iValue /= 2;
		}
	}
	
	private boolean[] toBits( int iValue, int[] aiVariables ){
		int i = 0;
		boolean[] abValues = new boolean[aiVariables.length];
		iValue = iValue >> aiVariables[0];
		//for( i = abValues.length - 1 ; i >= 0 ; i-- ){
		for( i = 0 ; i < aiVariables.length  ; i++ ){
			if( iValue % 2 == 1 )
				abValues[i] = true;
			else
				abValues[i] = false;
			iValue /= 2;
		}
		return abValues;
	}

	public void setComponentProbability( int iComponent, double[] adValues ){
		int iValue = 0, cValues = adValues.length;
		int[] aiVariables = m_pPOMDP.getIndependentComponentVariables( iComponent );
		boolean[] abValues = new boolean[aiVariables.length];
		m_addComponents[iComponent] = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		for( iValue = 0 ; iValue < cValues ; iValue++ ){
			if( adValues[iValue] != 0.0 ){
				toBits( iValue, abValues );
				m_addComponents[iComponent].addPartialPath( aiVariables, abValues, adValues[iValue], false );
			}
		}
		m_addComponents[iComponent].finalizePaths( 0.0 );
		m_addComponents[iComponent].reduce();
	}

	public void setComponentProbability( int iComponent, AlgebraicDecisionDiagram addProbs ){
		m_addComponents[iComponent] = addProbs.copy();
	}
	
	public void setUniformProbability( int iComponent ){
		int cVariables = m_pPOMDP.getIndependentComponentVariables(iComponent).length;
		double dProb = 1.0 / Math.pow( 2.0, cVariables );
		m_addComponents[iComponent] = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
		m_addComponents[iComponent].finalizePaths( dProb );
		m_addComponents[iComponent].reduce();
	}

	public void setDeterministicState( int iState ){
		int[] aiComponentVariables = null;
		boolean[] abValues = null;
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			aiComponentVariables = m_pPOMDP.getIndependentComponentVariables( iComponent );
			abValues = toBits( iState, aiComponentVariables );
			m_addComponents[iComponent] = m_pPOMDP.newAlgebraicDecisionDiagram( m_cStateVariables, true );
			m_addComponents[iComponent].addPartialPath( aiComponentVariables, abValues, 1.0, false );
			m_addComponents[iComponent].finalizePaths( 0.0 );
			m_addComponents[iComponent].reduce();
		}
	}
	
	protected AlgebraicDecisionDiagram computeNextBeliefProbability( int iComponent, int iAction, int iObservation ){
		int[] aiRelevantComponenets = m_pPOMDP.getRelevantComponentsForComponent( iAction, iComponent );
		int iRelevantComponent = 0, iRelevantComponentIdx = 0;
		AlgebraicDecisionDiagram addNew = null;
		AlgebraicDecisionDiagram addOb = m_pPOMDP.getObservationDiagram( iAction, iObservation );
		AlgebraicDecisionDiagram addComponent = null;
		
		addNew = m_pPOMDP.getIndependentComponentTransitionDiagram( iAction, iRelevantComponent );
		for( iRelevantComponent = 0 ; iRelevantComponent < m_cComponents ; iRelevantComponent++ ){
		//for( iRelevantComponentIdx = 0 ; iRelevantComponentIdx < aiRelevantComponenets.length ; iRelevantComponentIdx++ ){
			//iRelevantComponent = aiRelevantComponenets[iRelevantComponentIdx];
			addComponent = m_addComponents[iRelevantComponent];
			addComponent.translateVariables( m_pPOMDP.getPostActionVariableExpander() );
			addNew = addNew.product( addComponent );
			addComponent.translateVariables( m_pPOMDP.getVariableReducer() );
		}
		addOb.translateVariables( m_pPOMDP.getPreActionVariableExpander() );
		addNew = addNew.product( addOb );
		addOb.translateVariables( m_pPOMDP.getVariableReducer() );
		
		addNew = m_pPOMDP.existentialAbstraction( addNew, iComponent, true );
		addNew.product( 1 / addNew.getValueSum() );
		

		return addNew;
	}

	protected AlgebraicDecisionDiagram computeNextBeliefProbability( int iAction, int iObservation ){
		int[] aiRelevantComponenets = m_pPOMDP.getRelevantComponents( iAction, iObservation );
		int iRelevantComponent = 0, iRelevantComponentIdx = 0;
		AlgebraicDecisionDiagram addNew = null;
		AlgebraicDecisionDiagram addTr = null;
		AlgebraicDecisionDiagram addOb = m_pPOMDP.getObservationDiagram( iAction, iObservation );
		AlgebraicDecisionDiagram addComponentBefore = null;
		AlgebraicDecisionDiagram addComponentAfter = null;
		
		addOb.translateVariables( m_pPOMDP.getPreActionVariableExpander() );
		addNew = addOb.copy();
		addOb.translateVariables( m_pPOMDP.getVariableReducer() );
		if( m_bUseProduct ){
			m_addProduct.translateVariables( m_pPOMDP.getPostActionVariableExpander() );
			addNew = addNew.product( m_addProduct );
			m_addProduct.translateVariables( m_pPOMDP.getVariableReducer() );
		}
		//for( iRelevantComponentIdx = 0 ; iRelevantComponentIdx < aiRelevantComponenets.length ; iRelevantComponentIdx++ ){
			//iRelevantComponent = aiRelevantComponenets[iRelevantComponentIdx];
		for( iRelevantComponent = 0 ; iRelevantComponent < m_cComponents ; iRelevantComponent++ ){
			addComponentBefore = m_addComponents[iRelevantComponent];
			addTr = m_pPOMDP.getIndependentComponentTransitionDiagram( iAction, iRelevantComponent );
			if( m_bUseProduct ){
				addNew = addNew.product( addTr );
			}
			else{
				addComponentBefore.translateVariables( m_pPOMDP.getPostActionVariableExpander() );
				addComponentAfter = addTr.product( addComponentBefore );
				addComponentBefore.translateVariables( m_pPOMDP.getVariableReducer() );
				addNew = addNew.product( addComponentAfter );
			}
		}
		
		return addNew;
	}

//	protected double computeNextBeliefProbability( int[] aiComponent, int iAction, int iObservation, double[] adProbs ){
//		int[] aiRelevantVariables = null;
//		boolean[] abRelevantValues = null, abComponentValues = null;
//		int iRelevantValue = 0, cRelevantValues = 0;
//		int iComponentValue = 0, cComponentValues = 0;
//		double dPrior = 0.0, dSumProbs = 0.0,dOb = 0.0, dTr = 0.0;
//		
//		aiRelevantVariables = m_pPOMDP.getRelevantVariables( iAction );
//		abRelevantValues = new boolean[aiRelevantVariables.length];
//		abComponentValues = new boolean[aiComponent.length];
//		cRelevantValues = (int) Math.pow( 2, aiRelevantVariables.length );
//		cComponentValues = (int) Math.pow( 2, aiComponent.length );
//
//		if( adProbs != null ){
//			for( iComponentValue = 0 ; iComponentValue < cComponentValues ; iComponentValue++ ){
//				adProbs[iComponentValue] = 0;
//			}
//		}
//		
//		for( iRelevantValue = 0 ; iRelevantValue < cRelevantValues ; iRelevantValue++ ){
//			toBits( iRelevantValue, abRelevantValues );
//			dPrior = valueAt( aiRelevantVariables, abRelevantValues );
//			if( dPrior != 0.0 ){
//				for( iComponentValue = 0 ; iComponentValue < cComponentValues ; iComponentValue++ ){
//					toBits( iComponentValue, abComponentValues );
//					dTr = m_pPOMDP.transitionGivenRelevantVariables( iAction, aiComponent, abComponentValues, aiRelevantVariables, abRelevantValues );
//					if( dTr != 0.0 ){
//						dOb = m_pPOMDP.observationGivenRelevantVariables( iAction, iObservation, aiComponent, abComponentValues );
//						if( adProbs != null )
//							adProbs[iComponentValue] += dPrior * dTr * dOb;
//						dSumProbs += dPrior * dTr * dOb;
//					}
//				}
//			}
//		}
//
//		if( adProbs != null ){
//			for( iComponentValue = 0 ; iComponentValue < cComponentValues ; iComponentValue++ ){
//				adProbs[iComponentValue] /= dSumProbs;
//			}
//		}
//		
//		return dSumProbs;	
//	}


	public BeliefState nextBeliefState( int iAction, int iObservation ){	
		long lTimeBefore = 0, lTimeAfter = 0;
		if( ExecutionProperties.getReportOperationTime() ){
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
		}
		IndepandantBeliefState ibsNew = new IndepandantBeliefState( m_pPOMDP, -1 );
		AlgebraicDecisionDiagram addTotal = null, addComponent = null, addProduct = null;
		double dSumProbs = 0.0;
		int iComponent = 0;
		
		if( !m_bComponentUpdate ){
		
			if( m_bUseProduct && ( m_addProduct == null ) ){
				computeComponentProduct();
			}
			
			addTotal = computeNextBeliefProbability( iAction, iObservation );
			
			dSumProbs = addTotal.getValueSum();
			addTotal.product( 1.0 / dSumProbs );
			
			for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
				addComponent = m_pPOMDP.existentialAbstraction( addTotal, iComponent, true );
				if( m_bUseProduct ){
					if( addProduct == null )
						addProduct = addComponent;
					else
						addProduct = addProduct.product( addComponent );
				}
				ibsNew.setComponentProbability( iComponent, addComponent );
			}
			if( m_bUseProduct ){
				ibsNew.m_addProduct = addProduct;
			}
		}
		else{
			for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
				addComponent = computeNextBeliefProbability( iComponent, iAction, iObservation );
				ibsNew.setComponentProbability( iComponent, addComponent );
			}
		}
			
		if( ExecutionProperties.getReportOperationTime() ){
			lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			m_pPOMDP.getBeliefStateFactory().m_cTimeInTau += ( lTimeAfter - lTimeBefore ) / 1000;
			m_pPOMDP.getBeliefStateFactory().m_cBeliefUpdates++;
			/*
			if( m_pPOMDP.getBeliefStateFactory().m_cBeliefUpdates % 1000 == 0 ){
				Logger.getInstance().logln( "After " + m_pPOMDP.getBeliefStateFactory().m_cBeliefUpdates + " next BS computations, avg time " + (int)m_pPOMDP.getBeliefStateFactory().getAvgTauTime() );
			}
			*/
			m_pPOMDP.getBeliefStateFactory().m_cBeliefStateSize += size();
		}
		return ibsNew;
	}
	
	private void computeComponentProduct() {
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			if( m_addProduct == null )
				m_addProduct = m_addComponents[iComponent];
			else
				m_addProduct = m_addProduct.product( m_addComponents[iComponent] );
		}
	}

	/**(non-Javadoc)
	 * @see pomdp.utilities.BeliefState#probabilityOGivenA(int, int)
	 */
	public double probabilityOGivenA( int iAction, int iObservation ){	
		AlgebraicDecisionDiagram addNext = null;
		if( m_bUseProduct && ( m_addProduct == null ) ){
			computeComponentProduct();
		}
		addNext = computeNextBeliefProbability( iAction, iObservation );
		double dTotalProb = addNext.getValueSum();
		return dTotalProb;
	}
	
	public Collection<Entry<Integer,Double>> getNonZeroEntries(){
		return null;
	}
	
	public long size(){
		long iSize = 0;
		for( AlgebraicDecisionDiagram add : m_addComponents ){
			iSize += add.getVertexCount();
		}
		return iSize;
	}

	public boolean equals( IndepandantBeliefState ibs ){
		int iComponent = 0;
		for( iComponent = 0 ; iComponent < m_cComponents ; iComponent++ ){
			if( !m_addComponents[iComponent].equals( ibs.m_addComponents[iComponent] ) )
				return false;
		}
		return true;
	}
	
	public boolean equals( Object oOther ){
		if( oOther == this )
			return true;
		if( oOther instanceof IndepandantBeliefState ){
			IndepandantBeliefState ibs = (IndepandantBeliefState)oOther;
			return equals( ibs );
		}
		return false;
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
	public int getNonZeroEntriesCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}
}
