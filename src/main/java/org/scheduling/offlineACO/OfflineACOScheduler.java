package org.scheduling.offlineACO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.com.model.scheduling.OfflineACOParametersBean;
import org.exceptions.EmptyResourcesException;
import org.missions.Mission;
import org.routing.path.Path;
import org.scheduling.GlobalScore;
import org.scheduling.LocalScore;
import org.scheduling.MissionScheduler;
import org.scheduling.ScheduleEdge;
import org.scheduling.ScheduleTask;
import org.scheduling.display.IndicatorPane;
import org.scheduling.display.JMissionScheduler;
import org.system.Terminal;
import org.time.DiscretObject;
import org.time.Time;
import org.time.TimeScheduler;
import org.vehicles.StraddleCarrier;

/**
 * Mission Scheduler using an ACO based algorithm to solve the TSP using a
 * complete graph modeling
 * 
 * @author Ga&euml;tan Lesauvage
 * @since 2012
 */
public final class OfflineACOScheduler extends MissionScheduler {
	// RMI
	public static final String rmiBindingName = "OfflineACOScheduler";

	// ACO TSP
	private OfflineSchedulerParameters globalParameters;

	// TIME SYNCHRONIZATION
	private int syncSize;

	/**
	 * Max number of ants in a colony
	 */
	private int nbAntMax; // Used to iterate one ant of each colony at a time

	// private static final HashMap<String, ScheduleTask<TSPEdge>> missions =
	// new HashMap<String, ScheduleTask<TSPEdge>>();
	private Map<String, OfflineAnt> hills;

	// Best score handling
	// private TSPGlobalScore best;
	private OfflineSchedulerStats stats;

	/* public static ScheduleTask<TSPEdge> SOURCE_NODE; */

	/* ======================== CONSTRUCTORS ======================== */
	/**
	 * Constructor
	 */
	public OfflineACOScheduler() {
		super();
		if (globalParameters == null) {
			globalParameters = OfflineACOParametersBean.getParameters();
			evalParameters = OfflineACOParametersBean.getEvalParameters();
			syncSize = globalParameters.getSync();
			hills = new HashMap<String, OfflineAnt>();
			log.info("TSP ACO PARAMETERS: " + globalParameters);
			log.info("TSP EVAL PARAMETERS: " + evalParameters);
		}
		MissionScheduler.instance = this;
		if (!init)
			init();
	}

	public static OfflineACOScheduler getInstance() {
		return (OfflineACOScheduler) MissionScheduler.instance;
	}

	public static void closeInstance() {

	}

	/**
	 * Update the max count of ants in a colony according to the given new ant
	 * count of the colony.
	 * 
	 * @param newAntCount
	 *            New ant count in the colony where an ant has been added.
	 */
	public void antAdded(int newAntCount) {
		if (nbAntMax < newAntCount)
			nbAntMax = newAntCount;
	}

	/*
	 * ============================== INITIALIZATION
	 * ==============================
	 */
	/**
	 * Initialize both the algorithm and the GUI.
	 */
	@Override
	protected void init() {
		init = true;
		computeTime = 0;
		step = 0;
		sstep = TimeScheduler.getInstance().getStep() + 1;

		lock.lock();
		graphChanged = true;
		lock.unlock();

		SOURCE_NODE = new ScheduleTask<OfflineEdge>((Mission) null);

		jms = new JMissionScheduler();
		for (StraddleCarrier rsc : resources) {
			jms.addResource(rsc);
		}

		int current = 1;
		List<StraddleCarrier> lResources = Terminal.getInstance().getStraddleCarriers();
		int overall = lResources.size();
		for (StraddleCarrier rsc : lResources) {
			log.info("resource " + rsc.getId() + " (" + current + "/" + overall + ")");
			current++;
			insertResource(rsc);
		}
		current = 1;
		List<Mission> lTasks = Terminal.getInstance().getMissions();
		overall = lTasks.size();
		for (Mission m : lTasks) {
			log.info("task " + m.getId() + " (" + current + "/" + overall + ")");
			current++;
			insertMission(m);
		}
	}

	/*
	 * ============================== DISCRETE PART OF THE SCHEDULER
	 * ==============================
	 */
	@Override
	/**
	 * Precompute is called by the TimeScheduler at each step of simulation.
	 */
	public void precompute() {
		if (graphChanged || graphChangedByUpdate > 0) {
			processEvents();
			// GraphChanged may have been set to false in processEvents() sub
			// methods
			if (graphChanged/* ||graphChangedByUpdate>0 */) {
				// new
				// Exception("GO IN PRECOMPUTE : "+graphChanged+" "+graphChangedByUpdate).printStackTrace();
				if (!resources.isEmpty() && !pool.isEmpty()) {
					lock.lock();
					precomputed = true;
					lock.unlock();

					compute();

					lock.lock();
					graphChanged = false;
					lock.unlock();
				}

			}

			lock.lock();
			graphChangedByUpdate = 0;
			lock.unlock();

		}

	}

