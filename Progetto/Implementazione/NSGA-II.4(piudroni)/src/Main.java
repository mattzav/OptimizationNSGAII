import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import javax.management.RuntimeErrorException;

public class Main {
	static Random r = new Random();
	static int numObjective = 3;
	static boolean[] maxOrMinForThatObjective = new boolean[] { true, true, false };
	static int populationSize = 6 * 2; // numero pari
	static double minProfitNeeded = 300;
	static int numIteration = 300;
	static double mutationProb = 0.2;
	static double crossoverProb = 0.8;
	static double maxBatteryConsumption = 100;
	static int numNodesInTheGraph = 3;
	static int numExtraNodesForDroneInTheGraph = 100;

	static int numDronesPerVehicle = 5;

	static Graph graph;

	static ArrayList<Vehicle> vehicles;
	static int numMaxVehicle = 5;

	static HashMap<Integer, ArrayList<Integer>> reachableUsingDrone;

	static ArrayList<Integer> copyNodes;
	static ArrayList<Integer> copyNodesDrones;

	public static void main(String[] args) {
		copyNodes = new ArrayList<Integer>();
		reachableUsingDrone = new HashMap<Integer, ArrayList<Integer>>();
		graph = new Graph(numNodesInTheGraph + numExtraNodesForDroneInTheGraph);
		graph.print();

		initReachableUsingDrone();// inizializziamo i punti potenzialmente raggiungibili dai droni per ogni nodo
		createRandomVehicle(); // creiamo i veicoli a disposizione con capacit� random

		ArrayList<Individual> P = initPopulation();
		// System.out.println(P);

		ArrayList<ArrayList<Individual>> F = fast_non_dominated_sort(P);
		for (int front = 0; front < F.size(); front++) {
//			System.out.println("Nel front " + front + " ci sono " + F.get(front).size() + " soluzioni"); //
//			System.out.println(F.get(front));
			crowding_distance_assignment(F.get(front));
		}

		while (numIteration-- > 0) {
			if (numIteration % 100 == 0)
				System.out.println("Iter = " + numIteration);

			ArrayList<Individual> Q = generateChildren(P);

			ArrayList<Individual> union = new ArrayList<Individual>(Q);

			union.addAll(P);

			F = fast_non_dominated_sort(union);

			P = updatePopulation(F);

			// System.out.println(P.size());
		}

		F = fast_non_dominated_sort(P);

		for (int front = 0; front < F.size(); front++) {
			System.out.println(" Nel front " + front + " ci sono " + F.get(front).size() + " soluzioni");
			System.out.println(F.get(front));
		}

		checkSolution(P);

	}

	private static void checkSolution(ArrayList<Individual> p) {
		for (Individual ind : p)
			for (Vehicle v : ind.getGenotypeVehicle())
				for (Integer node : v.getTour()) {
					for (int i = 0; i < numDronesPerVehicle; i++) {
						double duration = 0;
						double consumption = 0;
						if (node == 0)
							continue;
						if (v.getDroneTour().get(node).get(i) != null)
							for (Integer drone : v.getDroneTour().get(node).get(i)) {
								duration += 2 * graph.getNormalizedDistance(node, drone);
								consumption += getConsumption(2 * graph.getNormalizedDistance(node, drone));
							}
						if (duration != v.getCurrentDroneTourLength().get(node).get(i) )
							throw new RuntimeException("error computing duration");
						if (consumption > maxBatteryConsumption)
							throw new RuntimeException("error battery consumed");
					}
				}
	}

	private static ArrayList<Individual> updatePopulation(ArrayList<ArrayList<Individual>> union) {
		ArrayList<Individual> toReturn = new ArrayList<Individual>();
		int index = 0;
		while (toReturn.size() + union.get(index).size() <= populationSize) {
			crowding_distance_assignment(union.get(index));
			toReturn.addAll(union.get(index));
			index++;
			if (index == union.size())
				break;
		}

		if (toReturn.size() == populationSize) {
			return toReturn;
		}

		crowding_distance_assignment(union.get(index));
		Collections.sort(union.get(index), new Comparator<Individual>() {
			@Override
			public int compare(Individual o1, Individual o2) {
				if (o1.getCrowdDistance() > o2.getCrowdDistance())
					return -1;
				if (o1.getCrowdDistance() == o2.getCrowdDistance())
					return 0;
				return 1;

			}
		});

		int i = 0;
		while (toReturn.size() < populationSize) {
			toReturn.add(union.get(index).get(i));
			i++;
		}

		return toReturn;
	}

