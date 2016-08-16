package soot.jimple.infoflow.android.nu;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.xml.internal.bind.v2.model.core.ID;

import soot.Immediate;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AnyNewExpr;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;
import soot.jimple.CastExpr;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.Expr;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InstanceOfExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.NewArrayExpr;
import soot.jimple.NewExpr;
import soot.jimple.NewMultiArrayExpr;
import soot.jimple.NullConstant;
import soot.jimple.NumericConstant;
import soot.jimple.ParameterRef;
import soot.jimple.Ref;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThisRef;
import soot.jimple.UnopExpr;
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
import soot.shimple.ShimpleExpr;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.Orderer;
import soot.toolkits.graph.PseudoTopologicalOrderer;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.LocalDefs;
import soot.util.queue.QueueReader;

public class FlowTriggerEventAnalyzer {
	public class MyFormatter extends Formatter {

	    /**
	     * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	     */
	    @Override
	    public String format(final LogRecord record) {
	        return MessageFormat.format(record.getMessage(), record.getParameters());
	    }
	}
	
	private static final Logger log = Logger.getLogger("NUFlow");
	{
		//for(Handler h : log.getHandlers())
		//	h.setFormatter(new MyFormatter());
		log.setLevel(Level.ALL);
	}

	private List<ResPackage> resourcePackages;
	private Infoflow savedInfoFlow;
	private HashMap<String, FlowTriggerEventObject> flowTriggerEventObjectMap;
	private String  apkFileLocation;
	private String appPackageName;
	ProcessManifest processMan;
	ARSCFileParser resParser;
	private Map<Integer, List<String>> id2Text;
	private VariableDefAnalyzer varDefAnalyzer;
	