	@Override
	/**
	 * Apply is called by the TimeScheduler at each step of the simulation after calling precompute() method of each DiscretObject of the scheduler. 
	 */
	public boolean apply() {
		boolean returnCode =  DiscretObject.NOTHING_CHANGED;
		if (precomputed) {
			// System.out.println("APPLY : ");
			
			//Reset workloads
			 for(StraddleCarrier sc : resources){
				 sc.clearWorkload();
			 }
			
			// AFFECT SOLUTION
			Map<String, LocalScore> solutions = best.getSolution();
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
			
			
			
			for (String colonyID : solutions.keySet()) {
				StraddleCarrier vehicle = vehicles.get(colonyID);
				LocalScore score = solutions.get(colonyID);
				List<ScheduleEdge> path = score.getPath();
				if (path.size() > 0) {
					List<Mission> toAffect = new ArrayList<Mission>(path.size() - 1);
					for (ScheduleEdge e : path) {
						if (e.getNodeTo() != SOURCE_NODE) {
							ScheduleTask<? extends ScheduleEdge> missionNode = e.getNodeTo();
							toAffect.add(missionNode.getMission());
						}
					}

					vehicle.addMissionsInWorkload(toAffect);

				}
			}

			Terminal.getInstance().flushAllocations();

			lock.lock();
			precomputed = false;
			lock.unlock();

			log.info("AVG Scores : distance= " + stats.getAVG_Distance() + " overspent_time= " + new Time(stats.getAVG_OverspentTime()) + " score= "
					+ stats.getAVG_Score());
			log.info("SOLUTION : \n" + best.getSolutionString());
			// stats.exportGNUPlot("/home/gaetan/TSPStats.dat");
			log.info("CPT TIME = " + new Time(getComputingTime()));
			returnCode = DiscretObject.SOMETHING_CHANGED;
		} else {
			returnCode = DiscretObject.NOTHING_CHANGED;
		}
		sstep++;
		return returnCode;
	}

	/*
	 * ============================== CONTINOUS PART OF THE SCHEDULER
	 * ==============================
	 */

	@Override
	/**
	 * Compute the ACO algorithm $syncSize times
	 */
	public void compute() {
		stats = new OfflineSchedulerStats(globalParameters);
		// Reset best score
		best = null;

		if (jms == null) {
			execute();
		} else {
			// TODO assert syncSize > 0
			int missionsToPlan = scheduleTasks.size() * resources.size();
			log.info("M2Plan = " + missionsToPlan + " steps = " + (syncSize * missionsToPlan));
			for (int i = 0; i < syncSize * missionsToPlan; i++) {
				execute();
				if (MissionScheduler.DEBUG && ((i - 1) % 100 == 0)){
					log.info("Step " + (i - 1));
				}
			}
			log.info("Computation done.");
		}

	}

	/**
	 * Execute 1 step of the ACO algorithm
	 */
	private void execute() {
		long before = System.nanoTime();

		OfflineAnt.compute();
		GlobalScore current = MissionScheduler.getInstance().getCurrentGlobalScore();
		stats.addScore(new GlobalScore(current));
		if (best == null) {
			best = new GlobalScore(current);
			OfflineAnt.spreadPheromone();

			log.info("OfflineACO:> [" + step + "/" + (scheduleTasks.size() * resources.size() * globalParameters.getSync()) + "] NEW RECORD " + best
					+ " [" + new Time(getComputingTime()) + "]");

		} else {
			if (best.compareTo(current) > 0) {
				best = new GlobalScore(current);

				OfflineAnt.spreadPheromone();

				log.info("OfflineACO:> [" + step + "/" + (scheduleTasks.size() * resources.size() * globalParameters.getSync()) + "] NEW RECORD "
						+ best + " [" + new Time(getComputingTime()) + "]");
			}
		}
		// TSPHill.spreadPheromone();

		step++;
		long after = System.nanoTime();
		computeTime += (after - before);
	}

