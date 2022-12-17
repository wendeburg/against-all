import socket
import sys
import os
import time
import json
import struct
import kafka
import threading
import uuid
import requests
import hashlib
import ssl
import pygame
from msvcrt import getch
from requests.packages.urllib3.exceptions import InsecureRequestWarning

requests.packages.urllib3.disable_warnings(InsecureRequestWarning)


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



class Player:
    def __init__(self, reg_ip, reg_port, engine_ip, engine_port, bootstrap_ip, bootstrap_port, player_number):
        self.player_number = player_number
        self.engine_addr = (engine_ip, engine_port)
        self.reg_addr = (reg_ip, reg_port)
        self.bootstrap_addr = [bootstrap_ip + ":" + str(bootstrap_port)]
        self._consumer=None
        self.producer=None
        self._valid_moves = {"W":"N", "A":"W", "S":"S", "D":"E", "w":"N", "a":"W", "s":"S", "d":"E", "Q":"NW", "E":"NE", "Z":"SW", "C":"SE", "q":"NW", "e":"NE", "z":"SW", "c":"SE"}
        self.token=None
        self.move=None
        self.muerto=False
        self.partida_iniciada=False
        self.alias=""
        self.partida=False

    def start_read(self):
        try:
            message_count = 0
            last_time=time.time()+9999
            while True:
                if self.partida:
                    break
                if (time.time() - last_time)>10:
                    os.system("cls||clear")
                    print(bcolors.WARNING+"Servidor no responde: intentando reconectar"+bcolors.ENDC)
                    if (time.time()-last_time)>30:
                        break
                if self.muerto:
                    break
                msg_pack=self._consumer.poll()
                for tp, messages in msg_pack.items():
                    last_time=time.time()
                    for message in messages:
                        self.partida_iniciada=True
                        if self.muerto:
                            break
                        message = message.value
                        if self.alias not in message["jugadores"]:
                            print(bcolors.FAIL + "MUELTO" + bcolors.ENDC)
                            self.muerto=True
                            break
                        self.partida = message["gamefinished"]
                        winners = message["winners"]
                        if self.partida:
                            print(bcolors.BOLD + "Partida terminada" + bcolors.ENDC)
                            print("Ganadores:", winners)
                            if self.alias in winners:
                                print(bcolors.OKGREEN + "Has ganado" + bcolors.ENDC)
                            else:
                                print(bcolors.FAIL + "Has perdido" + bcolors.ENDC)
                            break
                        jugador = message["jugadores"][self.alias]
                        map = message['mapa']
                        cities = list(message['ciudades'].keys())
                        npcs = {}
                        for n in message["npcs"].values():
                            npcs[n["token"]] = n["nivel"]
                        os.system("cls||clear")
                        #print('Message', message_count, ':')
                        string_mapa=""
                        string_mapa+=("Nivel: "+str(jugador["nivel"])+"\n")
                        string_mapa+=(cities[0]+': '+str(message['ciudades'][cities[0]])+ '             '+cities[1]+': '+ str(message['ciudades'][cities[1]])+"\n")
                        string_mapa+=('---------------------|----------------------'+"\n")
                        count = 0
                        for fila in map:
                            string_mapa+=('|')
                            count_col=0
                            for elem in fila:
                                count_col+=1
                                string_mapa+=(' ')
                                if len(elem) > 1:
                                    if elem[0]==1:
                                        string_mapa+=(bcolors.FAIL + str(npcs[elem[1]]) + bcolors.ENDC)
                                    else:
                                        string_mapa+=(bcolors.WARNING + 'M' + bcolors.ENDC)
                                else:
                                    match elem[0]:
                                        case 0:
                                            string_mapa+=(' ')
                                        case 1:
                                            string_mapa+=(bcolors.OKGREEN + 'A' + bcolors.ENDC)
                                        case 2:
                                            string_mapa+=(bcolors.WARNING + 'M' + bcolors.ENDC)
                                        case self.token:
                                            string_mapa+=(bcolors.OKBLUE + 'P' + bcolors.ENDC)
                                        case _:
                                            #string_mapa+=(bcolors.FAIL + 'E' + bcolors.ENDC)
                                            if elem[0] in npcs:
                                                string_mapa+=(bcolors.FAIL + str(npcs[elem[0]]) + bcolors.ENDC)
                                            else:
                                                string_mapa+=(bcolors.FAIL + 'E' + bcolors.ENDC)
                                if count_col==10:
                                    string_mapa+=('|')
                            count+=1
                            if count==10:
                                string_mapa+=(' |'+"\n")
                                string_mapa+=('---------------------|----------------------'+"\n")
                            else:
                                string_mapa+=(' |'+"\n")
                        string_mapa+=('---------------------|----------------------'+"\n")
                        string_mapa+=(cities[2]+': '+str(message['ciudades'][cities[2]])+ '             '+cities[3]+': '+ str(message['ciudades'][cities[3]])+"\n")
                        print(string_mapa)
                        message_count += 1
        except Exception as exc:
            print("Ha ocurrido un error al recibir mensajes desde el servidor:", exc)
            print(bcolors.OKCYAN + "Pulsa enter para continuar." + bcolors.ENDC)
            input()

    def update_every_second(self):
        while True:
            if self.move is None:
                time.sleep(2)
                self.producer.send("PLAYERMOVEMENTS", {self.token: "KA"})
            if self.muerto:
                break

    def start_write(self):
        try:
            t = threading.Thread(target=self.update_every_second)
            t.start()
            while True:
                if self.muerto:
                    break
                #event = keyboard.read_event()
                #if event.event_type == keyboard.KEY_DOWN:
                #    self.move = event.name
                #else:
                #    continue
                # Coger la tecla pulsada sin tener que darle a enter
                self.move = getch()
                
                if not self.partida_iniciada:
                    print(bcolors.WARNING + "Partida no iniciada" + bcolors.ENDC)
                elif self.move in self._valid_moves.keys():
                    m = self._valid_moves[self.move]
                    self.producer.send("PLAYERMOVEMENTS", {self.token: m})
                    self.move = None
                elif not self.muerto:
                    print(bcolors.WARNING + "Movimiento no válido" + bcolors.ENDC)
        except Exception as exc:
            print("Ha ocurrido un error al enviar mensajes al servidor:", exc)
            print(bcolors.OKCYAN + "Pulsa enter para continuar." + bcolors.ENDC)
            input()
    

    def register(self, operation):
        try:
            server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server.settimeout(30)
            try:
                server.connect(self.reg_addr)
            except:
                raise Exception("Servidor de registro no disponible")
            msg_server = server.recv(3).decode(FORMAT)
            if msg_server[2:]==ACK:
                print (f"Establecida conexión con el servidor de registro.")
            else:
                msg_server = server.recv(2048).decode(FORMAT)
                if not check_lrc(msg_server):
                    raise Exception("Ha ocurrido un error en el lrc")
                msg_server=unpack(msg_server)
                raise Exception(msg_server)
            send(operation, server)
            msg_server = server.recv(3).decode(FORMAT)
            while msg_server[2:]==ACK:
                msg_server = server.recv(2048).decode(FORMAT)
                if not check_lrc(msg_server):
                    raise Exception("Ha ocurrido un error en el lrc")
                msg_server=unpack(msg_server)
                print(msg_server)
                if msg_server=="FIN" or msg_server[:5]=="ERROR":
                    if msg_server=="FIN":
                        print(bcolors.OKGREEN + "Usuario registrado correctamente" + bcolors.ENDC)
                    break
                msg=input()
                if msg_server=="Alias: " and operation=="edit":
                    print("Contraseña: ")
                    contra=input()
                    entry = {"alias" : msg, "password": contra}
                    send(json.dumps(entry), server)
                else:
                    send(msg, server)
                msg_server = server.recv(3).decode(FORMAT)
            else:
                print(bcolors.WARNING + "Ha ocurrido un error:" + bcolors.ENDC, msg_server)

            send(EOT, server)        
            server.close()
        except socket.timeout as exc:
            print(bcolors.WARNING + "El servidor de registro ha tardado demasiado en responder:" + bcolors.ENDC, exc)
        except Exception as exc:
            print("Ha ocurrido un error:", exc)
        finally:
            print(bcolors.OKCYAN + "Pulsa enter para continuar." + bcolors.ENDC)
            input()

    def join_game(self):
        try:
            #cafile = "./secrets/player." + str(self.player_number) + ".CARoot.pem"
            #certfile = 
            self.token=None
            self.move=None
            self.muerto=False
            self.partida_iniciada=False
            self.alias=""
            self.partida=False
            try:
                self._consumer = kafka.KafkaConsumer("MAP",
                                            auto_offset_reset='latest', enable_auto_commit=True,
                                        bootstrap_servers=self.bootstrap_addr,
                                        security_protocol='SSL',
                                        ssl_check_hostname=False,
                                        ssl_cafile="./secrets/player.CARoot.pem",
                                        ssl_certfile="./secrets/player."+str(self.player_number)+".certificate.pem",
                                        ssl_keyfile="./secrets/player."+str(self.player_number)+".key.pem",
                                        ssl_password="against-all-aa-player-password",
                                        value_deserializer=lambda x: json.loads(x.decode('utf-8')),
                                        group_id=str(uuid.uuid4()), consumer_timeout_ms=120000)
                self.producer = kafka.KafkaProducer(bootstrap_servers=self.bootstrap_addr,
                                        security_protocol='SSL',
                                        ssl_check_hostname=False,
                                        ssl_cafile="./secrets/player.CARoot.pem",
                                        ssl_certfile="./secrets/player."+str(self.player_number)+".certificate.pem",
                                        ssl_keyfile="./secrets/player."+str(self.player_number)+".key.pem",
                                        ssl_password="against-all-aa-player-password",
                                        value_serializer=lambda x: json.dumps(x).encode('utf-8'))
            except Exception as exc:
                print(exc)
                raise Exception("Kafka broker no disponible")
            print("Alias: ")
            alias = input()
            print("Contra: ")
            contra= input()
            entry = {"alias" : alias, "password": contra}

            # Crear un socket TCP/IP
            server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            # Crear un contexto SSL
            ssl_context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH, cafile="./secrets/player.CARoot.pem")
            ssl_context.check_hostname = False
            ssl_context.set_ciphers("RSA")
            ssl_context.load_cert_chain(certfile="./secrets/player."+str(self.player_number)+".certificate.pem", keyfile="./secrets/player."+str(self.player_number)+".key.pem", password="against-all-aa-player-password")
            # Envolver el socket en un SSL socket
            server = ssl.wrap_socket(server, ca_certs="./secrets/player.CARoot.pem", cert_reqs=ssl.CERT_REQUIRED)
            server.settimeout(30)
            server.connect(self.engine_addr)
            print (f"Establecida conexión. Esperando autenticación")
            send(json.dumps(entry), server)
            msg_server = server.recv(3).decode(FORMAT)
            if msg_server[2:]==NACK:
                print("Ha ocurrido un error en la autenticación")
                send(EOT, server)        
                server.close()
                return None
            if msg_server[2:]==ACK:
                msg_server = server.recv(2048).decode(FORMAT)
            if not check_lrc(msg_server):
                print("Ha ocurrido un error en el lrc")
                #print(msg_server)
                send(EOT, server)        
                server.close()
                return None
            msg_server=unpack(msg_server)
            print("Token de partida asignado:",msg_server)
            self.token = json.loads(msg_server).get("token")

            send(EOT, server)        
            server.close()
            if self.token is not None:
                self.alias=alias
            self.play()
        except socket.timeout as exc:
            print("El servidor ha tardado demasiado en responder la autenticación:",exc)
        except Exception as exception:
            print("Ha ocurrido un error:", exception)
        finally:
            print(bcolors.OKCYAN + "Pulsa enter para continuar." + bcolors.ENDC)
            input()

    def draw_board(self):
        # Dividiendo el tablero en 4 cuadrantes de 10x10 casillas con distintos colores
        for i in range(10):
            for j in range(10):
                pygame.draw.rect(self.screen, (255, 255, 255), (i*30, j*30, 30, 30), 0)
                pygame.draw.rect(self.screen, (0, 0, 0), (i*30, j*30, 30, 30), 1)
        for i in range(10, 20):
            for j in range(10, 20):
                pygame.draw.rect(self.screen, (255, 255, 255), (i*30, j*30, 30, 30), 0)
                pygame.draw.rect(self.screen, (0, 0, 0), (i*30, j*30, 30, 30), 1)
        for i in range(10, 20):
            for j in range(10):
                pygame.draw.rect(self.screen, (0, 0, 0), (i*30, j*30, 30, 30), 0)
                pygame.draw.rect(self.screen, (255, 255, 255), (i*30, j*30, 30, 30), 1)
        for i in range(10):
            for j in range(10, 20):
                pygame.draw.rect(self.screen, (0, 0, 0), (i*30, j*30, 30, 30), 0)
                pygame.draw.rect(self.screen, (255, 255, 255), (i*30, j*30, 30, 30), 1)
        # Dibujar el tablero
        pygame.display.flip()


    def play(self):
        try:
                     

            self.muerto=False
            receive_kafka = threading.Thread(target=self.start_read)
            receive_kafka.start()
            send_kafka = threading.Thread(target=self.start_write)
            send_kafka.start()
            receive_kafka.join()
            print("Game end")
            self.muerto=True
        except Exception as exc:
            print("Ha ocurrido un error:", exc)

    def register_user(self):
        # Pedir los datos del usuario por consola
        alias = input("Ingrese el alias del usuario: ")

        # Validar que el alias tenga máximo 20 caracteres y solo contenga caracteres alfanuméricos
        if len(alias) > 20 or not alias.isalnum():
            print("El alias no es válido")
            return

        password = input("Ingrese la contraseña del usuario: ")
        
        # Cifrar la contraseña con MD5
        password = hashlib.md5(password.encode()).hexdigest()
        
        ef = input("EF: ")
        # Validar que ef y ec sean enteros válidos entre -10 y 10
        if not ef.isdigit() or not -10 <= int(ef) <= 10:
            print("El valor de ef no es válido")
            return

        ec = input("EC: ")
        if not ec.isdigit() or not -10 <= int(ec) <= 10:
            print("El valor de ec no es válido")
            return

        # URL de la ruta /register de la aplicación de Flask
        url = "https://" + self.reg_addr[0] + ":" + str(self.reg_addr[1]) + "/register"

        # Crear el cuerpo de la solicitud con los datos del usuario
        payload = {
            'alias': alias,
            'password': password,
            'ef': ef,
            'ec': ec
        }

        # Realizar la solicitud POST
        response = requests.post(url, json=payload, verify=False)

        # Imprimir el mensaje de respuesta
        print(response.text)

    # Función para editar un usuario
    def edit_user(self):
        # Pedir alias y contraseña del usuario por consola
        alias_old = input("Ingrese el alias del usuario: ")
        password_old = input("Ingrese la contraseña del usuario: ")

        # Cifrar la contraseña con MD5
        password_old = hashlib.md5(password_old.encode()).hexdigest()

        # Pedir los datos del usuario por consola
        alias = input("Ingrese el nuevo alias del usuario: ")

        # Validar que el alias tenga máximo 20 caracteres y solo contenga caracteres alfanuméricos
        if len(alias) > 20 or not alias.isalnum():
            print("El alias es demasiado largo o contiene caracteres inválidos")
            return

        password = input("Ingrese la nueva contraseña del usuario: ")

        # Cifrar la contraseña con MD5
        password = hashlib.md5(password.encode()).hexdigest()
        
        ef = input("EF: ")
        # Validar que ef y ec sean enteros válidos entre -10 y 10
        if not ef.isdigit() or not -10 <= int(ef) <= 10:
            print("El valor de ef no es válido")
            return

        ec = input("EC: ")
        if not ec.isdigit() or not -10 <= int(ec) <= 10:
            print("El valor de ec no es válido")
            return

        # URL de la ruta /edit de la aplicación de Flask
        url = "https://" + self.reg_addr[0] + ":" + str(self.reg_addr[1]) + "/edit"

        # Crear el cuerpo de la solicitud con los datos del usuario
        payload = {
            'alias_old': alias_old,
            'password_old': password_old,
            'alias': alias,
            'password': password,
            'ef': ef,
            'ec': ec
        }

        # Realizar la solicitud POST
        response = requests.post(url, json=payload, verify=False)

        # Imprimir el mensaje de respuesta
        print(response.text)
    
    def jugar_pygame(self):
        # Inicializar ventana Pygame
        pygame.init()
        self.screen = pygame.display.set_mode((600, 600))
        pygame.display.set_caption("Against All")
        # Dibujar un tablero de 20x20 casillas
        self.draw_board() 
        input()

if (len(sys.argv)==8):
    player=Player(sys.argv[1], int(sys.argv[2]), sys.argv[3], int(sys.argv[4]), sys.argv[5], int(sys.argv[6]), int(sys.argv[7]))
    while True:
        os.system('cls||clear')
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
            #player.register("reg")
            player.register_user()
            time.sleep(2)
        if opcion==2:
            #player.register("edit")
            player.edit_user()
            time.sleep(2)
        if opcion==3:
            player.join_game()
            #player.jugar_pygame()
        if opcion==4:
            os._exit(0)
else:
    print("Oops!. Something went bad. I need following args: <Registry_Server_IP> <Registry_Server_Port> <Auth_Server_IP> <Auth_Server_Port> <Bootstrap_Server_IP> <Bootstrap_Server_Port> <Player_Number>")
        