	private static ArrayList<Individual> generateChildren(ArrayList<Individual> population) {
		ArrayList<Individual> children = new ArrayList<Individual>();

//		for (Individual i : population)
//			for (Vehicle v : i.getGenotypeVehicle())
//				v.checkCapacity();

		// System.out.println(population);
		while (children.size() < populationSize) {

			// System.out.println(population.size());
//			for (Individual i : population) {
//				System.out.println(i);
//				for (Vehicle v : i.getGenotypeVehicle())
//					v.checkCapacity();
//			}
			ArrayList<Individual> copy = new ArrayList<Individual>(population);

			Individual first = new Individual(copy.remove(r.nextInt(copy.size())).getGenotypeVehicle());
			Individual second = new Individual(copy.remove(r.nextInt(copy.size())).getGenotypeVehicle());
			Individual third = new Individual(copy.remove(r.nextInt(copy.size())).getGenotypeVehicle());
			Individual fourth = new Individual(copy.remove(r.nextInt(copy.size())).getGenotypeVehicle());

//			for (Vehicle v : first.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : second.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : third.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : fourth.getGenotypeVehicle())
//				v.checkCapacity();

			Individual winning_first = null, winning_second = null;

			if (first.getFront() < second.getFront()
					|| (first.getFront() == second.getFront() && first.getCrowdDistance() > second.getCrowdDistance()))
				winning_first = first;
			else
				winning_first = second;

			if (third.getFront() < fourth.getFront()
					|| (third.getFront() == fourth.getFront() && third.getCrowdDistance() > fourth.getCrowdDistance()))
				winning_second = third;
			else
				winning_second = fourth;

//			for (Vehicle v : winning_first.getGenotypeVehicle())
//				v.checkCapacity();
//
//			for (Vehicle v : winning_second.getGenotypeVehicle())
//				v.checkCapacity();

			// System.out.println("FIRST \n" + winning_first + "\n SECOND \n" +
			// winning_second);

			// just because the following crossover operator suppose that the vehicles in
			// winning_first are more or equal than the ones in winning_second,
			// we swap the two if this is not the case
			if (winning_first.getGenotypeVehicle().size() < winning_second.getGenotypeVehicle().size()) {
				Individual toSwap = new Individual(winning_first.getGenotypeVehicle());
				winning_first = new Individual(winning_second.getGenotypeVehicle());
				winning_second = new Individual(toSwap.getGenotypeVehicle());
			}

//			for (Vehicle v : winning_first.getGenotypeVehicle())
//				v.checkCapacity();
//
//			for (Vehicle v : winning_second.getGenotypeVehicle())
//				v.checkCapacity();

			int childNumberOfVehicle = (winning_first.getGenotypeVehicle().size()
					+ winning_second.getGenotypeVehicle().size()) / 2;

			ArrayList<Vehicle> currentIndividualVehicleFC = new ArrayList<Vehicle>();
			ArrayList<Vehicle> currentIndividualVehicleSC = new ArrayList<Vehicle>();
			int crossOverPoint = r.nextInt(childNumberOfVehicle) + 1;

			for (int i = 0; i < childNumberOfVehicle; i++) {
				if (i < crossOverPoint) {
					currentIndividualVehicleFC.add(new Vehicle(winning_first.getGenotypeVehicle().get(i)));
				} else {
					if (winning_second.getGenotypeVehicle().size() - 1 - (i - crossOverPoint) >= 0) {

						currentIndividualVehicleFC.add(new Vehicle(winning_second.getGenotypeVehicle()
								.get(winning_second.getGenotypeVehicle().size() - 1 - (i - crossOverPoint))));
					} else {

						currentIndividualVehicleFC.add(new Vehicle(
								winning_first.getGenotypeVehicle().get(winning_first.getGenotypeVehicle().size()
										+ (winning_second.getGenotypeVehicle().size() - 1 - (i - crossOverPoint)))));
					}

				}

				if (crossOverPoint + i < winning_first.getGenotypeVehicle().size()) {
					currentIndividualVehicleSC
							.add(new Vehicle(winning_first.getGenotypeVehicle().get(crossOverPoint + i)));

				} else {

					currentIndividualVehicleSC.add(new Vehicle(winning_second.getGenotypeVehicle()
							.get((crossOverPoint + i) % winning_first.getGenotypeVehicle().size())));
				}
			}

			Individual firstInd = new Individual(currentIndividualVehicleFC);
			Individual secondInd = new Individual(currentIndividualVehicleSC);

//			for (Vehicle v : firstInd.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : secondInd.getGenotypeVehicle())
//				v.checkCapacity();

			// System.out.println("AFTER CROSSOVER \n FIRST \n" + firstInd + "\n SECOND \n"
			// + secondInd);

			recoverFeasibility(firstInd);
			recoverFeasibility(secondInd);

//			for (Vehicle v : firstInd.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : secondInd.getGenotypeVehicle())
//				v.checkCapacity();

			// System.out.println("AFTER FEASIBILITY \n FIRST \n" + firstInd + "\n SECOND
			// \n" + secondInd);

			mutation(firstInd);
			mutation(secondInd);

//			for (Vehicle v : firstInd.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : secondInd.getGenotypeVehicle())
//				v.checkCapacity();

			// System.out.println("AFTER MUTATION \n FIRST \n" + firstInd + "\n SECOND \n" +
			// secondInd);

			postOptimize(firstInd);
			postOptimize(secondInd);

//			for (Vehicle v : firstInd.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : secondInd.getGenotypeVehicle())
//				v.checkCapacity();

			// System.out.println("AFTER OPTIMIZATION \n FIRST \n" + firstInd + "\n SECOND
			// \n" + secondInd);

			children.add(firstInd);
			children.add(secondInd);
//
//			for (Vehicle v : firstInd.getGenotypeVehicle())
//				v.checkCapacity();
//			for (Vehicle v : secondInd.getGenotypeVehicle())
//				v.checkCapacity();

		}
		return children;
	}

