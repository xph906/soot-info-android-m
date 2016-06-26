package dk.brics.soot;
import java.io.File;
import java.util.Iterator;
import java.util.List;

import dk.brics.soot.analysis.SimpleVeryBusyExpressions;
import dk.brics.soot.analysis.VeryBusyExpressions;
import soot.Body;
import soot.Main;
import soot.NormalUnitPrinter;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPrinter;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.jimple.internal.*;
import testers.VeryBusyClass;

public class RunVeryBusyAnalysis {
	public static void main(String[] args) {

		args = new String[]{
				"-cp", "/Users/xpan/Documents/projects/FlowDroid/build/jre1.7.0_79.jre/Contents/Home/lib/jce.jar:/Users/xpan/Documents/projects/FlowDroid/build/jre1.7.0_79.jre/Contents/Home/lib/rt.jar:/Users/xpan/Documents/projects/FlowDroid/build/AnalysisExample/bin",
				"testers.VeryBusyClass"};
		
		Main.v().run(args);
		
		SootClass sClass = Scene.v().loadClassAndSupport("testers.VeryBusyClass");		
		sClass.setApplicationClass();
		System.err.println(sClass.toString());
		Iterator methodIt = sClass.getMethods().iterator();
		
		while (methodIt.hasNext()) {
			SootMethod m = (SootMethod)methodIt.next();
			Body b = m.retrieveActiveBody();
			
			System.out.println("=======================================");			
			System.out.println(m.toString());
			UnitGraph graph = new ExceptionalUnitGraph(b);
			VeryBusyExpressions vbe = new SimpleVeryBusyExpressions(graph);
			
			Iterator gIt = graph.iterator();
			while (gIt.hasNext()) {
				Unit u = (Unit)gIt.next();
				List before = vbe.getBusyExpressionsBefore(u);
				List after = vbe.getBusyExpressionsAfter(u);
				UnitPrinter up = new NormalUnitPrinter(b);
				up.setIndent("");
				
				System.out.println("---------------------------------------");			
				u.toString(up);			
				System.out.println(up.output());
				System.out.print("Busy in: {");
				String sep = "";
				Iterator befIt = before.iterator();
				while (befIt.hasNext()) {
					AbstractBinopExpr e = (AbstractBinopExpr)befIt.next();
					System.out.print(sep);
					System.out.print(e.toString());
					sep = ", ";
				}
				System.out.println("}");
				System.out.print("Busy out: {");
				sep = "";
				Iterator aftIt = after.iterator();
				while (aftIt.hasNext()) {
					AbstractBinopExpr e = (AbstractBinopExpr)aftIt.next();
					System.out.print(sep);
					System.out.print(e.toString());
					sep = ", ";
				}			
				System.out.println("}");			
				System.out.println("---------------------------------------");
			}
			
			System.out.println("=======================================");
		}
	}
}
