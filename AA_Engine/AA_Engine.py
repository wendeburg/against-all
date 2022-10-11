import sys
import threading

import authentication

PUERTO_ESCUCHA = None
MAX_JUGADORES = None
IP_CLIMA = None
PUERTO_CLIMA = None
JUGADORES = None
CIUDADES = None

def validate_arguments():
    if (len(sys.argv) != 5):
        print("Error: faltan argumentos.")
        print("Uso: python AA_Engine.py puerto_escucha max_jugadores IP_AA_Weather puerto_AA_Weather")
    else:
        try:
            PUERTO_ESCUCHA = int(sys.argv[1])
            MAX_JUGADORES = int(sys.argv[2])
            IP_CLIMA = sys.argv[3]
            PUERTO_CLIMA = int(sys.argv[4])

            return True
        except ValueError:
            print("Error: Tipo de argumentos no válido.")
            print("Uso: python AA_Engine.py puerto_escucha max_jugadores IP_AA_Weather puerto_AA_Weather")

    return False

def main():
    if not validate_arguments():
        sys.exit()
    
    espera_jugadores = threading.Thread(authentication.waitForPlayers())

    espera_jugadores.join()
    # Una vez temrina el hilo de espera jugadores, comienza la partida.


if __name__ == "__main__":
    main()