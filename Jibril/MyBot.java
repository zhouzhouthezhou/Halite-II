import hlt.*;

import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("null")
public class MyBot {
	//test
	public static Networking networking = new Networking();
	public static GameMap gameMap = networking.initialize("Jibril_v0.9");
	public static ArrayList<Move> moveList = new ArrayList<>();
	
	private static boolean isFirstTurn = true;

	private static ArrayList<Integer> workers = new ArrayList<>();
	private static int workerNum = 4;
	private static int workerCount = 0;
	
	private static int soldierNum = 1;
	private static int soldierCount = 0;

	private static ArrayList<Integer> attackers = new ArrayList<>();
	private static int attackNum = 1;
	private static int attackCount = 0;
	
	private static ArrayList<Integer> defenders = new ArrayList<>();
	private static int defendNum = 1;
	private static int defendCount = 0;

	private static int lastAssignedShip;
	
	public static void main(final String args[]) {
		//Start game
		for (;;) {
			moveList.clear();
			networking.updateMap(gameMap);
			
			if(isFirstTurn) {
				boolean firstShip = true;
				for (Ship ship : gameMap.getMyPlayer().getShips().values()) {
					Log.log("********First Turn**********");
					if(firstShip) {
						attackers.add(ship.getId());
						soldierCount++;
						attackCount++;
						lastAssignedShip = ship.getId();
						attack(ship);
						Log.log("Ship ID" + ship.getId() + " has been designated as an attacker number " + attackCount);
						firstShip = !firstShip;
					}else {
						workers.add(ship.getId());
						workerCount++;
						lastAssignedShip = ship.getId();
						Log.log("Ship ID" + ship.getId() + " has been designated as a worker number " + workerCount);
					}
				}
				
				Planet targetPlanet;
				HashMap<Double, Planet> planetData = new HashMap<>();

				for (final Planet planet : gameMap.getAllPlanets().values()) {
					planetData.put(gameMap.getShip(gameMap.getMyPlayerId(), workers.get(0)).getDistanceTo(planet), planet);
				}

				TreeMap<Double, Planet> sortedPlanetData = new TreeMap<>(planetData);
				Set<Entry<Double, Planet>> finalPlanetData = sortedPlanetData.entrySet();
				
				
				planetData = new HashMap<>();
				for (int i = 0; i < 3; i++) {
					planetData.put(finalPlanetData.iterator().next().getValue().getRadius(), finalPlanetData.iterator().next().getValue());
				}
				
				sortedPlanetData = new TreeMap<>(planetData);
				finalPlanetData = sortedPlanetData.entrySet();
				targetPlanet = finalPlanetData.iterator().next().getValue();
				
				for(int i: workers) {
					if(gameMap.getShip(gameMap.getMyPlayerId(), i).canDock(targetPlanet)) {
						moveList.add(new DockMove(gameMap.getShip(gameMap.getMyPlayerId(), i), targetPlanet));
					} else {
//						final ThrustMove navigate = Astar.navigate(gameMap.getShip(gameMap.getMyPlayerId(),  i), targetPlanet, gameMap);
						final ThrustMove navigate = Navigation.navigateShipToDock(gameMap, gameMap.getShip(gameMap.getMyPlayerId(), i), targetPlanet, Constants.MAX_SPEED);
						if (navigate != null) {
							moveList.add(navigate);
						}
					}
				}
				
				isFirstTurn = !isFirstTurn;
				
			}else {
				Log.log("********New Ship**********");
				turn();
			}
			Networking.sendMoves(moveList);
		}
	}

	public static void turn() {
		
		for (Ship ship: gameMap.getMyPlayer().getShips().values()) {
			if(ship.getId() > lastAssignedShip) {
				Log.log("Ship ID" + ship.getId() + " has not been assigned a role. Attempting role desagnation");
				assignRole(ship);
				lastAssignedShip = ship.getId();
			}
		}
		
		for(int id: workers) {
			try {
				Log.log("Itterating Worker: ID" + id);
				worker(gameMap.getShip(gameMap.getMyPlayerId(), id));
			}catch(NullPointerException npe) {
				Log.log("Ship ID" + id + " is dead");
			}
		}
		
		for(int id: attackers) {
			try {
				Log.log("Itterating Attacker: ID" + id);
				attack(gameMap.getShip(gameMap.getMyPlayerId(), id));
			}catch(NullPointerException npe) {
				Log.log("Ship ID" + id + " is dead");
			}
		}
		
		for(int id: defenders) {
			try {
				Log.log("Itterating Defender: ID" + id);
				defend(gameMap.getShip(gameMap.getMyPlayerId(), id));
			}catch(NullPointerException npe) {
				Log.log("Ship ID" + id + " is dead");
			}
		}
	}

	public static void assignRole(Ship ship) {
		if(workerNum >= soldierNum) {
			if(workerCount < workerNum) {
				workers.add(ship.getId());
				workerCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a worker");
			}else if(soldierCount < soldierNum) {
				Log.log("Assigning rank to ship ID" + ship.getId());
				assignRank(ship);
				soldierCount++;
			}else {
				Log.log("Assigning rank to ship ID" + ship.getId());
				assignRank(ship);
				workerCount = 0;
				soldierCount = 0;
			}
		}else {
			if(soldierCount < soldierNum) {
				Log.log("Assigning rank to ship ID" + ship.getId());
				assignRank(ship);
				soldierCount++;
			}else if(workerCount < workerNum) {
				workers.add(ship.getId());
				workerCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a worker");
			}else {
				workers.add(ship.getId());
				workerCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a worker");
				soldierCount++;
				workerCount = 0;
				soldierCount = 0;
			}
		}
	}

