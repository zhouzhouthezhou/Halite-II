import hlt
import logging
import pandas as pd
import _thread as Thread

game = hlt.Game("Plembleton v2")
logging.info("Just Monica")
odd = False
# TURN START
while True:
    logging.info("Start")
    command_queue = []
    game_map = game.update_map()

    for ship in game_map.get_me().all_ships():
        logging.info("*********New Ship**********")
        if odd and ship.id % 2 == 1:
            continue
        elif not odd and ship.id % 2 == 0:
            continue

        if ship.docking_status != ship.DockingStatus.UNDOCKED:
            continue
        # END IF

        planet_data = {'planet': [], 'distance': []}

        for p in game_map.all_planets():
            planet_data['planet'].append(p)
            planet_data['distance'].append(ship.calculate_distance_between(p))
        # END FOR

        df = pd.DataFrame(planet_data)
        df.set_index('distance', inplace=True)
        planet_data['distance'].sort()

        logging.info(" Size: " + str(len(planet_data['distance'])))
        for i in range(0, len(planet_data['distance'])):
            logging.info(i)
            try:
                planet = df.get('planet')[planet_data['distance'][i]].head(1)[i]
            except:
                planet = df.get('planet')[planet_data['distance'][i]]

            logging.info("Planet: " + str(planet))

            if (len(planet.all_docked_ships()) <= (planet.num_docking_spots / 4) or
                    not planet.is_owned() or
                    len(planet.all_docked_ships()) <= (planet.num_docking_spots / 2) or
                    not planet.is_full()):
                logging.info(planet)
                break
            else:
                continue
            # END IF

            if i == len(planet_data['distance'])-1:
                for p in df['planet']:
                    if planet.owner.id == game_map.get_me().id:
                        planet = p
        # END FOR



        if ship.can_dock(planet):
            command_queue.append(ship.dock(planet))
        else:
            navigate_command = ship.navigate(
                ship.closest_point_to(planet),
                game_map,
                speed=int(hlt.constants.MAX_SPEED),
                ignore_ships=False)
            if navigate_command:
                command_queue.append(navigate_command)
            # END IF
        # END IF
    # END FOR
    odd = not odd
    game.send_command_queue(command_queue)
# END WHILE

