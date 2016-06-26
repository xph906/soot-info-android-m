package dk.brics.soot.analysis;

import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.BackwardFlowAnalysis;

public class FlowAnalysisTemplate extends BackwardFlowAnalysis {

	public FlowAnalysisTemplate(DirectedGraph graph) {
		super(graph);
		doAnalysis();
	}

	@Override
	protected void flowThrough(Object in, Object d, Object out) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Object newInitialFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void merge(Object in1, Object in2, Object out) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void copy(Object source, Object dest) {
		// TODO Auto-generated method stub
		
	}

}