	private static void postOptimize(Individual s) {
		if (s.getGenotypeVehicle().size() == 1)
			return;

		// se un nodo � visitato pi� di una volta, lo lascio solo nel veicolo dove ha la
		// rotta per il drone pi� lunga
		for (int i = 1; i < numNodesInTheGraph; i++) {
			if (s.getVisited().contains(i)) {

				double mostProfitableTour = Double.MIN_VALUE;
				int maxTourVehicleIndex = -1;
				int howMany = 0;

				for (int j = 0; j < s.getGenotypeVehicle().size(); j++) {
					if (s.getGenotypeVehicle().get(j).getTour().contains(i)) {
						howMany++;
						double profit = 0;
						for (int drone = 0; drone < numDronesPerVehicle; drone++)
							profit += s.getGenotypeVehicle().get(j).getCurrentDroneTourProfit().get(i).get(drone);
						if (profit > mostProfitableTour) {
							mostProfitableTour = profit;
							maxTourVehicleIndex = j;
						}
					}
				}

				if (howMany >= 2) {
					for (int j = 0; j < s.getGenotypeVehicle().size(); j++) {
						if (j != maxTourVehicleIndex && s.getGenotypeVehicle().get(j).getTour().contains(i)) {
							s.getGenotypeVehicle().get(j).removeNode(i);
						}
					}
				}
			}
		}
//		System.out.println("BEFORE \n" + s.getGenotypeVehicle() + "\n END");
		Collections.sort(s.getGenotypeVehicle(), new Comparator<Vehicle>() {
			@Override
			public int compare(Vehicle o1, Vehicle o2) {
				if (o1.getTourLength() > o2.getTourLength())
					return 1;
				if (o1.getTourLength() == o2.getTourLength())
					return 0;

				return -1;
			}
		});

		boolean found = true;
		while (found) {
//			System.out.println("UPDATED \n " + s.getGenotypeVehicle());
			found = false;
			// controllare che il tour non sia vuoto
			Vehicle busiest = s.getGenotypeVehicle().get(s.getGenotypeVehicle().size() - 1);
			if (busiest.getTour().size() <= 1)
				break;
			int toAssign = busiest.getTour().get(1); // si potrebbe fare un for per ogni toAssing
			double droneTour = busiest.getLongestDroneTourForNode().get(toAssign);

//			System.out.println(busiest.getCurrentDroneTourLength());
			HashMap<Integer, ArrayList<Integer>> droneTourPath = busiest.getDroneTour().get(toAssign);
			double resourceNeeded = 0;
			for (int drone = 0; drone < numDronesPerVehicle; drone++)
				if (droneTourPath.get(drone) != null)
					for (Integer i : droneTourPath.get(drone))
						resourceNeeded += graph.getNeededResource(i);

			for (int index = 0; index < s.getGenotypeVehicle().size() - 1; index++) {
				Vehicle v = s.getGenotypeVehicle().get(index);
				if (v.getCurrentCapacity() >= graph.getNeededResource(toAssign) + resourceNeeded && v.getTourLength()
						+ graph.getNormalizedDistance(v.getTour().get(v.getTour().size() - 1), toAssign)
						+ droneTour < busiest.getTourLength()) {

					Vehicle avoidLoop = new Vehicle(busiest);
					avoidLoop.removeNode(toAssign);
					if (avoidLoop.getTourLength() >= busiest.getTourLength())
						continue;

//					System.out.println("freest " + v);
//					System.out.println("busiest " + busiest);
//					System.out.println("I'M ADDING " + toAssign + " in " + v);

					v.addNode(toAssign);
					for (int drone = 0; drone < numDronesPerVehicle; drone++)
						v.addDronePath(toAssign, drone, droneTourPath.get(drone));
//					System.out.println("obtaining " + toAssign + " in " + v);
					busiest.removeNode(toAssign);
					found = true;

					int iter = index + 1;
					while (iter < s.getGenotypeVehicle().size()
							&& s.getGenotypeVehicle().get(iter).getTourLength() <= v.getTourLength())
						iter++;

					if (iter != index + 1) {
						for (int i = index; i < iter - 1; i++) {
							Vehicle swap = s.getGenotypeVehicle().get(i);
							s.getGenotypeVehicle().set(i, s.getGenotypeVehicle().get(i + 1));
							s.getGenotypeVehicle().set(i + 1, swap);
						}
					}

					// usato perch� se devo sostituirlo con l'ultimo, arriver� al secondo while con
					// l'elemento non pi� ultimo ma penultimo
					int stride = 0;
					if (iter == s.getGenotypeVehicle().size()) {
						stride = 1;
						busiest = s.getGenotypeVehicle().get(s.getGenotypeVehicle().size() - 2);
					}

					iter = s.getGenotypeVehicle().size() - 2;

					while (iter - stride >= 0
							&& s.getGenotypeVehicle().get(iter - stride).getTourLength() > busiest.getTourLength())
						iter--;
					if (iter != s.getGenotypeVehicle().size() - 2) {
						for (int i = s.getGenotypeVehicle().size() - 1 - stride; i > iter - stride + 1; i--) {
							Vehicle swap = s.getGenotypeVehicle().get(i);
							s.getGenotypeVehicle().set(i, s.getGenotypeVehicle().get(i - 1));
							s.getGenotypeVehicle().set(i - 1, swap);
						}
					}

					break;

				}
			}
		}

//		System.out.println("AFTER \n" + s.getGenotypeVehicle() + "\n END");
		s.updateSolution(null);

	}

