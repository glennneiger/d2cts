package org.scheduling.bb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.locks.ReentrantLock;

import org.com.model.scheduling.BBParametersBean;
import org.com.model.scheduling.BranchAndBoundParametersBean;
import org.com.model.scheduling.ParameterBean;
import org.exceptions.EmptyResourcesException;
import org.exceptions.MissionNotFoundException;
import org.missions.Load;
import org.missions.Mission;
import org.missions.MissionState;
import org.scheduling.GlobalScore;
import org.scheduling.LocalScore;
import org.scheduling.MissionScheduler;
import org.scheduling.ScheduleEdge;
import org.scheduling.ScheduleResource;
import org.scheduling.ScheduleTask;
import org.scheduling.display.JMissionScheduler;
import org.system.Terminal;
import org.time.DiscretObject;
import org.time.Time;
import org.time.TimeScheduler;
import org.vehicles.StraddleCarrier;

public class BB extends MissionScheduler {
	/**
	 * RMI BINDING NAME
	 */
	public static final String rmiBindingName = "BB";

	/**
	 * Used to get each resource
	 */
	private int resourceIndex = 0;

	private double bound;

	private long evalNodes = 0;
	private int foundTimes = 0;

	private boolean recompute = true;

	public BB() {
		super();
		MissionScheduler.instance = this;
		if(!init)
			jms = new JMissionScheduler();
	}

	public static void closeInstance(){
		//Nothing to do
	}

	@Override
	protected void init() {
		init = true;
		lock = new ReentrantLock();
		if(evalParameters == null){
			evalParameters = BBParametersBean.getEvalParameters();
		}
		sstep = TimeScheduler.getInstance().getStep() + 1;
		step = 0;
		graphChanged = true;

		for (StraddleCarrier rsc : resources) {
			if(!jms.containsResource(rsc)){
				jms.addResource(rsc);
			}
		}
		jms.getIndicatorPane().updateUI();
		jms.getJTabbedPane().updateUI();
	}
	
	

	@Override
	public void addMission(Time t, Mission m) {
		super.addMission(t, m);
		recompute = true;
	}

	@Override
	public boolean removeMission(Time t, Mission m) {
		recompute = true;
		return super.removeMission(t, m);
	}

	@Override
	public void addResource(Time t, StraddleCarrier rsc) {
		super.addResource(t, rsc);
		recompute = true;
	}



	@Override
	public boolean removeResource(Time t, StraddleCarrier rsc) {
		recompute = true;
		return super.removeResource(t, rsc); 
	}

	@Override
	public void precompute() {
		if(!init)
			init();
		
		if (graphChanged || graphChangedByUpdate > 0) {
			processEvents();
			lock.lock();
			graphChanged = false;
			graphChangedByUpdate = 0;
			lock.unlock();
		}

		// TEST
		// GlobalScore global1 = new GlobalScore();
		// ScheduleResource v1 = scheduleResources.get("straddleCarrier_1");
		// LocalScore ls1 = new LocalScore(v1);
		// System.out.println("G1 : "+global1+"\nG1 S:"+global1.getSolutionString());
		// GlobalScore global12 = new GlobalScore(global1);
		//
		// ScheduleTask<ScheduleEdge> t0 = (ScheduleTask<ScheduleEdge>)
		// scheduleTasks.get("stockMission_A[0]");
		// LocalScore ls2 = v1.simulateVisitNode(ls1, t0);
		// System.out.println("G1 : "+global1+"\nG1 S:"+global1.getSolutionString());
		// global12.addScore(ls2);
		// System.out.println("G12 : "+global12+"\nG12 S:"+global12.getSolutionString());
		//
		// ScheduleTask<ScheduleEdge> t1 = (ScheduleTask<ScheduleEdge>)
		// scheduleTasks.get("stockMission_A[1]");
		// ls2 = v1.simulateVisitNode(ls1, t1);
		// System.out.println("G12 : "+global12+"\nG12 S:"+global12.getSolutionString());
		// global12.addScore(ls2);
		// System.out.println("G12 -2 : "+global12+"\nG12 S:"+global12.getSolutionString());
		//
		// //System.out.println("G1 : "+global1+"\nG1 S:"+global1.getSolutionString());
		// System.exit(0);
		if (recompute && !resources.isEmpty() && !pool.isEmpty()) {
			// COMPUTE ALGORITHM
			compute();
		}
	}

