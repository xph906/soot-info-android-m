package dk.brics.soot.analysis;

import java.util.Iterator;

import dk.brics.soot.flowsets.ValueArraySparseSet;
import soot.Local;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.BinopExpr;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;
import soot.toolkits.scalar.FlowSet;

public class SimpleVeryBusyAnalysis extends BackwardFlowAnalysis {
	public SimpleVeryBusyAnalysis(DirectedGraph graph) {
		super(graph);
		//System.out.println(graph);
		emptySet = new ValueArraySparseSet();
		doAnalysis();
	}

	private FlowSet emptySet;
	
	@Override
	protected void flowThrough(Object in, Object d, Object out) {
		FlowSet inSet = (FlowSet)in,
				outSet = (FlowSet)out;
		Unit u = (Unit)d;
		// out <- (in - expr containing locals defined in d) union out 
		kill(inSet, u, outSet);
		// out <- out union expr used in d
		gen(outSet, u);
	}

	@Override
	protected Object newInitialFlow() {
		return emptySet.clone();
	}
	
	@Override
	protected Object entryInitialFlow(){
		return emptySet.clone();
	}
	
	@Override
	protected void merge(Object in1, Object in2, Object out) {
		FlowSet inSet1 = (FlowSet)in1,
				inSet2 = (FlowSet)in2,
				outSet = (FlowSet)out;
		inSet1.intersection(inSet2, outSet);

	}

	@Override
	protected void copy(Object source, Object dest) {
		FlowSet srcSet = (FlowSet)source,
				destSet = (FlowSet)dest;
		srcSet.copy(destSet);

	}
	
	/**
	 * Performs kills by generating a killSet and then performing<br/>
	 * outSet <- inSet - killSet<br/>
	 * The kill set is generated by iterating over the def-boxes
	 * of the unit. For each local defined in the unit we iterate
	 * over the binopExps in the inSet, and check whether they use
	 * that local. If so, it is added to the kill set.
	 * @param inSet the set flowing into the unit
	 * @param u the unit being flown through
	 * @param outSet the set flowing out of the unit
	 */
	private void kill(FlowSet inSet, Unit u, FlowSet outSet) {
		if(inSet == null){
			System.err.println("inSet is null");
			return ;
		}
		FlowSet kills = (FlowSet)emptySet.clone();
		Iterator defIt = u.getDefBoxes().iterator();
		while (defIt.hasNext()) {
			ValueBox defBox = (ValueBox)defIt.next();

			if (defBox.getValue() instanceof Local) {
				
				Iterator inIt = inSet.iterator();
				
				while (inIt.hasNext()) {		
					BinopExpr e = (BinopExpr)inIt.next();
					
					Iterator eIt = e.getUseBoxes().iterator();

					
					while (eIt.hasNext()) {
						
						ValueBox useBox = (ValueBox)eIt.next();
						//System.err.println("    subbinbox:"+useBox.toString()+" kill:"+defBox.toString());
						if (useBox.getValue() instanceof Local &&
								useBox.getValue().equivTo(defBox.getValue()))
							kills.add(e);
					}
				}
			}
		}
		inSet.difference(kills, outSet);
	}
	
	/**
	 * Performs gens by iterating over the units use-boxes.
	 * If the value of a use-box is a binopExp then we add
	 * it to the outSet.
	 * @param outSet the set flowing out of the unit
	 * @param u the unit being flown through
	 */
	private void gen(FlowSet outSet, Unit u) {
		Iterator useIt = u.getUseBoxes().iterator();
		System.err.println("genBox:"+u.toString());
		while (useIt.hasNext()) {
			ValueBox useBox = (ValueBox)useIt.next();
			System.err.println("  useBox:"+useBox.toString()+" v:"+useBox.getValue());
			if (useBox.getValue() instanceof BinopExpr)
				outSet.add(useBox.getValue());
		}
	}

}
