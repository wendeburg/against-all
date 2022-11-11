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
    edit_alias=""
    alias=""
    contra=""
    nivel=""
    ef=""
    ec=""
    send("Alias: ", conn)
    mongo_client = pymongo.MongoClient("mongodb://"+IP_BD+":"+PORT_BD+"/")
    db = mongo_client["against-all-db"]
    players = db["users"]
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
                print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                send(ACK, conn)

                if alias=="":
                    if op=="reg":
                        if players.count_documents({"alias": msg}) > 0:
                            send("ERROR: " + bcolors.FAIL + "Alias duplicado" + bcolors.ENDC, conn)
                            break
                        alias=msg
                        send("Contraseña:", conn)
                    else:
                        if players.count_documents({"alias": msg}) == 0:
                            send("ERROR: " + bcolors.FAIL + "Alias no existe" + bcolors.ENDC, conn)
                            break
                        edit_alias=msg
                        send("Nuevo alias:", conn)
                        op="reg"
                elif contra=="":
                    contra=msg
                    send("Nivel:", conn)
                elif nivel=="":
                    try:
                        int_msg=int(msg)
                        nivel=msg
                    except:
                        send("Nivel no válido", conn)
                        break
                    send("EF:", conn)
                elif ef=="":
                    try:
                        int_msg=int(msg)
                        ef=msg
                    except:
                        send("EF no válido", conn)
                        break
                    send("EC:", conn)
                else:
                    try:
                        int_msg=int(msg)
                        ec=msg
                    except:
                        send("EC no válido", conn)
                        break
                    send("FIN", conn)
                    entry={'alias':alias, 'password':contra, 'nivel':nivel, 'ef':ef, 'ec':ec}
                    if edit_alias=="":
                        players.insert_one(entry)
                    else:
                        players.find_one_and_replace({ "alias" : edit_alias }, entry)
            else:
                print("Ha ocurrido un error con el mensaje")
                send(NACK, conn)
    return False
    

def handle_client(conn, addr):
    print(f"[NUEVA CONEXION] {addr} connected.")
    
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
                print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                send(ACK, conn)
                connected = handle_register(conn, addr, msg)
                print("Connection ended")
            else:
                print("Ha ocurrido un error con el mensaje")
                send(NACK, conn)
    print("ADIOS. TE ESPERO EN OTRA OCASION")
    conn.close()


def start():
    server.listen()
    print(f"[LISTENING] Servidor a la escucha en {SERVER}")
    CONEX_ACTIVAS = threading.active_count()-1
    print(CONEX_ACTIVAS)
    while True:
        conn, addr = server.accept()
        CONEX_ACTIVAS = threading.active_count()
        if (CONEX_ACTIVAS <= MAX_CONEXIONES): 
            thread = threading.Thread(target=handle_client, args=(conn, addr))
            thread.start()
            print(f"[CONEXIONES ACTIVAS] {CONEX_ACTIVAS}")
            print("CONEXIONES RESTANTES PARA CERRAR EL SERVICIO", MAX_CONEXIONES-CONEX_ACTIVAS)
        else:
            print("DEMASIADAS CONEXIONES. ESPERANDO A QUE ALGUIEN SE VAYA")
            conn.send("DEMASIADAS CONEXIONES. Tendrás que esperar a que alguien se vaya".encode(FORMAT))
            conn.close()
            CONEX_ACTUALES = threading.active_count()-1


print("Registry starting...")

if (len(sys.argv) == 4):
    PORT = int(sys.argv[1])
    ADDR = (SERVER, PORT)
    IP_BD = sys.argv[2]
    PORT_BD = sys.argv[3]
    MAX_CONEXIONES = sys.argv[4]

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(ADDR)

    start()
else:
    print ("Oops!. Something went bad. I need following args: <Puerto> <ip_bd> <puerto_bd> <conexiones_maximas_concurrentes>")


    