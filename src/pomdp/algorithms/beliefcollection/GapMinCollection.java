package pomdp.algorithms.beliefcollection;

import java.util.PriorityQueue;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;
import pomdp.valuefunction.JigSawValueFunction;

public class GapMinCollection extends BeliefCollection {

	protected JigSawValueFunction m_vfUpperBound;
	protected double m_dMaxWidthForIteration;

	Vector<BeliefState> exploredBeliefs;
	Vector<BeliefState> previousBeliefs;
	//parameters
	int defaultNumBeliefPoints = 200;
	//int count;

	public GapMinCollection(ValueIteration vi, boolean bAllowDuplicates, boolean bUseFIB)
	{	
		super(vi, bAllowDuplicates);	
		POMDP.resetMDPValueFunction();
		POMDP.getMDPValueFunction().persistQValues( true );
		POMDP.getMDPValueFunction().valueIteration( 1000, ExecutionProperties.getEpsilon());

		m_vfUpperBound = new JigSawValueFunction(POMDP, POMDP.getMDPValueFunction(), bUseFIB);
	}

	/* no initial belief */
	public Vector<BeliefState> initialBelief()
	{

		return new Vector<BeliefState>();
	}

	private class PriorityQueueElement implements Comparable<PriorityQueueElement>{

		public BeliefState m_bsBelief;
		public double m_dScore, m_dProb;
		public int m_iDepth;
		public String m_sHistory;

		public PriorityQueueElement(BeliefState bs, double dScore, double dProb, int iDepth, String sHistory){
			m_bsBelief = bs;
			m_dScore = dScore;
			m_dProb = dProb;
			m_iDepth = iDepth;
			m_sHistory = sHistory;
		}

		@Override
		public int compareTo(PriorityQueueElement eOther) {
			if(m_dScore > eOther.m_dScore)
				return 1;
			if(m_dScore < eOther.m_dScore)
				return -1;
			return 0;
		}

	}

	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		previousBeliefs = beliefPoints;
		exploredBeliefs = new Vector<BeliefState>();

		PriorityQueue<PriorityQueueElement> pq = new PriorityQueue<PriorityQueueElement>();

		int cUpperBoundPoints = 0;

		BeliefState bsInitial = POMDP.getBeliefStateFactory().getInitialBeliefState();
		double dInitialWidth = width(bsInitial);		
		Logger.getInstance().logln("initial width = " + dInitialWidth);


		int cPreviousBeliefs = 0;
		//do{
			m_dMaxWidthForIteration = 0.0;
			pq.add(new PriorityQueueElement(bsInitial, dInitialWidth, 1, 0, ""));
			explore(pq, epsilon, numNewBeliefs);
			//if(exploredBeliefs.size() == cPreviousBeliefs)
			//	break;
			//cPreviousBeliefs = exploredBeliefs.size();
		//}while( exploredBeliefs.size() < numNewBeliefs);
		

		if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
			m_vfUpperBound.pruneUpperBound();
			cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
		}			

		dInitialWidth = width(bsInitial);

		return exploredBeliefs;		
	}

	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints)
	{
		return expand(defaultNumBeliefPoints, beliefPoints);
	}


	protected void explore(PriorityQueue<PriorityQueueElement> pq, double dEpsilon, int cMaxBeliefs){
		int cInitialPoints = exploredBeliefs.size();
		double dDiscountFactor = valueIteration.getPOMDP().getDiscountFactor();
		Logger.getInstance().logln("Started exploring for " + cMaxBeliefs + " points.");
		int cChecked = 0, cAdded = 0, cExpanded = 0;
		long lStartTime = System.currentTimeMillis();
		while(!pq.isEmpty() && exploredBeliefs.size() < cMaxBeliefs){
			PriorityQueueElement e = pq.poll();
			BeliefState bsCurrent = e.m_bsBelief;
			Pair<Integer,Double> pBestAction = new Pair<Integer,Double>();
			int iBestAction = m_vfUpperBound.getAction2(bsCurrent, pBestAction);
			double dCurrentUB = m_vfUpperBound.valueAt(bsCurrent);
			double dBestUB = pBestAction.second();
			if(dCurrentUB - dBestUB > dEpsilon){//consider removing this
				m_vfUpperBound.setValueAt(bsCurrent, dBestUB);
			}

			double dBestLB = getLowerBoundUpdateValue(bsCurrent, iBestAction);
			double dCurrentLB = valueIteration.valueAt(bsCurrent);
			if(dBestLB - dCurrentLB > 0){
				if( m_bAllowDuplicates || !exploredBeliefs.contains(bsCurrent)){
					exploredBeliefs.add(bsCurrent);
					cAdded++;
				}
			}
			if(cChecked % 10 == 0)
				Logger.getInstance().log(".");
			
			//do not expand children if this is a terminal belief
			if (POMDP.terminalStatesDefined() && bsCurrent.isDeterministic()){
				int iState = bsCurrent.getDeterministicIndex();
				if(POMDP.isTerminalState(iState))
					continue;
			}

			int iObservation = 0;
			BeliefState bsNext = null;
			double dProb = 0.0;
			double dWidth = 0.0, dScore = 0.0;
			double dCurrentDiscount = Math.pow(dDiscountFactor, e.m_iDepth + 1);
			for( iObservation = 0 ; iObservation < valueIteration.getPOMDP().getObservationCount() ; iObservation++ ){
				dProb = bsCurrent.probabilityOGivenA(iBestAction, iObservation);
				if(dProb > 0)
				{
					cExpanded++;
					bsNext = bsCurrent.nextBeliefState(iBestAction, iObservation);
					dWidth = width(bsNext);
					if(dWidth * dCurrentDiscount > 0.1){
						dScore = dProb * e.m_dProb * dCurrentDiscount * dWidth;
						pq.add(new PriorityQueueElement(bsNext, dScore, dProb * e.m_dProb, e.m_iDepth + 1, e.m_sHistory + ", <" + iBestAction + "," + iObservation + ">"));
					}
				}
			}
			cChecked++;
			long lCurrentTime = System.currentTimeMillis();
			if((lCurrentTime - lStartTime) / 1000 > 50)
				break;
		}
		
		Logger.getInstance().logln("\nDone expansion: " + cAdded + ", " + cChecked + ", " + cExpanded);	
	}

	private double getLowerBoundUpdateValue(BeliefState bs, int iAction) {
		double dSum = 0.0, dProb = 0.0;
		int iObservation = 0;
		BeliefState bsNext = null;
		double dValue = 0.0;
		for( iObservation = 0 ; iObservation < valueIteration.getPOMDP().getObservationCount() ; iObservation++ ){
			dProb = bs.probabilityOGivenA(iAction, iObservation);
			if(dProb > 0)
			{
				bsNext = bs.nextBeliefState(iAction, iObservation);
				dValue = valueIteration.valueAt(bsNext);
				dSum += dProb * dValue;
			}
		}
		dSum = valueIteration.getPOMDP().R(bs,iAction) + dSum * valueIteration.getPOMDP().getDiscountFactor();
		return dSum;
	}


	protected double width( BeliefState bsCurrent ){
		double dUpperValue = 0.0, dLowerValue = 0.0, dWidth = 0.0;

		dUpperValue = m_vfUpperBound.valueAt( bsCurrent );
		dLowerValue = valueIteration.valueAt( bsCurrent );
		dWidth = dUpperValue - dLowerValue;	

		return dWidth;
	}



}
