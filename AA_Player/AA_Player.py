import socket
import sys
import os
import time
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



HEADER = 64
FORMAT = 'utf-8'
FIN = "FIN"
ENQ = "\x05"
ACK = "\x06"
NACK = "\x15"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"

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

class Player:
    def __init__(self, reg_ip, reg_port, engine_ip, engine_port):
        self.engine_addr = (engine_ip, engine_port)
        self.reg_addr = (reg_ip, reg_port)
    

    def hacer_cosas(self, operation):
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.connect(self.reg_addr)
        print (f"Establecida conexión en [{self.reg_addr}]")
        send(operation, server)
        msg_server = server.recv(2048).decode(FORMAT)
        while msg_server[2:]==ACK:
            msg_server = server.recv(2048).decode(FORMAT)
            if not check_lrc(msg_server):
                print("Ha ocurrido un error en el lrc")
                break
            msg_server=unpack(msg_server)
            print(msg_server)
            if msg_server=="FIN" or msg_server[:5]=="ERROR":
                
                break
            msg=input()
            send(msg, server)
            msg_server = server.recv(2048).decode(FORMAT)
        else:
            print("Ha ocurrido un error: nack")

        send(EOT, server)        
        server.close()

    def unirse_partida(self):
        print("Alias: ")
        alias = input()
        print("Contra: ")
        contra= input()
        entry = {"alias" : alias, "password": contra}
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.connect(self.engine_addr)
        print (f"Establecida conexión en [{self.engine_addr}]")
        send(json.dumps(entry), server)
        msg_server = server.recv(2048).decode(FORMAT)
        if msg_server[2:]==NACK:
            print("NACK")
            send(EOT, server)        
            server.close()
            return None
        if not check_lrc(msg_server):
            print("Ha ocurrido un error en el lrc")
            send(EOT, server)        
            server.close()
            return None
        msg_server=unpack(msg_server)
        print(msg_server)
        self.token = json.loads(msg_server).get("token")

        send(EOT, server)        
        server.close()


if (len(sys.argv)==5):
    player=Player(sys.argv[1], int(sys.argv[2]), sys.argv[3], int(sys.argv[4]))
    while True:
        os.system('cls')
        print("---------------------------------")
        print("     1. Registrarse")
        print("     2. Editar perfil")
        print("     3. Unirse a partida")
        print("     4. Salir")
        print("Opcion: ")
        try:
            opcion=int(input())
        except:
            
            print(bcolors.FAIL + "Opcion incorrecta" + bcolors.ENDC)
            time.sleep(2)
            continue
        if opcion==1:
            player.hacer_cosas("reg")
            time.sleep(2)
        if opcion==2:
            player.hacer_cosas("edit")
            time.sleep(2)
        if opcion==3:
            player.unirse_partida()
            time.sleep(2)
        if opcion==4:
            break
else:
    print("Oops!. Something went bad. I need following args: <Registry_Server_IP> <Registry_Server_Port> <Auth_Server_IP> <Auth_Server_Port>")
        
