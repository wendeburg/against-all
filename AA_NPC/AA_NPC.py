import sys
import time
import json
import kafka
import threading
import uuid
import random



class NPC:
    def __init__(self, bootstrap_ip, bootstrap_port):
        self.dead=False
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
    
    def update_every_second(self):
        while self.dead==False:
            time.sleep(1)
            self.producer.send("PLAYERMOVEMENTS", {self.token: random.choice(list(self._valid_moves.values()))})
    
    def start_read(self):
        self.receive_message()

    def receive_message(self):
        message_count = 0
        for message in self._consumer:
            message = message.value
            jugadores = message["npcs"]
            if self.token not in jugadores:
                print("MUELTO")
                self.dead=True
                break
            self.data.append(message)
            message_count += 1

    def join_game(self):
        self.id = str(uuid.uuid4())
        consumer = kafka.KafkaConsumer("TOKENOFFERS",
                                        auto_offset_reset='earliest', enable_auto_commit=True,
                                       bootstrap_servers=self.bootstrap_addr,
                                       value_deserializer=lambda x: json.loads(x.decode('utf-8')),
                                       group_id=self.id)
        producer = kafka.KafkaProducer(bootstrap_servers=self.bootstrap_addr,
                                      value_serializer=lambda x: json.dumps(x).encode('utf-8'))

        
        producer.send("NPCAUTHREQUEST", {"type":"request", "npcid":self.id})
        idPartida=""
        for message in consumer:
            message = message.value
            if message["npcid"] == self.id:
                self.token=message["token"]
                idPartida=message["partida"]
                producer.send("NPCAUTHREQUEST", {"type": "offer-accepted", "npcid": self.id, "partida": idPartida, "token": self.token, "nivel": "1", "ef": "1", "ec": "1"})
                break
        print(self.token)
        self.play()

    def play(self):
        receive_kafka = threading.Thread(target=self.start_read)
        receive_kafka.start()
        send_kafka = threading.Thread(target=self.update_every_second)
        send_kafka.start()
        receive_kafka.join()
        print("Game end")

if (len(sys.argv)==3):
    player=NPC(sys.argv[1], int(sys.argv[2]))

    player.join_game()
    time.sleep(2)
else:
    print("Oops!. Something went bad. I need following args: <Bootstrap_Server_IP> <Bootstrap_Server_Port>")
        
