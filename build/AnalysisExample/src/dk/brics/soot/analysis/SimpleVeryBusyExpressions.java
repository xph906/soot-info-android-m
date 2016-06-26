package dk.brics.soot.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Unit;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.FlowSet;

public class SimpleVeryBusyExpressions implements VeryBusyExpressions {
	private Map unitToExpressionsAfter;
	private Map unitToExpressionsBefore;
	
	public SimpleVeryBusyExpressions(DirectedGraph graph) {
		SimpleVeryBusyAnalysis analysis = new SimpleVeryBusyAnalysis(graph);

		unitToExpressionsAfter = new HashMap(graph.size() * 2 + 1, 0.7f);
		unitToExpressionsBefore = new HashMap(graph.size() * 2 + 1, 0.7f);
		Iterator unitIt = graph.iterator();
		while (unitIt.hasNext()) {
			Unit s = (Unit) unitIt.next();
			FlowSet set = (FlowSet) analysis.getFlowBefore(s);
			unitToExpressionsBefore.put(s,
					Collections.unmodifiableList(set.toList()));
			set = (FlowSet) analysis.getFlowAfter(s);
			unitToExpressionsAfter.put(s,
					Collections.unmodifiableList(set.toList()));
		}
	}
	
	@Override
	public List getBusyExpressionsBefore(Unit s) {
		List foo = (List) unitToExpressionsBefore.get(s);
		return foo;
	}

	@Override
	public List getBusyExpressionsAfter(Unit s) {
		return (List) unitToExpressionsAfter.get(s);
	}

}
