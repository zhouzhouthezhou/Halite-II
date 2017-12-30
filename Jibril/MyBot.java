import hlt.*;
import hlt.Ship.Role;

import java.util.*;
import java.util.Map.Entry;


public class MyBot {
	
	public static Networking networking = new Networking();
	public static GameMap gameMap = networking.initialize("Jibril_v0.1");
	public static ArrayList<Move> moveList = new ArrayList<>();
	
	public static void main (final String args[]) {;
		
		for(;;) {
			moveList.clear();
			networking.updateMap(gameMap);
			
			for (Ship ship : gameMap.getMyPlayer().getShips().values()) {
				Log.log("********New Ship**********");
				turn(ship);
			}
			
			Networking.sendMoves(moveList);
		}
	}
	
	public static void turn(Ship ship) {
		switch(ship.getRole()) {
		case Worker:
			Log.log("Itterating Worker: ID" + ship.getId());
			worker(ship);
			break;
		case Soldier:
			Log.log("Itterating Soldier: ID" + ship.getId());
			soldier();
			break;
		default:
			Log.log("Ship ID" + ship.getId() + " has not been assigned a role. Attempting role desagnation");
			assign(ship);
			Log.log("Re-Itterating");
			turn(ship);
			break;
		}
	}
	
	public static void worker(Ship ship) {
		//Planet the ship will try to travel to
		Planet targetPlanet = null;
		
		//Ignore docked ships
		if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
			return;
		}
		
		//K Nearest Neighbors Algorithm
		HashMap<Double,Planet> planetData = new HashMap<>();
		
		for (final Planet planet : gameMap.getAllPlanets().values()) {
			planetData.put(ship.getDistanceTo(planet), planet);
		}
		
		TreeMap<Double, Planet> sortedPlanetData = new TreeMap<>(planetData);
        Set<Entry<Double, Planet>> finalPlanetData = sortedPlanetData.entrySet();
		
        //Set targetPlanet based off K Nearest Neighbors
        for(Entry<Double, Planet> data : finalPlanetData) {
        	Planet tempPlanet = data.getValue();
        	if(tempPlanet.getDockedShips().size() <= tempPlanet.getDockingSpots() / 4 || !tempPlanet.isOwned() || tempPlanet.getDockedShips().size() <= tempPlanet.getDockingSpots() / 2 || !tempPlanet.isFull()) {
        		targetPlanet = data.getValue();
        		break;
        	} else {
        		continue;
        	}
        }
        
        //When all planets are occupied and filled, start attack!
        if(targetPlanet == null) {
	        for(Entry<Double, Planet> data : finalPlanetData) {
	        	Planet tempPlanet = data.getValue();
	        	if(tempPlanet.getOwner() != gameMap.getMyPlayerId()) {
	        		targetPlanet = data.getValue();
	        		break;
	        	} else {
	        		continue;
	        	}
	        }
        }
        
        //Null pointers can go kill themselves and this is really cringe worthy code but you know what its 3am and I don't care anymore
        if(targetPlanet == null) {
        	for(Planet tempPlanet : gameMap.getAllPlanets().values()){
        		targetPlanet = tempPlanet;
        		break;
        	}
        }
        
        //Control Algorithm
        if(ship.canDock(targetPlanet)) {
        	if((targetPlanet.isOwned() && targetPlanet.getOwner() != gameMap.getMyPlayerId()) || targetPlanet.isFull()) {
        		//ATTACK
                Log.log("Scooting");
                Ship targetShip = gameMap.getShip(targetPlanet.getOwner(), targetPlanet.getDockedShips().get(0));
        		final ThrustMove scoot = Navigation.navigateShipTowardsTarget(gameMap, ship, new Position(targetShip.getXPos(), targetShip.getYPos()), Constants.MAX_SPEED, true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0);
        		if (scoot != null) {
        			moveList.add(scoot);
        		}
        	}else{
        		//Dock on to new planet / populate existing planets
        		moveList.add(new DockMove(ship, targetPlanet));
        	}
        }else{
        	//Default travel stuff
        	final ThrustMove navigate = Navigation.navigateShipToDock(gameMap, ship, targetPlanet, Constants.MAX_SPEED);
			if (navigate != null) {
				moveList.add(navigate);
			}
        }
	}
	
	public static void soldier() {
		
	}
	
	public static void attack() {
		
	}
	
	public static void defence() {
		
	}
	
	public static void assign(Ship ship) {
		ship.setRole(Role.Worker);
		Log.log("Ship ID" + ship.getId() + " has been desegnated as a Worker");
	}
}
