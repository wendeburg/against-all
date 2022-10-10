import socket
import sys


HEADER = 64
FORMAT = 'utf-8'
FIN = "FIN"
ENQ = "\x05"
ACK = "\x06"
NACK = "\x25"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"

def send(msg, client):
    if msg != EOT:
        message = packet(msg)
    else:
        message = msg.encode(FORMAT)
    msg_length = len(message)
    send_length = str(msg_length).encode(FORMAT)
    send_length += b' ' * (HEADER - len(send_length))
    client.send(send_length)
    client.send(message)

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

class Player:
    def __init__(self, reg_ip, reg_port):
        #self.engine_addr = (engine_ip, engine_port)
        self.reg_addr = (reg_ip, reg_port)
    

    def register(self):
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.connect(self.reg_addr)
        print (f"Establecida conexión en [{self.reg_addr}]")
        send("reg", server)
        while server.recv(2048).decode(FORMAT)==ACK:
            msg_server = server.recv(2048).decode(FORMAT)
            if not check_lrc(msg_server):
                print("Ha ocurrido un error en el lrc")
                break
            msg_server=unpack(msg_server)
            if msg_server=="FIN":
                break
            print(msg_server)
            msg=input()
            send(msg, server)
        else:
            print("Ha ocurrido un error: nack")

        send(EOT, server)        
        server.close()

if (len(sys.argv)==3):
    player=Player(sys.argv[1], int(sys.argv[2]))
    player.register()
else:
    print("Oops!. Something went bad. I need following args: <Registry_Server_IP> <Registry_Server_Port>")
        