	private static void mutation(Individual individual) {

		// change position i with position j with probability equal to mutationProb
		for (Vehicle vehicle : individual.getGenotypeVehicle()) {
			for (int i = 1; i < vehicle.getTour().size() - 1; i++) {
				for (int j = i + 1; j < vehicle.getTour().size(); j++) {
					if (r.nextDouble() <= mutationProb) {
						vehicle.swap(i, j);

					}

				}
			}
		}

		// add node with probability mutationProb
		for (int i = 0; i < numNodesInTheGraph; i++) {
			if (r.nextDouble() > mutationProb)
				continue;
			int extractedRandomNode = copyNodes.get(r.nextInt(copyNodes.size()));
			if (individual.getVisited().contains(extractedRandomNode))
				continue;
			Vehicle extractedRandomVehicle = individual.getGenotypeVehicle()
					.get(r.nextInt(individual.getGenotypeVehicle().size()));
			if (extractedRandomVehicle.getCurrentCapacity() - graph.getNeededResource(extractedRandomNode) >= 0
					&& !extractedRandomVehicle.getTour().contains(extractedRandomNode)) {
				extractedRandomVehicle.addNode(extractedRandomNode);
			}
		}

		individual.updateSolution(null);

		double currentProfit = individual.getObjectiveValue()[2];
		// remove node with probability mutationProb
		for (int i = 0; i < numNodesInTheGraph; i++) {
			if (r.nextDouble() > mutationProb)
				continue;
			Vehicle extractedRandomVehicle = individual.getGenotypeVehicle()
					.get(r.nextInt(individual.getGenotypeVehicle().size()));

			if (extractedRandomVehicle.getTour().size() <= 1)
				continue;

			int extractedRandomNode = extractedRandomVehicle.getTour()
					.get(r.nextInt(extractedRandomVehicle.getTour().size() - 1) + 1);

			double droneProfit = 0;
			for (int drone = 0; drone < numDronesPerVehicle; drone++)
				droneProfit += extractedRandomVehicle.getCurrentDroneTourProfit().get(extractedRandomNode).get(drone);

			if (currentProfit - graph.getProfit(extractedRandomNode) - droneProfit >= minProfitNeeded) {
				currentProfit -= graph.getProfit(extractedRandomNode);
				currentProfit -= droneProfit;
				extractedRandomVehicle.removeNode(extractedRandomNode); // il profitto andrebbe sottrato in removeNode e
																		// non qui
			}
		}

		individual.updateSolution(null);

		boolean checked[] = new boolean[numExtraNodesForDroneInTheGraph];

		// add one drone node random
		for (int i = numNodesInTheGraph; i < numNodesInTheGraph + numExtraNodesForDroneInTheGraph; i++) {
			if (r.nextDouble() > mutationProb)
				continue;
			int extractedRandomNode = copyNodesDrones.get(r.nextInt(copyNodesDrones.size()));

			if (checked[extractedRandomNode - numNodesInTheGraph])
				continue;
			else
				checked[extractedRandomNode - numNodesInTheGraph] = true;

			if (individual.getVisited().contains(extractedRandomNode))
				continue;

			Vehicle extractedRandomVehicle = individual.getGenotypeVehicle()
					.get(r.nextInt(individual.getGenotypeVehicle().size()));
			if (extractedRandomVehicle.getTour().size() <= 1)
				continue;
			int extractedRandomNodeForAdding = extractedRandomVehicle.getTour()
					.get(r.nextInt(extractedRandomVehicle.getTour().size() - 1) + 1);
			int randomDrone = r.nextInt(numDronesPerVehicle);

			if (getConsumption(extractedRandomVehicle.getCurrentDroneTourLength().get(extractedRandomNodeForAdding).get(randomDrone))
					+ getConsumption(2 * graph.getNormalizedDistance(extractedRandomNodeForAdding,
							extractedRandomNode)) <= maxBatteryConsumption
					&& extractedRandomVehicle.getCurrentCapacity() >= Main.graph
							.getNeededResource(extractedRandomNode)) {
				extractedRandomVehicle.addExtraNode(extractedRandomNodeForAdding, extractedRandomNode, randomDrone,

						2 * graph.getNormalizedDistance(extractedRandomNodeForAdding, extractedRandomNode),
						graph.getProfit(extractedRandomNode));
			}

		}

		individual.updateSolution(null);

		currentProfit = individual.getObjectiveValue()[2];
		// remove droneNode with probability mutationProb
		for (int i = numNodesInTheGraph; i < numNodesInTheGraph + numExtraNodesForDroneInTheGraph; i++) {
			if (r.nextDouble() > mutationProb)
				continue;
			Vehicle extractedRandomVehicle = individual.getGenotypeVehicle()
					.get(r.nextInt(individual.getGenotypeVehicle().size()));

			if (extractedRandomVehicle.getTour().size() <= 1)
				continue;

			int extractedRandomNode = extractedRandomVehicle.getTour()
					.get(r.nextInt(extractedRandomVehicle.getTour().size() - 1) + 1);
			int extractedRandomDrone = r.nextInt(numDronesPerVehicle);

			ArrayList<Integer> droneTourSelected = extractedRandomVehicle.getDroneTour().get(extractedRandomNode)
					.get(extractedRandomDrone);

			if (droneTourSelected == null || droneTourSelected.size() == 0)
				continue;

			int extractedDroneNode = droneTourSelected.get(r.nextInt(droneTourSelected.size()));

			if (currentProfit - graph.getProfit(extractedDroneNode) >= minProfitNeeded) {
				extractedRandomVehicle.removeExtraNode(extractedRandomNode, extractedRandomDrone, extractedDroneNode,
						2 * graph.getNormalizedDistance(extractedRandomNode, extractedDroneNode),
						graph.getProfit(extractedDroneNode));
				currentProfit -= graph.getProfit(extractedDroneNode);
			}
		}
		individual.updateSolution(null);

	}