	public static void worker(Ship ship) {
		// Planet the ship will try to travel to
		Planet targetPlanet = null;

		// Ignore docked ships
		if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
			return;
		}

		// K Nearest Neighbors Algorithm
		HashMap<Double, Planet> planetData = new HashMap<>();

		for (final Planet planet : gameMap.getAllPlanets().values()) {
			planetData.put(ship.getDistanceTo(planet), planet);
		}

		TreeMap<Double, Planet> sortedPlanetData = new TreeMap<>(planetData);
		Set<Entry<Double, Planet>> finalPlanetData = sortedPlanetData.entrySet();

		// Set targetPlanet based off K Nearest Neighbors
		for (Entry<Double, Planet> data : finalPlanetData) {
			Planet tempPlanet = data.getValue();
			if (tempPlanet.getDockedShips().size() <= tempPlanet.getDockingSpots() / 4 || !tempPlanet.isOwned()
					|| tempPlanet.getDockedShips().size() <= tempPlanet.getDockingSpots() / 2 || !tempPlanet.isFull()) {
				targetPlanet = data.getValue();
				break;
			}
		}

		// When all planets are occupied and filled, start attack!
		if (targetPlanet == null) {
			for (Entry<Double, Planet> data : finalPlanetData) {
				Planet tempPlanet = data.getValue();
				if (tempPlanet.getOwner() != gameMap.getMyPlayerId()) {
					targetPlanet = data.getValue();
					break;
				}
			}
		}

		// Null pointers can go kill themselves
		if (targetPlanet == null) {
			targetPlanet = gameMap.getAllPlanets().values().iterator().next();
		}

		// Control Algorithm
		if (ship.canDock(targetPlanet)) {
			if ((targetPlanet.isOwned() && targetPlanet.getOwner() != gameMap.getMyPlayerId())
					|| targetPlanet.isFull()) {
				// ATTACK
				Log.log("Scooting");
				Ship targetShip = gameMap.getShip(targetPlanet.getOwner(), targetPlanet.getDockedShips().get(0));
				final ThrustMove scoot = Astar.navigate(ship, targetShip, gameMap);
//				final ThrustMove scoot = Navigation.navigateShipTowardsTarget(gameMap, ship,
//						new Position(targetShip.getXPos(), targetShip.getYPos()), Constants.MAX_SPEED, true,
//						Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI / 180.0);
				if (scoot != null) {
					moveList.add(scoot);
				}
			} else {
				// Dock on to new planet / populate existing planets
				moveList.add(new DockMove(ship, targetPlanet));
			}
		} else {
			// Default travel stuff
//			final ThrustMove navigate = Astar.navigate(ship, targetPlanet, gameMap);
			final ThrustMove navigate = Navigation.navigateShipToDock(gameMap, ship, targetPlanet, Constants.MAX_SPEED);
			if (navigate != null) {
				moveList.add(navigate);
			}
		}
	}

	public static void assignRank(Ship ship) {
		if(attackNum >= defendNum) {
			if(attackCount < attackNum) {
				attackers.add(ship.getId());
				attackCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a attacker");
			}else if(defendCount < defendNum) {
				defenders.add(ship.getId());
				defendCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a defender");
			}else {
				defenders.add(ship.getId());
				defendCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a defender");
				attackCount = 0;
				defendCount = 0;
			}
		}else {
			if(defendCount < defendNum) {
				defenders.add(ship.getId());
				defendCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a defender");
			}else if(attackCount < attackNum) {
				attackers.add(ship.getId());
				attackCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a attacker");
			}else {
				attackers.add(ship.getId());
				attackCount++;
				Log.log("Ship ID" + ship.getId() + " has been designated as a attacker");
				attackCount = 0;
				defendCount = 0;
			}
		}
	}

	public static void attack(Ship ship) {

		HashMap<Double, Ship> shipData = new HashMap<>();
		
		for (Ship targetShip : gameMap.getAllShips()) {
			if (targetShip.getOwner() != gameMap.getMyPlayerId()) {
				shipData.put(ship.getDistanceTo(targetShip), targetShip);
			}
		}

		TreeMap<Double, Ship> sortedShipData = new TreeMap<>(shipData);
		Set<Entry<Double, Ship>> finalShipData = sortedShipData.entrySet();
		
		Ship temp = finalShipData.iterator().next().getValue();
		
		Log.log("Attacking Ship ID" + temp.getId());
//		final ThrustMove navigate = Astar.navigate(ship, temp, gameMap);
		final ThrustMove navigate = Navigation.navigateShipToDock(gameMap, ship, temp, Constants.MAX_SPEED);
		if (navigate != null) {
			moveList.add(navigate);
		}
		
	}

	public static void defend(Ship ship) {
		attack(ship);
	}
}
