package dk.brics.soot.callgraphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

public class CallGraphExample
{	
	public static void main(String[] args) {
	   List<String> argsList = new ArrayList<String>(Arrays.asList(args));
	   argsList.addAll(Arrays.asList(new String[]{
			   "-w","-ire",
			   //"-cp", "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/rt.jar:/Users/xpan/Documents/projects/FlowDroid/build/AnalysisExample/bin",
			   "-cp", "/Users/xpan/Documents/projects/FlowDroid/build/jre1.7.0_79.jre/Contents/Home/lib/jce.jar:/Users/xpan/Documents/projects/FlowDroid/build/jre1.7.0_79.jre/Contents/Home/lib/rt.jar:/Users/xpan/Documents/projects/FlowDroid/build/AnalysisExample/bin",
			   "-main-class",
			   "testers.CallGraphs",//main-class
			   "testers.CallGraphs",//argument classes
			   "testers.A",			//
	   }));
	
	   testers.A a = new testers.A();
	   
	   PackManager.v().getPack("wjtp").add(
			   new Transform("wjtp.myTrans", new SceneTransformer() {
			@Override
			protected void internalTransform(String phaseName, Map options) {
					long t0 = System.currentTimeMillis();
			       //CHATransformer.v().transform();
	               SootClass a = Scene.v().getSootClass("testers.A");
	               long t1 = System.currentTimeMillis();
	               System.out.println("time1: "+(t1-t0));
			       SootMethod src = Scene.v().getMainClass().getMethodByName("doStuff");
			       CallGraph cg = Scene.v().getCallGraph();
			       System.out.println( "Number of reachable methods: "
			                +Scene.v().getReachableMethods().size() );
			       
			       long t2 = System.currentTimeMillis();
			       System.out.println("time2: "+(t2-t1));
			       Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(src));
			       while (targets.hasNext()) {
			           SootMethod tgt = (SootMethod)targets.next();
			           System.out.println(src + " may call " + tgt);
			       }
			       long t3 = System.currentTimeMillis();
			       System.out.println("time3: "+(t3-t2));
			}
	   }));

       args = argsList.toArray(new String[0]);
           
       soot.Main.main(args);
	}
}
