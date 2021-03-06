package org.vehicles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.conf.parameters.ReturnCodes;
import org.system.Terminal;
import org.system.container_stocking.Quay;

public class Ship implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6979083276541144921L;

	private double rateFrom, rateTo;

	private Quay quay;

	private Set<String> toUnload;
	private Set<String> toLoad;

	private List<String> concernedSlotsIDs;

	private int capacity;

	private double currentLoadTEU;
	private String ID;

	public Ship(int capacity, Quay quay, double rateFrom, double rateTo,
			List<String> concernedSlotsIDs,
			Set<String> toUnload) {
		this.quay = quay;
		this.rateFrom = rateFrom;
		this.rateTo = rateTo;
		this.ID = quay.getId() + "-" + rateFrom + "-" + rateTo;

		this.capacity = capacity;
		currentLoadTEU = 0;
		this.toUnload = new HashSet<>();
		for (Iterator<String> itContainers = toUnload.iterator(); itContainers.hasNext();) {
			String s = itContainers.next();
//			Container c = Terminal.getInstance().getContainer(s);
//			if(c==null){
//				System.err.println("Container "+s+" not found!");
//			}
//			double teu = c.getTEU();
//			if (currentLoadTEU + teu <= capacity) {
				this.toUnload.add(s);
//				currentLoadTEU += teu;
//			} else {
//				// TODO Replace by an exception
//				System.out.println("Error : ship capacity exceeded !");
//				break;
//			}
		}
		this.toLoad = new HashSet<>();
		this.concernedSlotsIDs = concernedSlotsIDs;
	}

	public double getRateFrom() {
		return rateFrom;
	}

	public double getRateTo() {
		return rateTo;
	}

	public Quay getQuay() {
		return quay;
	}

	public int getCapacity() {
		return capacity;
	}

	public List<String> getToUnload() {
		return new ArrayList<String>(toUnload);
	}

	public List<String> getToLoad() {
		return new ArrayList<String>(toLoad);
	}

	public List<String> getConcernedSlotsIDs() {
		return concernedSlotsIDs;
	}

	/**
	 * 
	 * @param containerID
	 */
	// TODO ADD NOT FOUND EXCEPTION !
	public void containerUnloaded(String containerID) {
		if (toUnload.contains(containerID)) {
			toUnload.remove(containerID);
			System.out.println("Container " + containerID
					+ " has been remove from toUnload list (size="
					+ toUnload.size() + ") !");
			Terminal.getInstance().updateShip(this);

		} else {
			System.out.println("Container " + containerID
					+ " not found in toUnload list !");
		}
		/*
		 * int size = toUnload.size(); for(int i=0;i<toUnload.size();i++){
		 * if(toUnload.get(i).equals(containerID)) { toUnload.remove(i);
		 * System.out.println("Container "+containerID+
		 * " has been remove from toUnload list (size="+toUnload.size()+") !");
		 * try { Terminal.getRMIInstance().updateShip(this); } catch
		 * (RemoteException e) { e.printStackTrace(); } break; } } if(size ==
		 * toUnload.size()) System.out.println("Container "+containerID+
		 * " not found in toUnload list !");
		 */
	}

	// TODO ADD NOT FOUND EXCEPTION !
	public void containerLoaded(String containerID) {
		if (!toLoad.contains(containerID)) {
			System.out.println("Container " + containerID
					+ " not found in toLoad list !");
			new Exception("Container " + containerID
					+ " not found in toLoad list !").printStackTrace();
			System.exit(ReturnCodes.EXIT_ON_CONTAINER_NOT_FOUND_ERROR.getCode());

		} else {
			toLoad.remove(containerID);
			Terminal.getInstance().updateShip(this);
		}
		System.err
				.println("Container " + containerID + " loaded on " + getID());
	}

	public void addContainerToLoad(String containerID) {
		toLoad.add(containerID);
	}

	public double getCurrentLoadTEU() {
		return currentLoadTEU;
	}

	public void destroy() {
		concernedSlotsIDs.clear();
		concernedSlotsIDs = null;
		quay = null;
		toLoad.clear();
		toLoad = null;
		toUnload.clear();
		toUnload = null;
	}

	public void resetContainerToLoad() {
		toLoad.clear();
	}

	public String getID() {
		return ID;
	}
}
