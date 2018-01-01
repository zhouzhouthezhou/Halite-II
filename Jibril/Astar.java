import java.util.*;
import java.util.Map.Entry;

import hlt.*;

public class Astar {
	
	public static ThrustMove navigate(Ship start, Entity target, GameMap gameMap) {
		Log.log("Starting Astar");
		ArrayList<Node> closed = new ArrayList<>();
		
		ArrayList<Node> open = new ArrayList<>();
		open.add(new Node(start.getXPos(), start.getYPos()));
		
		HashMap<Node, Node> parents = new HashMap<>();
		parents.put(open.get(0), null);
		
		HashMap<Node, Double> g = new HashMap<>();
		g.put(open.get(0), 0.);
		
		HashMap<Node, Double> h = new HashMap<>();
		h.put(open.get(0), heuristicCost(open.get(0), target));
		
		HashMap<Double, Node> f = new HashMap<>();
		f.put(g.get(open.get(0)) + h.get(open.get(0)), open.get(0));
		
		boolean first = true;
		while(!open.isEmpty()) {
			TreeMap<Double, Node> sortedF = new TreeMap<>(f);
			Set<Entry<Double, Node>> finalF = sortedF.entrySet();
			Entry<Double, Node> current = finalF.iterator().next();
			
			Log.log("Current node: " + current.getValue().toString());
			
			if(isOccupied(current.getValue(), target)) 
				return reconstructPath(current.getValue(), parents, gameMap, start);
			
			boolean skip = false;
			
			if(!first) {
				for(Planet planet: gameMap.getAllPlanets().values()) {
					if(isOccupied(current.getValue(), planet)) {
						skip = true;
						break;
					}
				}
				
				for(Ship ship: gameMap.getMyPlayer().getShips().values()) {
					if(isOccupied(current.getValue(), ship)) {
						skip = true;
						break;
					}
				}
			}
			
			if(skip) {
				Log.log("Skipping node " + current.getValue().toString());
				continue;
			}
			
			open.remove(current.getValue());
			f.remove(current.getKey(), current.getValue());
			closed.add(current.getValue());
			
			for(Node neighbor: findNeighbors(current.getValue())) {
				
				boolean inClosed = false;
				for(Node node: closed) {
					if (neighbor.getX() == node.getX() && neighbor.getY() == node.getY()) {
						inClosed = true;
						break;
					}
				}
				
				if(inClosed)
					continue;
				
				boolean inOpen = false;
				for(Node node: open) {
					if (neighbor.getX() == node.getX() && neighbor.getY() == node.getY()) {
						inOpen = true;
						break;
					}
				}
				
				if(!inOpen) {
					open.add(neighbor);
					g.put(neighbor, g.get(current.getValue()) + calculateGScore(neighbor));
					f.put(g.get(neighbor) + heuristicCost(neighbor, target), neighbor);
					parents.put(neighbor, current.getValue());
				}
				
			}
		}
		return null;
	}
	
	public static double calculateGScore(Node node) {
		Log.log("Calculating g score for node " + node.toString());
		return (node.getIsDiagonal()) ? Math.sqrt(2) : 1;
	}
	
	public static double heuristicCost(Node node, Entity target) {
		Log.log("Calculating heuristic cost for node " + node.toString());
		return ((Math.abs(node.getX() - target.getXPos())) + (Math.abs(node.getY() - target.getYPos()))) - (0.5 + target.getRadius());
	}
	
	
	public static boolean isOccupied(Node node, Entity center) {
		double circle = Math.sqrt(Math.pow(center.getRadius() + 0.5, 2) - Math.pow((node.getX() - center.getXPos()), 2)) + Math.pow(center.getYPos(), 2);
		return ( node.getY() < circle && node.getY() > -1 * circle ) ? true:false;
	}
	
	public static ArrayList<Node> findNeighbors(Node node){
		Log.log("Finding neighbors for node " + node.toString());
		ArrayList<Node> neighbors = new ArrayList<>();
		for(double y = node.getY() - 1; y < node.getY() + 1; y++) {
			for(double x = node.getX() - 1; x < node.getX() + 1; x++) {
				if(node.getX() == x && node.getY() == y)
					continue;
				Node tempNode = new Node(x, y);
				if(x != node.getX() && y != node.getY())
					tempNode.setIsDiagonal(true);
				neighbors.add(tempNode);
			}
		}
		return neighbors;
	}
	
	
	public static ThrustMove reconstructPath(Node node, HashMap<Node, Node> parents, GameMap gameMap, Ship start) {
		Log.log("Reconstrucing path from node " + node.toString());
		if(parents.get(node) == null) {
			return Navigation.navigateShipTowardsTarget(gameMap, start, new Position(node.getX(), node.getY()), Constants.MAX_SPEED, false, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0);
		}else {
			reconstructPath(parents.get(node), parents, gameMap, start);
		}
		return null;
	}
}