	// consumo in funzione della distanza d
	public static Double getConsumption(double d) {
		return 2*d;
	}

	private static void crowding_distance_assignment(ArrayList<Individual> front_i_th) {
		for (Individual i : front_i_th) {
			i.setCrowdDistance(0);
		}

		for (int i = 0; i < numObjective; i++) {
			Collections.sort(front_i_th, new ObjectiveComparator(i));
			double normalization = front_i_th.get(front_i_th.size() - 1).getObjectiveValue()[i]
					- front_i_th.get(0).getObjectiveValue()[i];

			front_i_th.get(0).setCrowdDistance(Double.MAX_VALUE);
			front_i_th.get(front_i_th.size() - 1).setCrowdDistance(Double.MAX_VALUE);
			for (int index = 1; index < front_i_th.size() - 1; index++) {
				Individual current = front_i_th.get(index);
				current.setCrowdDistance(current.getCrowdDistance() + ((front_i_th.get(index + 1).getObjectiveValue()[i]
						- front_i_th.get(index - 1).getObjectiveValue()[i])) / normalization);
			}
		}
	}

	private static ArrayList<ArrayList<Individual>> fast_non_dominated_sort(ArrayList<Individual> population) {
		ArrayList<ArrayList<Individual>> toReturn = new ArrayList<ArrayList<Individual>>();
		ArrayList<Individual> F1 = new ArrayList<Individual>();

		for (Individual p : population) {
			ArrayList<Individual> dominated = new ArrayList<Individual>();
			int dominatedBy = 0;
			for (Individual q : population) {
				if (isDominatedBy(q, p))
					dominated.add(q);
				else if (isDominatedBy(p, q))
					dominatedBy++;
			}
			p.setDominatedBy(dominatedBy);
			if (dominatedBy == 0) {
				p.setFront(0);
				F1.add(p);
			}

			p.setDominated(dominated);
		}
		toReturn.add(F1);

		int i = 0;
		ArrayList<Individual> current = toReturn.get(i);
		while (!current.isEmpty()) {
			ArrayList<Individual> Q = new ArrayList<Individual>();
			for (Individual p : current)
				for (Individual q : p.getDominated()) {
					q.setDominatedBy(q.getDominatedBy() - 1);
					if (q.getDominatedBy() == 0) {
						q.setFront(i + 1);
						Q.add(q);
					}
				}
			i += 1;
			current = Q;
			if (Q.size() != 0)
				toReturn.add(Q);
		}

		return toReturn;
	}