	/*
	 * ========================== TASKS AND RESOURCES OPERATIONS
	 * ==========================
	 */
	/**
	 * Insert a vehicle as a resource in the algorithm.
	 * 
	 * @param rsc
	 *            The straddle carrier to add.
	 */
	@Override
	protected synchronized void insertResource(StraddleCarrier rsc) {
		String modelID = rsc.getModel().getId();
		String rscID = rsc.getId();

		resources.add(rsc);
		vehicles.put(rscID, rsc);
		if (jms != null)
			jms.addResource(rsc);

		OfflineAnt ant = new OfflineAnt(rsc);
		scheduleResources.put(rscID, ant);
		hills.put(rscID, ant);
		// Compute costs for rsc
		boolean computeCosts = true;
		for (String model : resourceModels.keySet()) {
			if (model.equals(modelID)) {
				computeCosts = false;
				break;
			}
		}

		List<String> lModel = null;
		if (computeCosts) {
			lModel = new ArrayList<String>();
			for (ScheduleTask<? extends ScheduleEdge> n : scheduleTasks.values()) {
				n.addCostFor(rsc);
			}
		} else {
			lModel = resourceModels.get(modelID);

		}

		lModel.add(rscID);
		resourceModels.put(modelID, lModel);

		log.info("TSP:> Resource " + rscID + " (" + modelID + ") added");
	}

	@Override
	public synchronized void incrementNumberOfCompletedMissions(String resourceID) {
		IndicatorPane ip = jms.getIndicatorPane();
		if (ip != null) {
			ip.incNbOfCompletedMissions(resourceID);
		} else
			new Exception("INDICATOR PANE IS NULL").printStackTrace();
	}

	/**
	 * Insert mission
	 *
	 * @param m
	 * Mission to insert
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected synchronized void insertMission(Mission m) {
		pool.add(m);

		ScheduleTask<OfflineEdge> n = new ScheduleTask<OfflineEdge>(m);
		for (String modelID : resourceModels.keySet()) {
			try {
				Path p = getTravelPath(m, m,
						vehicles.get(resourceModels.get(modelID).get(0)));
				n.addMissionCostFor(modelID, p.getCost());
				n.setDistance(p.getCostInMeters());
			} catch (EmptyResourcesException e) {
				e.printStackTrace();
			}
		}

		for (Mission m2 : pool) {
			if (!m2.getId().equals(m.getId())) {
				ScheduleTask<OfflineEdge> n2 = (ScheduleTask<OfflineEdge>)
						scheduleTasks.get(m2.getId());
				OfflineEdge e = new OfflineEdge(n, n2);
				OfflineEdge e2 = new OfflineEdge(n2, n);

				for (String modelID : resourceModels.keySet()) {
					e.addCost(vehicles.get(resourceModels.get(modelID).get(0)));
					n.addDestination(e);
					e2.addCost(vehicles.get(resourceModels.get(modelID).get(0)));
					n2.addDestination(e2);
				}
			}
		}

		OfflineEdge e3 = new OfflineEdge((ScheduleTask<OfflineEdge>)
				MissionScheduler.SOURCE_NODE, n);
		OfflineEdge e4 = new OfflineEdge(n, (ScheduleTask<OfflineEdge>)
				MissionScheduler.SOURCE_NODE);

		for (String vehicleID : vehicles.keySet()) {
			e3.addCost(vehicles.get(vehicleID));
			((ScheduleTask<OfflineEdge>)
					(MissionScheduler.SOURCE_NODE)).addDestination(e3);
			e4.addCost(vehicles.get(vehicleID));
			n.addDestination(e4);
		}

		scheduleTasks.put(m.getId(), n);
		log.info("TSP:> Task " + m.getId() + " added");
	}

	/**
	 * Remove mission
	 * 
	 * @param m
	 *            Mission to remove
	 * @return
	 */
	@Override
	protected boolean removeMission(Mission m) {
		boolean found = super.removeMission(m);
		if (found) {
			// Remove the mission from the workloads //TODO

			// Remove the node and concerned edges
			@SuppressWarnings("unchecked")
			ScheduleTask<OfflineEdge> n = (ScheduleTask<OfflineEdge>) scheduleTasks.remove(m.getId());

			@SuppressWarnings("unchecked")
			ScheduleTask<OfflineEdge> source = (ScheduleTask<OfflineEdge>) SOURCE_NODE;

			// DEPOT -> n
			source.removeEdgeTo(n);
			n.removeEdgeTo(source);
			// n -> n2 && n2 -> n
			for (ScheduleTask<? extends ScheduleEdge> n2 : scheduleTasks.values()) {
				@SuppressWarnings("unchecked")
				ScheduleTask<OfflineEdge> n3 = (ScheduleTask<OfflineEdge>) n2;
				n3.removeEdgeTo(n);
				n.removeEdgeTo(n3);
			}
			n.destroy();
		}

		return found;
	}

	/*
	 * ========================== TASKS AND RESOURCES LISTENERS
	 * ==========================
	 */
	/*
	 * @Override public void missionStarted(Time t, Mission m, String
	 * resourceID) { // DO NOT RECOMPUTE SOLUTION WHEN THE CURRENT ONE IS GOING
	 * ON // lock.lock(); // graphChanged = true; // lock.unlock();
	 * 
	 * // REMOVE MISSION FROM SCHEDULER Set<MissionStartedHelper> toStartList =
	 * null; if (missionsToStart.containsKey(t)) toStartList =
	 * missionsToStart.get(t); else toStartList = new
	 * HashSet<MissionStartedHelper>();
	 * 
	 * toStartList.add(new MissionStartedHelper(m, resourceID));
	 * missionsToStart.put(t, toStartList); }
	 */