	@Override
	public boolean apply() {
		boolean returnCode = NOTHING_CHANGED;
		step++;
		sstep++;
		if(precomputed){
			precomputed = false;
			returnCode = SOMETHING_CHANGED;
		}
		return returnCode;
		
//		if (precomputed) {
//			pool.clear();
//
//			Map<String, LocalScore> solutions = best.getSolution();
//			for (String colonyID : solutions.keySet()) {
//				StraddleCarrier vehicle = vehicles.get(colonyID);
//
//				// TO REMOVE
//				List<String> toRemove = new ArrayList<String>();
//				for (Load l : vehicle.getWorkload().getLoads()) {
//					if (l.getState() == MissionState.STATE_TODO) {
//						toRemove.add(l.getMission().getId());
//					}
//				}
//				for (String mID : toRemove) {
//					try {
//						vehicle.removeMissionInWorkload(mID);
//					} catch (MissionNotFoundException e1) {
//						// ALREADY REMOVED
//					}
//				}
//			}
//			for (String colonyID : solutions.keySet()) {
//				StraddleCarrier vehicle = vehicles.get(colonyID);
//				LocalScore score = solutions.get(colonyID);
//				List<ScheduleEdge> path = score.getPath();
//				if (path.size() > 0) {
//					List<Mission> toAffect = new ArrayList<Mission>(
//							path.size() - 1);
//					for (ScheduleEdge e : path) {
//						if (e.getNodeTo() != SOURCE_NODE) {
//							ScheduleTask<? extends ScheduleEdge> missionNode = e
//									.getNodeTo();
//							toAffect.add(missionNode.getMission());
//						}
//					}
//					vehicle.addMissionsInWorkload(toAffect);
//
//				}
//			}
//
//			Terminal.getInstance().flushAllocations();
//
//			precomputed = false;
//			step++;
//			returnCode = DiscretObject.SOMETHING_CHANGED;
//		}
//
//		sstep++;
//		return returnCode;
	}
		
