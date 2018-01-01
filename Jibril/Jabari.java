import hlt.*;

import java.util.*;
import java.util.Map.Entry;


public class Jabari {

	@SuppressWarnings("null")
	public static void main(final String args[]) {
		final Networking networking = new Networking();
		final GameMap gameMap = networking.initialize("Jabari v2");

		// We now have 1 full minute to analyze the initial map.
		final String initialMapIntelligence = "width: " + gameMap.getWidth() + "; height: " + gameMap.getHeight()
				+ "; players: " + gameMap.getAllPlayers().size() + "; planets: " + gameMap.getAllPlanets().size();
		Log.log(initialMapIntelligence);

		final ArrayList<Move> moveList = new ArrayList<>();
		boolean firstTurn = true;
		boolean odd = true;
		
		//Game Start
		for (;;) {
			moveList.clear();
			networking.updateMap(gameMap);
			
			for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
				Log.log("***********New Ship**********");
				
				//Planet the ship will try to travel to
				Planet targetPlanet = null;
				
				//Ignore docked ships
				if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
					continue;
				}
				
				//Shitty optimization
				//Only increment through half the ships per turn
//				if (odd && ship.getId() % 2 == 1) {
//					continue;
//				}else if(!odd && ship.getId() % 2 == 0) {
//					continue;
//				}
				
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
		        //TODO implement A*
		        //TODO kamakazi??
		        //TODO air force??
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
//			odd = !odd;
			Networking.sendMoves(moveList);
		}
	}
	
	public static void miner() {
		
	}
	
	public static void airforce() {
		
	}
}