	private static boolean isDominatedBy(Individual first, Individual second) {
		boolean atLeastOne = false;
		for (int i = 0; i < numObjective; i++)
			if (maxOrMinForThatObjective[i]) // we want to minimize that objective function
			{
				if (first.getObjectiveValue()[i] > second.getObjectiveValue()[i])
					atLeastOne = true;
				else if (first.getObjectiveValue()[i] < second.getObjectiveValue()[i])
					return false;
			} else {
				if (first.getObjectiveValue()[i] < second.getObjectiveValue()[i])
					atLeastOne = true;
				else if (first.getObjectiveValue()[i] > second.getObjectiveValue()[i])
					return false;
			}
		return atLeastOne;
	}

	private static ArrayList<Individual> initPopulation() {
		ArrayList<Individual> toReturn = new ArrayList<Individual>();
		for (int i = 0; i < populationSize; i++) {

			// creo una lista con valori da 0 alla size dei veicoli disponibili
			ArrayList<Integer> vehiclesIndexAvailable = new ArrayList<Integer>();
			for (int j = 0; j < numMaxVehicle; j++)
				vehiclesIndexAvailable.add(j);

			// decidiamo il numero di veicoli da usare nella soluzione in modo random
			int numVehicle = r.nextInt(numMaxVehicle) + 1;

			ArrayList<Vehicle> vehiclesToUse = new ArrayList<Vehicle>();
			for (int j = 0; j < numVehicle; j++) {
				Vehicle choosen = vehicles
						.get(vehiclesIndexAvailable.remove(r.nextInt(vehiclesIndexAvailable.size())));
				vehiclesToUse.add(new Vehicle(choosen.getCapacity()));
			}

			ArrayList<Integer> nodesInTheGraph = new ArrayList<Integer>();
			ArrayList<Integer> nodesForTheDrones = new ArrayList<Integer>();

			for (int j = 1; j < numNodesInTheGraph; j++)
				nodesInTheGraph.add(j);

			for (int j = numNodesInTheGraph; j < numNodesInTheGraph + numExtraNodesForDroneInTheGraph; j++)
				nodesForTheDrones.add(j);

			if (i == 0) {
				initCopyNodes(nodesInTheGraph);
				copyNodesDrones = new ArrayList<Integer>(nodesForTheDrones); // NON SONO ORDINATI IN BASE AL PROFITTO
			}

			// con probabilit� pari a 0.8 assegniamo un nodo ad un veicolo random
			while (!nodesInTheGraph.isEmpty()) {
				int selectedNode = nodesInTheGraph.remove(r.nextInt(nodesInTheGraph.size()));

				if (r.nextDouble() <= crossoverProb) {
					int selectedVehicle = r.nextInt(numVehicle);
					if (vehiclesToUse.get(selectedVehicle).getCurrentCapacity() >= graph
							.getNeededResource(selectedNode)) {
						vehiclesToUse.get(selectedVehicle).addNode(selectedNode);
					}
				}
			}

			HashMap<Pair, HashMap<Integer, Double>> energyConsumed = new HashMap<Pair, HashMap<Integer, Double>>();

			// add node for drone for random vehicles and random node as starting point
			for (int j = 0; j < numExtraNodesForDroneInTheGraph; j++) {
				int randomIndex = r.nextInt(numVehicle); // veicolo scelto random
				int sizeOfItsPath = vehiclesToUse.get(randomIndex).getTour().size();
				if (sizeOfItsPath <= 1)
					continue;
				int nodeRandom = vehiclesToUse.get(randomIndex).getTour().get(r.nextInt(sizeOfItsPath - 1) + 1);

				if (reachableUsingDrone.get(nodeRandom).size() == 0)
					continue;

				if (energyConsumed.get(new Pair(randomIndex, nodeRandom)) == null)
					energyConsumed.put(new Pair(randomIndex, nodeRandom), new HashMap<Integer, Double>());
				{
					for (int drone = 0; drone < numDronesPerVehicle; drone++)
						if (energyConsumed.get(new Pair(randomIndex, nodeRandom)).get(drone) == null)
							energyConsumed.get(new Pair(randomIndex, nodeRandom)).put(drone, 0.);
				}

				int destination = reachableUsingDrone.get(nodeRandom)
						.get(r.nextInt(reachableUsingDrone.get(nodeRandom).size()));
				int droneRandom = r.nextInt(numDronesPerVehicle);

				// scegliamo un indice di partenza random e usiamo il modulo per scorrere in
				// modo ciclico la lista (da i a n e da 0 a i-1)
				if (getConsumption(2 * graph.getNormalizedDistance(nodeRandom, destination))
						+ energyConsumed.get(new Pair(randomIndex, nodeRandom))
								.get(droneRandom) <= maxBatteryConsumption
						&& nodesForTheDrones.contains(destination) && vehiclesToUse.get(randomIndex)
								.getCurrentCapacity() >= graph.getNeededResource(destination)) {
					nodesForTheDrones.remove(Integer.valueOf(destination));
					vehiclesToUse.get(randomIndex).addExtraNode(nodeRandom, destination, droneRandom,
							2 * graph.getNormalizedDistance(nodeRandom, destination),
							graph.getProfit(destination));
					energyConsumed.get(new Pair(randomIndex, nodeRandom)).replace(droneRandom,
							energyConsumed.get(new Pair(randomIndex, nodeRandom)).get(droneRandom)
									+ getConsumption(2 * graph.getNormalizedDistance(nodeRandom, destination)));
				}

			}

			Individual toAdd = new Individual(vehiclesToUse);
			recoverFeasibility(toAdd);

			toReturn.add(toAdd);
		}

		return toReturn;

	}