	@Override
	public void compute() {
		System.out.println("BBParameters : ");
		for(ParameterBean b : BBParametersBean.getAll()){
			System.out.println(b.toString());
		}
		
		System.out.println("COMPUTE : " + resources.size() + " ; "
				+ pool.size());
		if (BBParametersBean.SOLUTION_INIT_FILE.getValueAsString().equals("")) {
			// Compute max bound
			// HashMap<String, List<String>> boundSolution = new HashMap<String,
			// List<String>>();

			/*
			 * ScheduleResource.resetVisited(); for(ScheduleResource sr :
			 * scheduleResources.values()) { sr.reset();
			 * System.err.println("RESET : "
			 * +sr.getID()+" @ "+sr.getCurrentTime()); }
			 */

			//int missionAffected = 0;
			best = new GlobalScore();
			Iterator<Mission> itMissions = pool.iterator();
			while (itMissions.hasNext()) {
				StraddleCarrier rsc = pickAStraddleCarrier();
				if (rsc.isAvailable()) {
					ScheduleResource sr = scheduleResources.get(rsc.getId());
					LocalScore ls = null;
					if (best.getSolution().containsKey(sr.getID()))
						ls = best.getSolution().get(sr.getID());
					else
						ls = new LocalScore(sr);
					Mission m = itMissions.next();
					ls = sr.simulateVisitNode(ls, scheduleTasks.get(m.getId()));
					best.addScore(ls);
					// scheduleResources.get(rsc.getId()).visitNode(scheduleTasks.get(m.getId()));
					//missionAffected++;
				}

			}
			System.out.println("INITIAL SOLUTION : " + best + "\n"
					+ best.getSolutionString());
			foundTimes = 1;
			// ScheduleResource.resetVisited();
			// best = new GlobalScore();
			// for(ScheduleResource sr : scheduleResources.values()){
			// best.addScore(sr.getLocalScore());
			// sr.reset();
			// System.err.println("RESET2 : "+sr.getID()+" @ "+sr.getCurrentTime());
			// }
		} else {
			try {
				System.out.println("READ SOLUTION");
				best = readSolution(BBParametersBean.SOLUTION_INIT_FILE.getValueAsString());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		bound = best.getScore();

		if (!BBParametersBean.SOLUTION_INIT_FILE.getValueAsString().equals(BBParametersBean.SOLUTION_FILE.getValueAsString())) {
			// Must have found a solution
			System.out.println("BOUND : " + bound + " && " + best.toString());

			// Now compute B&B with the found max bound
			long tBefore = System.nanoTime();
			computeBB(new GlobalScore());
			long tAfter = System.nanoTime();
			computeTime += tAfter - tBefore;
			Time timeCost = new Time(((tAfter - tBefore) / 1000000000.0));
			System.out.println("BEST SOLUTION FOUND : " + bound + " found "
					+ foundTimes + " times afeter evaluated " + evalNodes
					+ " nodes in " + timeCost + " :\n" + best.toString());
			for (LocalScore ls : best.getSolution().values()) {
				System.out.print(ls.getResource().getID() + " : ");
				for (ScheduleEdge se : ls.getPath()) {
					System.out.print(se.getNodeTo().getID() + " -> ");
				}
				System.out.println();
			}
			if (!BBParametersBean.SOLUTION_FILE.getValueAsString().equals("")) {
				try {
					System.out.println("WRITE SOLUTION");
					writeSolution(best, BBParametersBean.SOLUTION_FILE.getValueAsString());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("BEST SOLUTION FOUND : (" + bound + " | "
					+ best.toString());
		}
		recompute = false;
	}

	private List<GlobalScore> getChildren(GlobalScore current) {

		List<GlobalScore> children = new ArrayList<GlobalScore>();

		List<ScheduleTask<? extends ScheduleEdge>> remainingTasks = MissionScheduler
				.getInstance().getUnallocatedTasks(current);

		for (ScheduleTask<? extends ScheduleEdge> task : remainingTasks) {

			for (ScheduleResource resource : MissionScheduler.getInstance()
					.getScheduleResources()) {
				GlobalScore global = new GlobalScore(current);
				LocalScore before = current.getSolution().get(resource.getID());
				if (before == null)
					before = new LocalScore(resource);

				// System.err.println("S = "+s.toString()+" && "+s.getSolutionString());
				// System.err.println("BEFORE "+resource.getID()+" SIMULATE VISITING "+task.getID()+" : "+s.getSolution().get(resource.getID()));
				LocalScore local = new LocalScore(resource.simulateVisitNode(
						before, task));
				// System.err.println("CHILDREN "+local+" | \n"+before);
				global.addScore(local);
				children.add(global);
				// new Scanner(System.in).nextLine();
			}

		}
		// System.err.println("IN CHILDREN : CURRENT = "+current.toString()+" && "+current.getSolutionString()+" there are "+children.size()+" children.");
		// System.err.println("1st Child : "+children.get(0).getSolutionString());
		return children;
	}

	private void computeBB(GlobalScore currentSolution) {
		lock.lock();
		evalNodes++;
		lock.unlock();

		boolean display = false;

		List<GlobalScore> children = getChildren(currentSolution);

		if (currentSolution.getTaskCount() == 0) {
			System.out.println("ROOT");
			display = true;
		}

		int childCount = 1;
		for (GlobalScore child : children) {
			if (display)
				System.out.println("CHILD n°" + childCount + "/"
						+ children.size());

			childCount++;
			if (child.getScore() <= bound) {
				// IF VISITED ALL CITIES
				if (child.getTaskCount() == MissionScheduler.getInstance()
						.getPoolSize()) {
					// ADD GOBACK TO DEPOT
					for (String resourceID : child.getSolution().keySet()) {
						ScheduleResource resource = scheduleResources
								.get(resourceID);
						LocalScore ls = child.getSolution().get(resourceID);
						ls = resource.simulateVisitNode(ls,
								MissionScheduler.SOURCE_NODE);
						child.addScore(ls);
					}
					if (child.getScore() < bound) {
						// New record
						System.out.println("FOUND " + foundTimes + " TIMES !");
						foundTimes = 1;
						best = new GlobalScore(child);
						bound = best.getScore();
						System.out.println("NEW RECORD : " + bound + " | "
								+ best.toString() + " "
								+ best.getSolutionString());
					} else if (child.getScore() == bound) {
						foundTimes++;
					}

				} else {
					// Keep searching
					// GlobalScore copy = new GlobalScore(child);
					computeBB(new GlobalScore(child));
				}
			}
			// else STOP SEARCHING
		}
	}

	/*
	 * ------------------------------------------- OVERRIDE REMOTE MISSION
	 * SCHEDULER METHODS --------------------------------------------
	 */
	@Override
	public String getId() {
		return BB.rmiBindingName;
	}

	/* UTIL */

	/**
	 * Return each resource in the same order at each time
	 * 
	 * @return a resource
	 */
	private StraddleCarrier pickAStraddleCarrier() {
		StraddleCarrier rsc = resources.get(resourceIndex);
		resourceIndex++;
		if (resourceIndex == resources.size())
			resourceIndex = 0;
		return rsc;
	}

	private GlobalScore readSolution(String filename)
			throws FileNotFoundException {
		File f = new File(filename);
		if (!f.exists()) {
			f.getParentFile().mkdirs();
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Scanner scan = new Scanner(f);
		// RESET VEHICLES
		resetVisited();
		for (ScheduleResource sr : scheduleResources.values()) {
			sr.reset();
		}
		GlobalScore global = new GlobalScore();

		// RUN SOLUTION
		while (scan.hasNextLine()) {
			String line = scan.nextLine();
			StringTokenizer st = new StringTokenizer(line);
			String resource = st.nextToken();
			String task = st.nextToken();
			ScheduleResource sr = scheduleResources.get(resource);
			LocalScore ls = null;
			if (global.getSolution().containsKey(sr.getID()))
				ls = global.getSolution().get(sr.getID());
			else
				ls = new LocalScore(sr);
			ls = sr.simulateVisitNode(ls, scheduleTasks.get(task));
			global.addScore(ls);
		}
		for (String resourceID : global.getSolution().keySet()) {
			ScheduleResource resource = scheduleResources.get(resourceID);
			LocalScore ls = global.getSolution().get(resourceID);
			ls = resource.simulateVisitNode(ls,
					MissionScheduler.SOURCE_NODE);
			global.addScore(ls);
		}

		scan.close();

		return global;
	}

	private void writeSolution(GlobalScore toWrite, String fileName)
			throws FileNotFoundException {
		String s = "";
		Map<String, LocalScore> solMap = toWrite.getSolution();
		for (String rID : solMap.keySet()) {
			LocalScore local = solMap.get(rID);
			for (ScheduleEdge edge : local.getPath()) {
				if (edge.getNodeTo().getMission() != null) {
					s += rID + "\t" + edge.getNodeTo().getID() + "\n";
				}
			}
		}

		PrintWriter pw = new PrintWriter(fileName);
		pw.append(s);
		pw.flush();
		pw.close();
	}

}
