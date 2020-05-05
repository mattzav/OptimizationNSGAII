import java.util.ArrayList;
import java.util.HashMap;

public class Vehicles {
	private int capacity;
	private int currentCapacity;
	private double tourLength;
	private ArrayList<Integer> tour;

	private HashMap<Integer, ArrayList<Integer>> droneTour;
	private HashMap<Integer, Double> currentDroneTourLength;
	private HashMap<Integer, Double> currentDroneTourProfit;

	public Vehicles(int capacity) {

		this.capacity = capacity;
		this.currentCapacity = capacity;
		this.tourLength = 0;

		this.tour = new ArrayList<Integer>();
		this.tour.add(0);

		this.droneTour = new HashMap<Integer, ArrayList<Integer>>();
		this.currentDroneTourLength = new HashMap<Integer, Double>();
		this.currentDroneTourProfit = new HashMap<Integer, Double>();

	}

	public Vehicles(Vehicles v) {
		this.capacity = v.capacity;
		this.currentCapacity = v.getCurrentCapacity();
		this.tourLength = v.getTourLength();
		this.tour = new ArrayList<Integer>(v.getTour());
		this.droneTour = new HashMap<Integer, ArrayList<Integer>>(v.getDroneTour());
		this.currentDroneTourLength = new HashMap<Integer, Double>(v.getCurrentDroneTourLength());
		this.currentDroneTourProfit = new HashMap<Integer, Double>(v.getCurrentDroneTourProfit());
	}