	/*private Map<String, List<Value>> findLocalVaraibleDefIntra(Local target, Unit startingPoint, UnitGraph g){
		Map<String, List<Value>> rs = new HashMap<String, List<Value>>();
		Map<Local, Unit> targets = new HashMap<Local, Unit>();
		targets.put(target, startingPoint);
		
		while(!targets.isEmpty()){
			Set<Unit> visitedUnit = new HashSet<Unit>();
			Queue<Unit> q = new java.util.concurrent.LinkedBlockingQueue<Unit>();
			Local t = targets.keySet().iterator().next();
			Unit u = targets.get(t);
			targets.remove(t);
			for(Unit child : g.getPredsOf(u))
				q.add(child);
			visitedUnit.add(u);
			while(!q.isEmpty()){
				Unit curUnit = q.poll();
				
				//pre-processing this unit
				List<ValueBox> defs = curUnit.getDefBoxes();
				if(defs == null || defs.size()<=0)
					continue;
				else if(defs.size() >1){
					log.warning("Multiple defs in one statement:"+curUnit);
					continue;
				}
				Value defValue = defs.get(0).getValue();
				
				//processing this unit
				if(defValue instanceof Local){
					if( !((Local)defValue).equals(t) )
						continue;
					if(! (curUnit instanceof DefinitionStmt) ){
						log.warning("[ALERT]: def not in DefinitionStmt:"+curUnit);
						continue;
					}
					//defValue == t && statement instanceof DefinitionStmt
					DefinitionStmt as = (DefinitionStmt)curUnit;
					log.info("findLocalVaraibleDefIntra: found def of "+t+" "+curUnit.getClass()+" Right:"+as.getRightOp()+" RC:"+as.getRightOp().getClass());
					Value right = as.getRightOp();
					if(right instanceof Constant){
						log.info("  right is a const");
					}
					else if(right instanceof Expr){
						log.info("  right is an expression");
					}
					else if(right instanceof Stmt){
						log.info("  right is a stmt");
					}
					else if(right instanceof Ref){
						log.info("  right is Ref: ");
						if (right instanceof ParameterRef){
							ParameterRef pr = (ParameterRef)right;
							log.info("    right is ParameterRef: "+pr.getIndex());
							
						}
						else if(right instanceof ThisRef){
							//TODO:
							log.info("    right is ArrayRef TODO");
						}
						else if(right instanceof ArrayRef){
							//TODO:
							log.info("    right is ArrayRef TODO");
						}
						else if(right instanceof FieldRef){
							//TODO:
							log.info("    right is FieldRef TODO");
						}
						else{
							log.warning("    right is unknown Ref: "+right.getClass());
						}
						
					}
					else{
						log.info("  right is unknown: "+right.getClass());
					}
				}
				else{
					log.warning("Non local def: "+curUnit);
				}
				
				//post-processing this unit
				visitedUnit.add(curUnit);
				for(Unit child : g.getPredsOf(curUnit)){
					if(!visitedUnit.contains(child))
						q.add(child);
				}
			}
		}
		return rs;
	}
	
	private boolean twoUnitsReachable(Unit u1, Unit u2, UnitGraph g){
		Set<Unit> visitedTargets = new HashSet<Unit>();
		Queue<Unit> q = new java.util.concurrent.LinkedBlockingQueue<Unit>();
		q.add(u1);
		while(!q.isEmpty()){
			Unit c = q.poll();
			if(c == u2)
				return true;
			else{
				visitedTargets.add(c);
				for(Unit child : g.getSuccsOf(c)){
					if (! visitedTargets.contains(child))
						q.add(child);
				}
			}
		}
		visitedTargets.clear();
		q.clear();
		q.add(u2);
		while(!q.isEmpty()){
			Unit c = q.poll();
			if(c == u1)
				return true;
			else{
				visitedTargets.add(c);
				for(Unit child : g.getSuccsOf(c)){
					if (! visitedTargets.contains(child))
						q.add(child);
				}
			}
		}
		return false;
	}
	*/
	
	
	public FlowTriggerEventAnalyzer( Infoflow savedInfoFlow, String apkFileLocation){
		this.flowTriggerEventObjectMap = new HashMap<String, FlowTriggerEventObject>();
		this.savedInfoFlow = savedInfoFlow;
		this.apkFileLocation =  apkFileLocation;
		this.processMan = null;
		try{
			processMan = new ProcessManifest(apkFileLocation);
		}
		catch(Exception e){
			log.severe("failed to init FlowTriggerEventAnalyzer: ProcessManifest");
			e.printStackTrace();
		}
		this.appPackageName = processMan.getPackageName();
		
		resParser = new ARSCFileParser();
		try {
			resParser.parse(apkFileLocation);
		} catch (Exception e) {
			log.severe("failed to init FlowTriggerEventAnalyzer: ARSCFileParser");
			e.printStackTrace();
		}
		this.resourcePackages = resParser.getPackages();	
		id2Text = null;
		varDefAnalyzer = new VariableDefAnalyzer();
	}
	public void analyzePredicates(){
		for (QueueReader<MethodOrMethodContext> rdr =
				Scene.v().getReachableMethods().listener(); rdr.hasNext(); ) {
			SootMethod m = rdr.next().method();
			if(!m.hasActiveBody())
				continue;
			UnitGraph g = new ExceptionalUnitGraph(m.getActiveBody());
			log.info("Analyze Method:"+m.getName());
			displayGraph(g, m.getName(), g.getHeads(),false);
			
			 Orderer<Unit> orderer = new PseudoTopologicalOrderer<Unit>();
			for (Unit u : orderer.newList(g, false)) {
				log.info("  COND:S"+u+" "+u.getClass());
				
				if(u.branches()){
					
					log.info("  BRANCH:"+u+" "+u.getClass()+" ");
					//log.info("    T1:"+g.getSuccsOf(u).get(0));
					//log.info("    T2:"+g.getSuccsOf(u).get(1));
					if(u instanceof IfStmt){
						IfStmt ifStmt = (IfStmt)u;
						//log.info(" COND: C"+ifStmt.getCondition()+" ");
						for(ValueBox b : ifStmt.getUseBoxes()){
							log.info("  COND: vb:"+b+" "+b.getValue());
							if(b.getValue() instanceof Local){
								log.info("    COND: local"+b.getValue());
								//Local l = (Local)b.getValue();
								
							varDefAnalyzer.findLocalVaraibleDefIntra(b.getValue(), u, g, false, null, null, null,0);
							}
							else if(b.getValue() instanceof Immediate){
								log.info("    COND: immediate:"+b.getValue());
							}
							else {
								//soot.jimple.internal.ConditionExprBox;
								log.warning("    COND: Stmt:"+b.getValue()+" "+b.getClass());
							}	
						}
					}
					else{
						log.warning("COND: unknown condition stmt:"+u+" "+u.getClass());
					}
				}//if branch
				else if(u instanceof DefinitionStmt){
					//DEBUG:
					/*
					DefinitionStmt as = (DefinitionStmt)u;
					Value t = as.getLeftOp();
					log.info("DEBUG DefStmt: found def of "+t+" "+u.getClass()+"\n  UNIT:"+u);
					log.info("  left:"+as.getLeftOp()+" "+as.getLeftOp().getClass());
					log.info("  right:"+as.getRightOp());
					Value right = as.getRightOp();
					if(right instanceof Constant){
						log.info("  right is a const");
						Constant c = (Constant)right;
						if(c instanceof NumericConstant){
							NumericConstant nc = (NumericConstant)c;
							log.info("    right numeric const:"+nc.toString());
						}
						else if(c instanceof StringConstant){
							StringConstant sc = (StringConstant)c;
							log.info("    right string const:"+sc.toString());
						}
						else if(c instanceof NullConstant){
							log.info("    right null const:");
						}
						else{
							log.info("    right's constant value is unknown: "+c.getClass());
						}
						
					}
					else if(right instanceof Expr){
						log.info("  right is an expression");
						
						if(right instanceof NewMultiArrayExpr){
							log.info("  right is NewMultiArrayExpr expression TODO");
							
						}
						else if(right instanceof NewArrayExpr){
							
							log.info("  right is NewArrayExpr expression TODO: "+((NewArrayExpr)right).getBaseType() );
							
						}
						else if(right instanceof NewExpr){
							log.info("  right is NewExpr expression TODO");
							List<Unit> succs  = g.getSuccsOf(u);
							Unit initUnit = findConstructorInvokeStmt(((DefinitionStmt) u).getLeftOp(), u, g);
							if(initUnit==null)
								log.warning("    cannot find initialization method.");
							else
								log.info("    NewExpr InitInvoke: "+initUnit);
							displayGraph(g, "NewExpr:"+m.getName(),g.getHeads(), false);
							log.info("  done right is NewExpr expression TODO");
						}
						else if(right instanceof AnyNewExpr){
							log.info("  right is AnyNewExpr expression TODO");
							displayGraph(g, "AnyNewExpr:"+m.getName(),g.getHeads(), false);
							log.info("  done right is AnyNewExpr expression TODO");
						}
						else if(right instanceof BinopExpr){
							log.info("  right is BinopExpr expression TODO");
						}
						else if(right instanceof CastExpr){
							log.info("  right is CastExpr expression TODO");
						}
						else if(right instanceof InstanceOfExpr){
							log.info("  right is InstanceOfExpr expression TODO");
						}
						else if(right instanceof InvokeExpr){
							log.info("  right is InvokeExpr expression TODO");
						}
						
						else if(right instanceof UnopExpr){
							log.info("  right is UnopExpr expression TODO");
						}
						else if(right instanceof ShimpleExpr){
							log.info("  right is ShimpleExpr expression TODO");
						}
						else{
							log.info("    right is unknown expression:"+right);
						}
					}
					else if(right instanceof Stmt){
						log.info("  right is a stmt");
					}
					else if(right instanceof Ref){
						log.info("  right is Ref: ");
						if (right instanceof ParameterRef){
							ParameterRef pr = (ParameterRef)right;
							log.info("    right is ParameterRef: "+pr.getIndex());
						}
						else if(right instanceof ThisRef){
							//TODO:
							log.info("    right is ThisRef TODO");
							displayGraph(g, "This Ref ", g.getSuccsOf(u), false);
						}
						else if(right instanceof ArrayRef){
							//TODO:
							log.info("    right is ArrayRef TODO");
						}
						else if(right instanceof FieldRef){
							//TODO:
							
							FieldRef fr = (FieldRef)right;
							log.info("    right is FieldRef TODO: "+fr.getField().getDeclaration()+" ||Name:"+fr.getField().getName()+
									" ||"+fr.getType()+" ||FRType:"+right.getType());
							
						}
						else{
							log.warning("    right is unknown Ref: "+right.getClass());
						}
						
					}
					else if(right instanceof Local){
						log.info("    right is Local.");
					}
					else{
						log.warning("  right is unknown: "+right.getClass());
					} 
					*/
				}
			}
		}
	}
	
	//DEBUG function, can be removed after testing.
	private Unit findConstructorInvokeStmt(Value objValue, Unit startingUnit, UnitGraph g){
		Set<Unit> visitedUnit = new HashSet<Unit>();
		Queue<Unit> q = new java.util.concurrent.LinkedBlockingQueue<Unit>();
		q.addAll(g.getSuccsOf(startingUnit));
		visitedUnit.add(startingUnit);
		
		while(!q.isEmpty()){
			Unit s = q.poll();
			visitedUnit.add(s);
			
			if(s instanceof InvokeStmt){
				InvokeStmt is = (InvokeStmt)s;
				InvokeExpr ie = is.getInvokeExpr();
				if(ie instanceof InstanceInvokeExpr){
					if(is.getInvokeExpr().getMethod().getName().equals("<init>") && 
							((InstanceInvokeExpr)ie).getBase().equivTo(objValue))
						return s;
				}
			}
			
			for(Unit child : g.getSuccsOf(s)){
				if (!visitedUnit.contains(child))
					q.add(child);
			}	
		}
		return null;
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
	
	static public void displayGraph(UnitGraph g,String graphName, List<Unit> startingUnits, boolean reverse){
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
			//if(item.second > 20)
			//	break;
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
