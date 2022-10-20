import socket
import sys
import threading
import pymongo
import json

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
PLAYERS_DB="./AA_Registry/PLAYERS.json"

HEADER = 64
ENQ = "\x05"
ACK = "\x06"
NACK = "\x25"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"
FORMAT = 'utf-8'
MAX_CONEXIONES = 2

def packet(msg):
    lrc = get_lrc(msg.encode(FORMAT))
    msg = STX + msg + ETX
    msg = msg.encode(FORMAT) + str(lrc).encode(FORMAT)
    return msg

def get_lrc(msg):
    lrc = 0
    for b in msg:
        lrc ^= b
    return lrc

def check_lrc(msg):
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

def unpack(msg):
    i=1
    while msg[i]!=ETX:
        i+=1
    msg=msg[1:i]
    return msg

def ack(conn):
    conn.send(ACK.encode(FORMAT))

def nack(conn):
    conn.send(NACK.encode(FORMAT))

def handle_register(conn, addr, op):
    edit_alias=""
    alias=""
    nivel=""
    ef=""
    ec=""
    conn.send(packet("Alias:"))
    #with open(PLAYERS_DB, mode='r', encoding='utf-8') as feedsjson:
    #    feeds = json.load(feedsjson)
    mongo_client = pymongo.MongoClient("mongodb://localhost:27017/")
    db = mongo_client["playersdb"]
    players = db["players"]
    connected = True
    while connected:
        msg_length = conn.recv(HEADER).decode(FORMAT)
        if msg_length:
            msg_length = int(msg_length)
            msg = conn.recv(msg_length).decode(FORMAT)
            if msg == EOT:
                connected = False
            else:
                if msg == ENQ:
                    ack(conn)
                if check_lrc(msg):
                    msg=unpack(msg)
                    print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                    ack(conn)

                    if alias=="":
                        if op=="reg":
                            if players.count_documents({"alias": msg}) > 0:
                                conn.send(packet("ERROR: " + bcolors.FAIL + "Alias duplicado" + bcolors.ENDC))
                                break
                            alias=msg
                            conn.send(packet("Nivel:"))
                        else:
                            if players.count_documents({"alias": msg}) == 0:
                                conn.send(packet("ERROR: " + bcolors.FAIL + "Alias no existe" + bcolors.ENDC))
                                break
                            edit_alias=msg
                            conn.send(packet("Nuevo alias:"))
                            op="reg"
                    elif nivel=="":
                        try:
                            int_msg=int(msg)
                            nivel=msg
                        except:
                            conn.send(packet("Nivel no válido"))
                            break
                        conn.send(packet("EF:"))
                    elif ef=="":
                        try:
                            int_msg=int(msg)
                            ef=msg
                        except:
                            conn.send(packet("EF no válido"))
                            break
                        conn.send(packet("EC:"))
                    else:
                        try:
                            int_msg=int(msg)
                            ec=msg
                        except:
                            conn.send(packet("EC no válido"))
                            break
                        conn.send(packet("FIN"))
                        """
                        with open(PLAYERS_DB, mode='w', encoding=FORMAT) as feedsjson:
                            entry={'alias':alias, 'nivel':nivel, 'ef':ef, 'ec':ec}
                            feeds.append(entry)
                            json.dump(feeds, feedsjson)
                        """
                        entry={'alias':alias, 'nivel':nivel, 'ef':ef, 'ec':ec}
                        if edit_alias=="":
                            players.insert_one(entry)
                        else:
                            players.find_one_and_replace({ "alias" : edit_alias }, entry)
                else:
                    print("Ha ocurrido un error con el mensaje")
                    nack(conn)
    return False
    

def handle_client(conn, addr):
    print(f"[NUEVA CONEXION] {addr} connected.")
    
    connected = True
    while connected:
        msg_length = conn.recv(HEADER).decode(FORMAT)
        if msg_length:
            msg_length = int(msg_length)
            msg = conn.recv(msg_length).decode(FORMAT)
            if msg == EOT:
                connected = False
            else:
                if msg == ENQ:
                    ack(conn)
                if check_lrc(msg):
                    msg=unpack(msg)
                    print(f" He recibido del cliente [{addr}] el mensaje: {msg}")
                    ack(conn)
                    connected = handle_register(conn, addr, msg)
                    print("Connection ended")
                else:
                    print("Ha ocurrido un error con el mensaje")
                    nack(conn)
    print("ADIOS. TE ESPERO EN OTRA OCASION")
    conn.close()


def start():
    #with open(PLAYERS_DB, mode='w', encoding=FORMAT) as f:
    #    json.dump([],f)
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
            print("OOppsss... DEMASIADAS CONEXIONES. ESPERANDO A QUE ALGUIEN SE VAYA")
            conn.send("OOppsss... DEMASIADAS CONEXIONES. Tendrás que esperar a que alguien se vaya".encode(FORMAT))
            conn.close()
            CONEX_ACTUALES = threading.active_count()-1


print("Registry starting...")



"""
mongo_client = pymongo.MongoClient("mongodb://mongodb:27017/")
db = mongo_client["customersdb"]
customers = db["customers"]

mongo_client.server_info()

customers_list = [
  { "name": "Amy", "address": "Apple st 652"},
  { "name": "Hannah", "address": "Mountain 21"},
  { "name": "Michael", "address": "Valley 345"},
  { "name": "Sandy", "address": "Ocean blvd 2"},
  { "name": "Betty", "address": "Green Grass 1"},
  { "name": "Richard", "address": "Sky st 331"},
  { "name": "Susan", "address": "One way 98"},
  { "name": "Vicky", "address": "Yellow Garden 2"},
  { "name": "Ben", "address": "Park Lane 38"},
  { "name": "William", "address": "Central st 954"},
  { "name": "Chuck", "address": "Main Road 989"},
  { "name": "Viola", "address": "Sideway 1633"}
]
x = customers.insert_many(customers_list)
# print list of the _id values of the inserted documents:
print(x.inserted_ids)

for x in customers.find():
    print(x)
"""

if (len(sys.argv) == 2):
    PORT = int(sys.argv[1])
    ADDR = (SERVER, PORT)  

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(ADDR)

    start()
else:
    print ("Oops!. Something went bad. I need following args: <Puerto>")