	private static void recoverFeasibility(Individual toAdd) {

		if (toAdd.getObjectiveValue()[2] > minProfitNeeded)
			return;
		// sort vehicles depending on the length of the tour and assign,
		// until the feasibility is reached, the most profitable node to the most free
		// vehicle

		Collections.sort(toAdd.getGenotypeVehicle(), new Comparator<Vehicle>() {
			@Override
			public int compare(Vehicle o1, Vehicle o2) {
				if (o1.getTourLength() < o2.getTourLength())
					return -1;
				else if (o1.getTourLength() == o2.getTourLength())
					return 0;
				return 1;
			}
		});

		// provo ad aggiungere solo nodi del grafo e non nodi per i droni, aggiungere.
		double added = 0;
		for (int indexNode = 1; indexNode < numNodesInTheGraph
				&& toAdd.getObjectiveValue()[2] + added < minProfitNeeded; indexNode++) {
			if (toAdd.getVisited().contains(indexNode))
				continue;

			for (int j = 0; j < toAdd.getGenotypeVehicle().size(); j++) {
				if (toAdd.getGenotypeVehicle().get(j).getCurrentCapacity() >= graph.getNeededResource(indexNode)) {

					toAdd.getGenotypeVehicle().get(j).addNode(indexNode);
					added += graph.getProfit(indexNode);
					break;
				}

			}

		}
		toAdd.updateSolution(null);

		added = 0;
		for (int i = numNodesInTheGraph; i < numNodesInTheGraph + numExtraNodesForDroneInTheGraph
				&& toAdd.getObjectiveValue()[2] + added < minProfitNeeded; i++) {

			if (toAdd.getVisited().contains(i))
				continue;
			else {
				// debug
				for (int a = 0; a < toAdd.getGenotypeVehicle().size(); a++)
					for (Integer b : toAdd.getGenotypeVehicle().get(a).getTour())
						for (int d = 0; d < numDronesPerVehicle; d++) {
							HashMap<Integer, ArrayList<Integer>> check = toAdd.getGenotypeVehicle().get(a)
									.getDroneTour().get(b);
							if (check != null) {
								ArrayList<Integer> checkb = check.get(d);
								if (checkb != null && checkb.contains(i))
									throw new RuntimeException("errore");
							}
						}
				// end debug
			}
			boolean inserted = false;
			int startingrandom = r.nextInt(toAdd.getGenotypeVehicle().size());
			for (int j = startingrandom; j < startingrandom + toAdd.getGenotypeVehicle().size() && !inserted; j++) {
				Vehicle vehicle = toAdd.getGenotypeVehicle().get(j % toAdd.getGenotypeVehicle().size());
				if (vehicle.getTour().size() <= 1)
					continue;
				for (Integer nodeForAdding : vehicle.getTour()) {
					if (nodeForAdding == 0)
						continue;
					int droneRandomStart = r.nextInt(numDronesPerVehicle);
					for (int index = droneRandomStart; index < droneRandomStart + numDronesPerVehicle; index++)
						if (getConsumption(vehicle.getCurrentDroneTourLength().get(nodeForAdding).get(index % numDronesPerVehicle))
								+ getConsumption(
										2 * graph.getNormalizedDistance(nodeForAdding, i)) <= maxBatteryConsumption
								&& vehicle.getCurrentCapacity() >= Main.graph.getNeededResource(i)) {
							vehicle.addExtraNode(nodeForAdding, i, index % numDronesPerVehicle,
									2 * graph.getNormalizedDistance(nodeForAdding, i),
									graph.getProfit(i));
							inserted = true;
							added += Main.graph.getProfit(i);
							break;
						}
					if (inserted)
						break;
				}
			}
		}

		toAdd.updateSolution(null);

	}