	public int getCapacity() {
		return capacity;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getCurrentCapacity() {
		return currentCapacity;
	}

	public void setCurrentCapacity(int currentCapacity) {
		this.currentCapacity = currentCapacity;
	}

	public double getTourLength() {
		return tourLength;
	}

	public void setTourLength(double tourLength) {
		this.tourLength = tourLength;
	}

	public ArrayList<Integer> getTour() {
		return tour;
	}

	public void setTour(ArrayList<Integer> tour) {
		this.tour = tour;
	}

	public HashMap<Integer, ArrayList<Integer>> getDroneTour() {
		return droneTour;
	}

	public void setDroneTour(HashMap<Integer, ArrayList<Integer>> droneTour) {
		this.droneTour = droneTour;
	}

	public HashMap<Integer, Double> getCurrentDroneTourLength() {
		return currentDroneTourLength;
	}

	public void setCurrentDroneTourLength(HashMap<Integer, Double> currentDroneTourLength) {
		this.currentDroneTourLength = currentDroneTourLength;
	}

	public HashMap<Integer, Double> getCurrentDroneTourProfit() {
		return currentDroneTourProfit;
	}

	public void setCurrentDroneTourProfit(HashMap<Integer, Double> currentDroneTourProfit) {
		this.currentDroneTourProfit = currentDroneTourProfit;
	}

	public void addNode(int selectedNode) {
		currentCapacity -= Main.graph.getNeededResource(selectedNode);
		tourLength += Main.graph.getNormalizedDistance(tour.get(tour.size() - 1), selectedNode);
		tour.add(selectedNode);

		droneTour.put(selectedNode, new ArrayList<Integer>());
		currentDroneTourLength.put(selectedNode, 0.);
		currentDroneTourProfit.put(selectedNode, 0.);

	}

	public void addExtraNode(Integer k, int selectedNode, double normalizedDistance, double profit) {
		tourLength += normalizedDistance;
		currentCapacity -= Main.graph.getNeededResource(selectedNode);

		droneTour.get(k).add(selectedNode);
		currentDroneTourLength.replace(k, currentDroneTourLength.get(k) + normalizedDistance);
		currentDroneTourProfit.replace(k, currentDroneTourProfit.get(k) + profit);
	}

	public void addDronePath(int newNode, ArrayList<Integer> droneTourPath) {
		for (Integer i : droneTourPath)
			addExtraNode(newNode, i, Main.graph.getNormalizedDistance(newNode, i), Main.graph.getProfit(i));

	}

	@Override
	public String toString() {
		return "\n (" + capacity + "," + currentCapacity + ")" + tour + " LENGTH:" + tourLength + " \n Drone Tour:"
				+ droneTour;
	}

	public void swap(int i, int j) {
		DurationParameters duration = Main.graph.getDuration(tour.get(i - 1), tour.get(i));
		tourLength -= duration.getExpectedDuration() * 0.75;
		tourLength -= duration.getVarianceDuration() * 0.25;

		duration = Main.graph.getDuration(tour.get(i), tour.get(i + 1));
		tourLength -= duration.getExpectedDuration() * 0.75;
		tourLength -= duration.getVarianceDuration() * 0.25;

		if (j != i + 1) {
			duration = Main.graph.getDuration(tour.get(j - 1), tour.get(j));
			tourLength -= duration.getExpectedDuration() * 0.75;
			tourLength -= duration.getVarianceDuration() * 0.25;
		}

		if (j < tour.size() - 1) {
			duration = Main.graph.getDuration(tour.get(j), tour.get(j + 1));
			tourLength -= duration.getExpectedDuration() * 0.75;
			tourLength -= duration.getVarianceDuration() * 0.25;
		}
		duration = Main.graph.getDuration(tour.get(i - 1), tour.get(j));
		tourLength += duration.getExpectedDuration() * 0.75;
		tourLength += duration.getVarianceDuration() * 0.25;

		if (j != i + 1) {
			duration = Main.graph.getDuration(tour.get(j), tour.get(i + 1));
			tourLength += duration.getExpectedDuration() * 0.75;
			tourLength += duration.getVarianceDuration() * 0.25;

			duration = Main.graph.getDuration(tour.get(j - 1), tour.get(i));
			tourLength += duration.getExpectedDuration() * 0.75;
			tourLength += duration.getVarianceDuration() * 0.25;
		} else {
			duration = Main.graph.getDuration(tour.get(j), tour.get(i));
			tourLength += duration.getExpectedDuration() * 0.75;
			tourLength += duration.getVarianceDuration() * 0.25;
		}

		if (j < tour.size() - 1) {
			duration = Main.graph.getDuration(tour.get(i), tour.get(j + 1));
			tourLength += duration.getExpectedDuration() * 0.75;
			tourLength += duration.getVarianceDuration() * 0.25;
		}

		int swap = tour.get(i);
		tour.set(i, tour.get(j));
		tour.set(j, swap);
	}

	public void removeNode(int i) {

		DurationParameters duration = Main.graph.getDuration(tour.get(tour.indexOf(i) - 1), tour.get(tour.indexOf(i)));
		tourLength -= duration.getExpectedDuration() * 0.75;
		tourLength -= duration.getVarianceDuration() * 0.25;

		if (tour.indexOf(i) < tour.size() - 1) {
			duration = Main.graph.getDuration(tour.get(tour.indexOf(i)), tour.get(tour.indexOf(i) + 1));
			tourLength -= duration.getExpectedDuration() * 0.75;
			tourLength -= duration.getVarianceDuration() * 0.25;

			duration = Main.graph.getDuration(tour.get(tour.indexOf(i) - 1), tour.get(tour.indexOf(i) + 1));
			tourLength += duration.getExpectedDuration() * 0.75;
			tourLength += duration.getVarianceDuration() * 0.25;
		}
		currentCapacity += Main.graph.getNeededResource(i);
		for (Integer visited : droneTour.get(i))
			currentCapacity += Main.graph.getNeededResource(visited);

		tour.remove(tour.indexOf(i));

		tourLength -= currentDroneTourLength.get(i); // rimuovo la durata del tour del drone in quel nodo dato che non
														// ci sarà più

		currentDroneTourLength.remove(i);
		currentDroneTourProfit.remove(i);
		droneTour.remove(i);
	}

	public void removeExtraNode(Integer k, int selectedNode, double normalizedDuration, double profit) {

		currentCapacity += Main.graph.getNeededResource(selectedNode);
		tourLength -= normalizedDuration;

		droneTour.get(k).remove(Integer.valueOf(selectedNode));
		currentDroneTourLength.replace(k, currentDroneTourLength.get(k) - normalizedDuration);
		currentDroneTourProfit.replace(k, currentDroneTourProfit.get(k) - profit);

	}
}
