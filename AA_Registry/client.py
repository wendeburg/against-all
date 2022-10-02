import socket
import sys

HEADER = 64
PORT = 5050
FORMAT = 'utf-8'
FIN = "FIN"
ENQ = "\x05"
ACK = "\x06"
NACK = "\x25"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"

def send(msg):
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
        if get_lrc(msg[1:i].encode(FORMAT)) == msg[i+1:].encode(FORMAT):
            return True
    return False
    
########## MAIN ##########


print("****** WELCOME TO OUR BRILLIANT SD UA CURSO 2020/2021 SOCKET CLIENT ****")

if  (len(sys.argv) == 4):
    SERVER = sys.argv[1]
    PORT = int(sys.argv[2])
    ADDR = (SERVER, PORT)
    
    client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client.connect(ADDR)
    print (f"Establecida conexión en [{ADDR}]")

    msg=sys.argv[3]
    while msg != FIN :
        if msg == "ENQ":
            print("Envio al servidor: ", "ENQ")
            send(ENQ)
            if client.recv(2048).decode(FORMAT) == ACK:
                print("Recibo del Servidor: ", "ACK")
            msg=input()
            continue
        print("Envio al servidor: ", msg)
        send(msg)
        print("Recibo del Servidor: ", client.recv(2048).decode(FORMAT))
        msg=input()

    print ("SE ACABO LO QUE SE DABA")
    print("Envio al servidor: ", "EOT")
    send(EOT)
    client.close()
else:
    print ("Oops!. Parece que algo falló. Necesito estos argumentos: <ServerIP> <Puerto> <Texto Bienvenida>")
