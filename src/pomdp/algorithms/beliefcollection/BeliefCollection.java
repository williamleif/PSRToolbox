package pomdp.algorithms.beliefcollection;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.Vector;

import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;

public abstract class BeliefCollection {

	POMDP POMDP;
	public ValueIteration valueIteration;
	double epsilon = ExecutionProperties.getEpsilon();
	protected boolean m_bAllowDuplicates;
	
	public BeliefCollection (ValueIteration vi, boolean bAllowDuplicates)
	{
		valueIteration = vi;		
		POMDP = vi.getPOMDP();
		m_bAllowDuplicates = bAllowDuplicates;
	}
	
	public String toString()
	{
		return this.getClass().getSimpleName();
	}
	
	public void removeDuplicates(Vector<BeliefState> beliefPoints)
	{
		Collection<BeliefState> cleaned = new LinkedHashSet<BeliefState>(beliefPoints);
		beliefPoints.clear();
		beliefPoints.addAll(cleaned);	
	}
	
	/* default expansion method */
	public abstract Vector<BeliefState> expand(Vector<BeliefState> beliefPoints);
	
	/* expand to collect numNewBeliefs new belief points */
	public abstract Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints);

	
	public abstract Vector<BeliefState> initialBelief();

	public String allowDuplicates() {
		if(m_bAllowDuplicates)
			return "AllowDuplicates";
		else
			return "NoDuplicates";
	}
	
}
