package soot.jimple.infoflow.android.nu;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.xml.internal.bind.v2.model.core.ID;

import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.CastExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.nu.FlowTriggerEventObject.EventID;
import soot.jimple.infoflow.android.nu.FlowTriggerEventObject.EventOriginObject;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.ARSCFileParser.AbstractResource;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResConfig;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResPackage;
import soot.jimple.infoflow.android.resources.ARSCFileParser.ResType;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.util.queue.QueueReader;

public class FlowTriggerEventAnalyzer {
	private static final Logger LOGGER = Logger.getLogger("NUFlow");
	{
		LOGGER.setLevel(Level.ALL);
	}

	private List<ResPackage> resourcePackages;
	private Infoflow savedInfoFlow;
	private HashMap<String, FlowTriggerEventObject> flowTriggerEventObjectMap;
	private String  apkFileLocation;
	private String appPackageName;
	ProcessManifest processMan;
	ARSCFileParser resParser;
	private Map<Integer, List<String>> id2Text;
	
	public FlowTriggerEventAnalyzer( Infoflow savedInfoFlow, String apkFileLocation){
		this.flowTriggerEventObjectMap = new HashMap<String, FlowTriggerEventObject>();
		this.savedInfoFlow = savedInfoFlow;
		this.apkFileLocation =  apkFileLocation;
		this.processMan = null;
		try{
			processMan = new ProcessManifest(apkFileLocation);
		}
		catch(Exception e){
			LOGGER.severe("failed to init FlowTriggerEventAnalyzer: ProcessManifest");
			e.printStackTrace();
		}
		this.appPackageName = processMan.getPackageName();
		
		resParser = new ARSCFileParser();
		try {
			resParser.parse(apkFileLocation);
		} catch (Exception e) {
			LOGGER.severe("failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
			e.printStackTrace();
		}
		this.resourcePackages = resParser.getPackages();	
		id2Text = null;
	}

	public void analyzeRegistrationCalls(){
		Map<String, String> view2Layout = new HashMap<String, String>();
		
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
		    LocalDefs localDefs = LocalDefs.Factory.newLocalDefs(g);
		    Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
		    //System.err.println("DEBUG=======================Start Method:"+m.getName()+" ==============");
		    for (Unit u : orderer.newList(g, false)) {
		    	Stmt s = (Stmt)u;
		    	//System.err.println("DEBUG: RunAnalysi Statement: "+s+" || "+s.getClass().getTypeName());
		    	if(s.containsInvokeExpr()){
		    		InvokeExpr expr = s.getInvokeExpr();
		    		SootMethod invokedM = expr.getMethod();
		    		
		    		if(invokedM.getName().equals("findViewById")){
		    			System.err.println("DEBUG: findViewById: "+u);
		    			 
		    		}
		    		else if(invokedM.getName().equals(FlowTriggerEventObject.onClickRegisterMethodName)){
		    			System.err.println("DEBUG BEGIN: "+s);
		    			Value arg = expr.getArg(0);
		    			if (! (arg instanceof Local) ) {
		    				System.err.println("Error: setOnClickListener arg is not Local.");
		    				continue;   
		    			}
		    			Local larg = (Local)arg;
	    				List<Unit> defsOfUse = localDefs.getDefsOfAt(larg, u);
	    				if (defsOfUse.size() != 1){
	    					System.err.println("Error: cannot find setOnClickListener arg defs");
	    					continue;
	    				}
	    				DefinitionStmt defStmt = (DefinitionStmt) defsOfUse.get(0);
	    				
	    				SootClass viewClass = invokedM.getDeclaringClass();
	    				String eventListenerClassName = defStmt.getRightOp().getType().toString();
	    				System.err.println("  Found setOnClickListener: "+viewClass+" ==> "+defStmt.getRightOp().getType());
	    				
	    				List<ValueBox> values = u.getUseBoxes();
	    				if(values.size() != 3){
	    					System.err.println("Error: the size of UseBoxes is not 2: "+values.size());
	    					continue;
	    				}
	    				Value obj = values.get(1).getValue();
	    				if(! (obj instanceof Local) ){
	    					System.err.println("Error: setOnClickListener is not registered on a local variable.");
	    					continue;
	    				}
	    				
	    				Local lObj = (Local)obj;
	    				defsOfUse = localDefs.getDefsOfAt(lObj, u);
	    				if (defsOfUse.size() != 1){
	    					System.err.println("Error: cannot find setOnClickListener source obj defs");
	    					continue;
	    				}
	    				defStmt = (DefinitionStmt) defsOfUse.get(0);
	    				System.err.println("  def:"+defStmt);
	    				String type = defStmt.getRightOpBox().getValue().getType().toString();
	    				String full = defStmt.getRightOp().toString();
	    				String name = "";
	    				String cls = "";
	    				if(defStmt.getRightOp() instanceof InstanceFieldRef){
	    					Pattern p = Pattern.compile(".+<(.+): ("+type+") (.+)>");
	    					Matcher matcher = p.matcher(full);
	    					if(matcher.find()){
	    						name = matcher.group(3);
	    						cls = matcher.group(1);
	    					}
	    					//find id
	    					int id = findResourceID(name, "id");
	    					//System.err.println(" EventSrcObject:\n  Name:"+name+"\n  id :"+id+"\n  type:"+type+"\n  cls :"+cls);

	    					if (!flowTriggerEventObjectMap.containsKey(eventListenerClassName)){
	    						FlowTriggerEventObject eventObj = new FlowTriggerEventObject(EventID.onClick, eventListenerClassName);
	    						flowTriggerEventObjectMap.put(eventListenerClassName, eventObj);
	    						eventObj.addTriggerEventSrcObject(name, type, cls, id);
	    					}
	    					else {
	    						flowTriggerEventObjectMap.get(eventListenerClassName)
	    							.addTriggerEventSrcObject(name, type, cls, id);
	    					}
	    					//System.err.println("  InstanceFieldRef: "+defStmt.getRightOp());
	    				}
	    				else if(defStmt.getRightOp() instanceof CastExpr){
	    					//((Button) findViewById(R.id.start_button))
	    					//  .setOnClickListener(new OnClickListener() {});
	    					CastExpr castExpr = (CastExpr)defStmt.getRightOp();
	    					//System.err.println("XIANG: castExpr op: "+castExpr.getOp());
	    					if(! (castExpr.getOp() instanceof Local) ) {
	    						System.err.println("Error: castExpr's op is not local:"+castExpr);
		    					continue;
	    					}
	    							
	    					System.err.println("  castexpr: def:"+defStmt+" op:"+castExpr.getOp()+" left:"+defStmt.getLeftOp());
	    					DefinitionStmt btnDefStmt = null;
	    					Queue<Pair<Unit, Integer>> queue = new java.util.concurrent.LinkedBlockingQueue<Pair<Unit, Integer>>();
	    					Set<Unit> visitedUnits = new HashSet<Unit>();
	    					for(Unit su : g.getPredsOf(defStmt)){
	    						if(!visitedUnits.contains(su)){
	    							queue.add(new Pair<Unit, Integer>(su, 1));
	    							visitedUnits.add(su);
	    						}
	    					}
	    					while(!queue.isEmpty()){
	    						Pair<Unit, Integer> item = queue.poll();
	    						//System.out.println("DEBUG: XX: "+item.first.getClass()+" ||"+item.first);
	    						if(item.first instanceof DefinitionStmt){
	    							DefinitionStmt tmpDStmt = (DefinitionStmt)item.first;
	    							if(tmpDStmt.getLeftOp() == castExpr.getOp()){
	    								btnDefStmt = tmpDStmt;
	    								break;
	    							}
	    						}
	    						List<Unit> next = g.getPredsOf(item.first);
	    						for(Unit n : next){
	    							if(!visitedUnits.contains(n)){
	    								queue.add(new Pair<Unit, Integer>(n, item.second+1));
	    								visitedUnits.add(n);
	    							}
	    						}
	    					}
	    					
	    					if(btnDefStmt == null){
	    						defsOfUse = localDefs.getDefsOfAt((Local)(castExpr.getOp()), u);
	    						if (defsOfUse.size() != 1){
	    							System.err.println("Error: cannot find castExpr's defs:"+castExpr);
		    						continue;
	    						}
	    						btnDefStmt = (DefinitionStmt) defsOfUse.get(0);
	    					}
	    					
	    					if(btnDefStmt.getRightOp() instanceof InvokeExpr){
	    						InvokeExpr ie = (InvokeExpr)(btnDefStmt.getRightOp());
	    						if(ie.getMethod().getName().equals("findViewById")){
	    							System.err.println("findViewById: "+ie.getArg(0)+" D:"+ie.getMethod().getDeclaringClass());
	    							if (!flowTriggerEventObjectMap.containsKey(eventListenerClassName)){
	    	    						FlowTriggerEventObject eventObj = new FlowTriggerEventObject(EventID.onClick, eventListenerClassName);
	    	    						flowTriggerEventObjectMap.put(eventListenerClassName, eventObj);
	    	    						eventObj.addTriggerEventSrcObject("", type, m.getDeclaringClass().toString(),Integer.valueOf(ie.getArg(0).toString()) );
	    	    					}
	    	    					else {
	    	    						flowTriggerEventObjectMap.get(eventListenerClassName)
	    	    							.addTriggerEventSrcObject("", type, m.getDeclaringClass().toString(),Integer.valueOf(ie.getArg(0).toString()) );
	    	    					}
	    						}
	    						else{
	    							System.err.println("  Alert: castExpr unexpected function call:"+castExpr);
	    						}
	    					}
	    				}
	    				else{
	    					System.err.println(" Alert: Unhandled TYPE:"+defStmt.getRightOp().getClass());
	    				}
	    				
	    				
		    		} //setOnClickListener
		    		else if(invokedM.getName().equals("setContentView")){
		    			//System.err.println("DEBUG setContentView: "+u);
		    			Value arg = expr.getArg(0);
		    			if(! (arg instanceof IntConstant) ){
		    				System.err.println("Error: setContentView arg is not integer.");
		    				continue;
		    			}
		    			IntConstant ib = (IntConstant)arg;
		    			
		    			//System.err.println("DEBUG setContentView: "+arg);
		    			List<ValueBox> values = u.getUseBoxes();
		    			if(values.size() != 3){
		    				System.err.println("Error: setContentView invoke has "+values.size()+" use boexs. Should be 3.");
		    				continue;
		    			}
		    			Value obj = values.get(1).getValue();
	    				if(! (obj instanceof Local) ){
	    					System.err.println("Error: setContentView is not called on a local variable.");
	    					continue;
	    				}
	    				Local lObj = (Local)obj;
	    				List<Unit> defsOfUse = localDefs.getDefsOfAt(lObj, u);
	    				if (defsOfUse.size() != 1){
	    					System.err.println("Error: cannot find setContentView source obj defs");
	    					continue;
	    				}
	    				
	    				DefinitionStmt defStmt = (DefinitionStmt) defsOfUse.get(0);
	    				//System.err.println("DEBUG setContentView: [" + defStmt.getRightOp().getType().toString()+"] ID:"+ib.value+" "+defStmt);
	    				String viewClsName = defStmt.getRightOp().getType().toString();
	    				int layoutID = ib.value;
	    				
		    			for(ARSCFileParser.ResPackage rp : resourcePackages){
    						for (ResType rt : rp.getDeclaredTypes()){
    							if(!rt.getTypeName().equals("layout"))
    								continue;
    							for (ResConfig rc : rt.getConfigurations())
    								for (AbstractResource res : rc.getResources()){
    									if(res.getResourceID() == layoutID)
    										view2Layout.put(viewClsName, res.getResourceName());
    									//System.err.println("DEBUG LAYOUT: "+res.getResourceName()+" => "+res.getResourceID());				
    								}
    						}
    					}
		    		} //setContentView
		    	}
		    	
		    }
		    //displayGraph(g, m.getName(), null, false );
		}
		
		LayoutFileParserForTextExtraction lfp = null;
		
		lfp = new LayoutFileParserForTextExtraction(this.appPackageName, resParser);
		lfp.parseLayoutFileForTextExtraction(apkFileLocation);	
		id2Text = lfp.getId2Texts();
		
		//update flowTriggerEventObject's text attributes
		for(String listenerClass : flowTriggerEventObjectMap.keySet()){
			FlowTriggerEventObject obj = flowTriggerEventObjectMap.get(listenerClass);
			System.err.println("EventListenerClass Name: "+listenerClass);
			for(EventOriginObject oo : obj.getEventOriginObjects()){
				//oo.setText(findResourceID());
				if(id2Text.containsKey(oo.getId()) ){
					List<String> tmp = id2Text.get(oo.getId());
					String texts = "";
					for(String s : tmp){
						if (texts.length() > 0)
							texts += "\n";
						texts += s.trim();
					}
					oo.setText(texts);
				}
				if(view2Layout.containsKey(oo.getDeclaringClass())){
					String layoutName = (String)view2Layout.get(oo.getDeclaringClass());
					if(lfp.getTextTreeMap().containsKey(layoutName)){
						oo.setDeclaringClsLayoutTextTree(lfp.getTextTreeMap().get(layoutName));
					}
				}
				System.err.println("  "+oo.toString());
			}
			System.err.println();
		}
		
		for(String callback : lfp.getCallbackMap().keySet()){
			FlowTriggerEventObject obj = lfp.getCallbackMap().get(callback);
			System.err.println("Callback Name: "+callback);
			for(EventOriginObject eoo : obj.getEventOriginObjects()){
				System.err.println("  "+eoo.toString());
			}
			System.err.println();
		}
		
		
	}
	