	/*
	 * @Override protected void missionStarted(Mission m, String resourceID) {
	 * //System.err.println("STARTED MISSION "+m.getId()+" by "+resourceID);
	 * removeMission(m); //TODO: /*try {
	 * hillsNodes.get(resourceID).getStraddleCarrier().addMissionInWorkload(m);
	 * } catch (RemoteException e) { e.printStackTrace(); }
	 */
	// System.err.println("MISSION "+m.getId()+" REMOVED FROM GRAPH AND READDED IN "+resourceID+"'s WORKLOAD");
	// }

	/*
	 * @Override public boolean removeMission(Time t, Mission m) { lock.lock();
	 * graphChanged = true; lock.unlock();
	 * 
	 * List<Mission> toRemoveList = null; if (missionsToRemove.containsKey(t))
	 * toRemoveList = missionsToRemove.get(t); else toRemoveList = new
	 * ArrayList<Mission>();
	 * 
	 * int i = 0; for (Mission m2 : toRemoveList) { int comp =
	 * m2.getId().compareTo(m.getId()); if (comp >= 0) break; else i++; }
	 * toRemoveList.add(i, m);
	 * 
	 * missionsToRemove.put(t, toRemoveList); return true; }
	 */

	/*
	 * @Override public void updateMission(Time t, Mission m) { lock.lock();
	 * graphChanged = true; lock.unlock();
	 * 
	 * List<Mission> toUpdateList = null; if (missionsToUpdate.containsKey(t))
	 * toUpdateList = missionsToUpdate.get(t); else toUpdateList =
	 * Collections.synchronizedList(new ArrayList<Mission>());
	 * 
	 * int i = 0; for (Mission m2 : toUpdateList) { int comp =
	 * m2.getId().compareTo(m.getId()); if (comp >= 0) break; else i++; }
	 * toUpdateList.add(i, m);
	 * 
	 * missionsToUpdate.put(t, toUpdateList); }
	 */

	@Override
	public void addMission(Time t, Mission m) {
		lock.lock();
		graphChanged = true;
		lock.unlock();

		List<Mission> toAddList = null;
		if (missionsToAdd.containsKey(t))
			toAddList = missionsToAdd.get(t);
		else
			toAddList = new ArrayList<Mission>();

		int i = 0;
		for (Mission m2 : toAddList) {
			int comp = m2.getId().compareTo(m.getId());
			if (comp >= 0)
				break;
			else
				i++;
		}
		toAddList.add(i, m);

		missionsToAdd.put(t, toAddList);
	}

	/* ========================== GETTERS AND SETTERS ========================== */
	@Override
	public String getId() {
		return MissionScheduler.rmiBindingName;
	}

	/**
	 * Getter on the parameters of the algorithm
	 * 
	 * @return The parameters of the ACO algorithm
	 */
	public OfflineSchedulerParameters getGlobalParameters() {
		return globalParameters;
	}

	/**
	 * Setter on the global parameters of the ACO algorithm
	 * 
	 * @param p
	 */
	public void setGlobalParameters(OfflineSchedulerParameters p) {
		globalParameters = p;
	}

	public int getSyncSize() {
		return syncSize;
	}

	/**
	 * Change the synchronization size of the algorithm ($syncSize ACO
	 * iterations / 1 simulation step)
	 * 
	 * @param newSyncSize
	 *            New synchronization size
	 */
	public void setSyncSize(final int newSyncSize) {
		syncSize = newSyncSize;
	}

	/* ========================== DESTRUCTOR ========================== */
	/**
	 * Destruct the object
	 */
	@SuppressWarnings("unchecked")
	public void destroy() {
		super.destroy();

		// globalParameters = null;

		for (OfflineAnt h : hills.values()) {
			h.destroy();
		}

		hills.clear();
		for (String s : scheduleTasks.keySet()) {
			ScheduleTask<OfflineEdge> n = (ScheduleTask<OfflineEdge>) scheduleTasks.get(s);
			for (OfflineEdge e : n.getDestinations()) {
				n.removeEdgeTo((ScheduleTask<OfflineEdge>) e.getNodeTo());
			}
		}
		for (ScheduleTask<? extends ScheduleEdge> n : scheduleTasks.values()) {
			n.destroy();
		}
		scheduleTasks.clear();

		if (SOURCE_NODE != null)
			SOURCE_NODE.destroy();
	}
}
