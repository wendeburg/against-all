import socket
import sys
import os
import time
import json
import struct
import kafka
import threading
import uuid


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
    """Función para enviar mensajes por socket

    Args:
        msg (string): Mensaje a enviar
        client (socket): Socket por donde se envia el mensaje
    """
    if msg != EOT and msg != ACK and msg != NACK:
        message = pack_msg(msg)
    else:
        message = pack_signal(msg)
    client.send(message)

def pack_signal(msg):
    """Empaquetar una señal para socket

    Args:
        msg (string): Señal a enviar

    Returns:
        string: Señal empaquetada
    """
    msg = struct.pack(">H", len(msg)) + msg.encode(FORMAT)
    return msg

def pack_msg(msg):
    """Empaquetar mensaje para sockets

    Args:
        msg (string): Mensaje a empaquetar

    Returns:
        bytes: Mensaje empaquetado (y codificado)
    """
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



class NPC:
    def __init__(self, bootstrap_ip, bootstrap_port):
        self.bootstrap_addr = [bootstrap_ip + ":" + str(bootstrap_port)]
        self._consumer = kafka.KafkaConsumer("MAP",
                                        auto_offset_reset='latest', enable_auto_commit=True,
                                       bootstrap_servers=self.bootstrap_addr,
                                       value_deserializer=lambda x: json.loads(x.decode('utf-8')),
                                       group_id=str(uuid.uuid4()))
        self.producer = kafka.KafkaProducer(bootstrap_servers=self.bootstrap_addr,
                                      value_serializer=lambda x: json.dumps(x).encode('utf-8'))
        self.data = []
        self._valid_moves = {"W":"N", "A":"W", "S":"S", "D":"E", "w":"N", "a":"W", "s":"S", "d":"E", "Q":"NW", "E":"NE", "Z":"SW", "C":"SE", "q":"NW", "e":"NE", "z":"SW", "c":"SE"}
        self.move=None
    
    def update_every_second(self):
        while True:
            if self.move is None:
                time.sleep(1)
                self.producer.send("PLAYERMOVEMENTS", {self.token: "KA"})
    
    def start_read(self):
        self.receive_message()

    def receive_message(self):
        message_count = 0
        for message in self._consumer:
            message = message.value
            jugadores = message["jugadores"]
            if self.token not in jugadores:
                print("MUELTO")
                break
            self.data.append(message)
            message_count += 1

    def start_write(self):
        t = threading.Thread(target=self.update_every_second)
        t.start()
        while True:
            self.move = input()
            if self.move in self._valid_moves.keys():
                m = self._valid_moves[self.move]
                self.producer.send("PLAYERMOVEMENTS", {self.token: m})
                self.move = None
    

    def register(self, operation):
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

    def join_game(self):

        consumer = kafka.KafkaConsumer("TOKENOFFERS",
                                        auto_offset_reset='latest', enable_auto_commit=True,
                                       bootstrap_servers=self.bootstrap_addr,
                                       value_deserializer=lambda x: json.loads(x.decode('utf-8')),
                                       group_id=str(uuid.uuid4()))
        producer = kafka.KafkaProducer(bootstrap_servers=self.bootstrap_addr,
                                      value_serializer=lambda x: json.dumps(x).encode('utf-8'))

        npcid = str(uuid.uuid4())
        producer.send("NPCAUTHREQUEST", {"type":"request", "npcid":npcid})
        for message in consumer:
            message = message.value
            if message["npcid"] == npcid:
                self.token=message["token"]

        self.play()

    def play(self):
        receive_kafka = threading.Thread(target=self.start_read)
        receive_kafka.start()
        send_kafka = threading.Thread(target=self.start_write)
        send_kafka.start()
        receive_kafka.join()
        print("Game end")

if (len(sys.argv)==2):
    player=NPC(sys.argv[1], int(sys.argv[2]))

    player.join_game()
    time.sleep(2)
else:
    print("Oops!. Something went bad. I need following args: <Bootstrap_Server_IP> <Bootstrap_Server_Port>")
        