	/* The type could be "id" or "string" */
	private int findResourceID(String name, String type){
		for(ARSCFileParser.ResPackage rp : resourcePackages){
			for (ResType rt : rp.getDeclaredTypes()){
				if(!rt.getTypeName().equals(type))
					continue;
				for (ResConfig rc : rt.getConfigurations())
					for (AbstractResource res : rc.getResources()){
						System.err.println("DEBUG ID: "+res.getResourceName()+" => "+res.getResourceID());
						if (res.getResourceName().equalsIgnoreCase(name))
							return res.getResourceID();
					}
				}
		}
		
		
		return 0;
	}
	
	public Infoflow getSavedInfoFlow(){
		return this.savedInfoFlow;
	}
	public Map<String, FlowTriggerEventObject> getFlowTriggerEventObjectMap(){
		return this.flowTriggerEventObjectMap;
	}
	
	private void displayGraph(UnitGraph g,String graphName, List<Unit> startingUnits, boolean reverse){
		if(startingUnits == null)
			startingUnits = g.getHeads();
		Queue<Pair<Unit, Integer>> queue = new java.util.concurrent.LinkedBlockingQueue<Pair<Unit, Integer>>();
		Set<Unit> visitedUnits = new HashSet<Unit>();
		System.err.println("DEBUG DisplayGraph: "+graphName);
		for(Unit su : startingUnits){
			if(!visitedUnits.contains(su)){
				queue.add(new Pair<Unit, Integer>(su, 1));
				visitedUnits.add(su);
			}
		}
		
		while(!queue.isEmpty()){
			Pair<Unit, Integer> item = queue.poll();
			String space = new String(new char[item.second*2]).replace('\0', ' ');
			System.err.println("  DEBUG G:"+space+item.first+" ");
			if(item.second > 20)
				break;
			List<Unit> next = (reverse? g.getPredsOf(item.first) : g.getSuccsOf(item.first));
			for(Unit n : next){
				if(!visitedUnits.contains(n)){
					queue.add(new Pair<Unit, Integer>(n, item.second+1));
					visitedUnits.add(n);
				}
			}
		}
		System.err.println();
	}
}
