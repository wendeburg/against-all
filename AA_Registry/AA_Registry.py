import socket
import sys
import threading
import pymongo
import json
import struct

class bcolors:
    HEADER = '\033[95m'
    OKBLUE = '\033[94m'
    OKCYAN = '\033[96m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'

SERVER = socket.gethostbyname(socket.gethostname())
#PLAYERS_DB="./AA_Registry/PLAYERS.json"

HEADER = 64
ENQ = "\x05"
ACK = "\x06"
NACK = "\x15"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"
FORMAT = 'utf-8'


def send(msg, client):
    if msg != EOT and msg != ACK and msg != NACK:
        message = pack_msg(msg)
    else:
        message = pack_signal(msg)
    client.send(message)

def pack_signal(msg):
    msg = struct.pack(">H", len(msg)) + msg.encode(FORMAT)
    return msg


def pack_msg(msg):
    lrc = get_lrc(msg.encode(FORMAT))
    msg = STX + msg + ETX
    msg += str(lrc)
    msg = struct.pack(">H", len(msg)) + msg.encode(FORMAT)
    return msg

def get_lrc(msg):
    lrc = 0
    for b in msg:
        lrc ^= b
    return lrc

def check_lrc(message):
    msg = message[2:]
    if msg[0] == STX:
        try:
            i = 1
            while msg[i] != ETX:
                i += 1
        except:
            print("Falta ETX")
            return False
        if get_lrc(msg[1:i].encode(FORMAT)) == int(msg[i+1:]):
            return True
    return False

def unpack(message):
    msg = message[2:]
    i=1
    while msg[i]!=ETX:
        i+=1
    msg=msg[1:i]
    return msg


def handle_register(conn, addr, op):
    try:
        edit_alias=""
        alias=""
        contra=""
        nivel=""
        ef=""
        ec=""
        send("Alias: ", conn)
        try:
            mongo_client = pymongo.MongoClient("mongodb://"+IP_BD+":"+PORT_BD+"/")
        
            db = mongo_client["against-all-db"]
            players = db["users"]
        except Exception as exc:
            raise Exception("La base de datos no responde"+str(exc))
        connected = True
        while connected:
            msg = conn.recv(2048).decode(FORMAT)
            if msg[2:] == EOT:
                connected = False
            else:
                if msg[2:] == ENQ:
                    send(ACK, conn)
                if check_lrc(msg):
                    msg=unpack(msg)
                    #print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                    send(ACK, conn)

                    if alias=="":
                        if op=="reg":
                            if players.count_documents({"alias": msg}) > 0 and edit_alias=="":
                                send("ERROR: " + bcolors.FAIL + "Alias duplicado" + bcolors.ENDC, conn)
                                break
                            alias=msg
                            send("Contraseña:", conn)
                        else:
                            edit_player = json.loads(msg)
                            if players.count_documents({"alias": edit_player["alias"]}) == 0:
                                send("ERROR: " + bcolors.FAIL + "Alias no existe" + bcolors.ENDC, conn)
                                break
                            elif players.count_documents({"alias": edit_player["alias"], "password": edit_player["password"]}) == 0:
                                send("ERROR: " + bcolors.FAIL + "Contraseña incorrecta" + bcolors.ENDC, conn)
                                break
                            edit_alias=edit_player["alias"]
                            send("Nuevo alias:", conn)
                            op="reg"
                    elif contra=="":
                        contra=msg
                        send("Efecto ante el Frío:", conn)
                    elif ef=="":
                        try:
                            if int(msg)<11 and int(msg)>-11:
                                ef=msg
                            else:
                                raise Exception("El efecto ante el frío debe ser un número entero entre -10 y 10 (incluidos)")
                        except Exception as exc:
                            send("EF no válido: " + bcolors.FAIL + str(exc) + bcolors.ENDC, conn)
                            break
                        send("Efecto ante el Calor:", conn)
                    else:
                        try:
                            if int(msg)<11 and int(msg)>-11:
                                ec=msg
                            else:
                                raise Exception("El efecto ante el calor debe ser un número entero entre -10 y 10 (incluidos)")
                        except Exception as exc:
                            send("EC no válido" + bcolors.FAIL + str(exc) + bcolors.ENDC, conn)
                            break
                        send("FIN", conn)
                        entry={'alias':alias, 'password':contra, 'nivel':'1', 'ef':ef, 'ec':ec}
                        if edit_alias=="":
                            players.insert_one(entry)
                        else:
                            players.find_one_and_replace({ "alias" : edit_alias }, entry)
                else:
                    print("Ha ocurrido un error con el mensaje")
                    send(NACK, conn)
        return False
    except Exception as exc:
        print("ERROR:",exc)
    

def handle_client(conn, addr):
    print(f"[NUEVA CONEXION] {addr} connected.")
    
    connected = True
    while connected:
        try:
            msg = conn.recv(2048).decode(FORMAT)
            if msg[2:] == EOT:
                connected = False
            else:
                if msg[2:] == ENQ:
                    send(ACK, conn)
                if check_lrc(msg):
                    msg=unpack(msg)
                    print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                    send(ACK, conn)
                    connected = handle_register(conn, addr, msg)
                    print("Connection ended")
                else:
                    print("Ha ocurrido un error con el mensaje")
                    send(NACK, conn)
        except socket.timeout as exc:
            print("Conexion cerrada. El cliente tardó demasiado en responder:",exc)
        except Exception as exc:
            print("algo falló:",exc)
    print(f"[CONEXION CERRADA] {addr} disconnected.")
    conn.close()


def start():
    server.listen()
    print(f"[LISTENING] Servidor a la escucha en {SERVER}")
    CONEX_ACTIVAS = threading.active_count()//3
    print(CONEX_ACTIVAS)
    while True:
        try:
            conn, addr = server.accept()
            CONEX_ACTIVAS = threading.active_count()//3
            if (CONEX_ACTIVAS <= MAX_CONEXIONES):
                send(ACK, conn)
                thread = threading.Thread(target=handle_client, args=(conn, addr))
                thread.start()
                print(f"/n[CONEXIONES ACTIVAS] {CONEX_ACTIVAS}")
                print("/nCONEXIONES RESTANTES PARA CERRAR EL SERVICIO", MAX_CONEXIONES-CONEX_ACTIVAS-1)
            else:
                send(NACK, conn)
                print("DEMASIADAS CONEXIONES. ESPERANDO A QUE ALGUIEN SE VAYA")
                send("DEMASIADAS CONEXIONES. Tendrás que esperar a que alguien se vaya", conn)
                conn.close()
        except socket.timeout:
            continue
        except Exception as exc:
            print("Algo falló con la conexion:", exc)


print("Registry starting...")

if (len(sys.argv) == 5):
    PORT = int(sys.argv[1])
    ADDR = (SERVER, PORT)
    IP_BD = sys.argv[2]
    PORT_BD = sys.argv[3]
    MAX_CONEXIONES = int(sys.argv[4])

    try:
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.bind(ADDR)
        server.settimeout(30)
        start()
    except Exception as exc:
        print("Something failed binding the socket:", exc)
else:
    print ("Oops!. Something went bad. I need following args: <Puerto> <ip_bd> <puerto_bd> <conexiones_maximas_concurrentes>")


    