	private static void initCopyNodes(ArrayList<Integer> nodesInTheGraph) {
		copyNodes = new ArrayList<Integer>(nodesInTheGraph);

		// ordiniamo i nodi in base al profitto
		Collections.sort(copyNodes, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if (graph.getProfit(o1) / graph.getNeededResource(o1) > graph.getProfit(o2)
						/ graph.getNeededResource(o2))
					return -1;
				if (graph.getProfit(o1) / graph.getNeededResource(o1) < graph.getProfit(o2)
						/ graph.getNeededResource(o2))
					return 1;

				return 0;
			}
		});
	}

	private static void createRandomVehicle() {
		vehicles = new ArrayList<Vehicle>();
		for (int i = 0; i < numMaxVehicle; i++) {
			boolean capacity = r.nextBoolean();
			if (capacity)
				vehicles.add(new Vehicle(100));
			else
				vehicles.add(new Vehicle(200));
		}
	}

	private static void initReachableUsingDrone() {
		// per ogni nodo del grafo iniziale (0,..,numNodesInTheGraph) calcolo quali
		// punti extra riesco a raggiungere
		for (int i = 0; i < numNodesInTheGraph; i++) {
			reachableUsingDrone.put(i, new ArrayList<Integer>());
			for (int j = numNodesInTheGraph; j < numNodesInTheGraph + numExtraNodesForDroneInTheGraph; j++) {
				if (getConsumption(2 * graph.getNormalizedDistance(i, j)) <= maxBatteryConsumption) {
					reachableUsingDrone.get(i).add(j);
				}
			}
		}

	}
}
