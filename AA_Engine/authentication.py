import struct
import threading
import time
import socket
import json
from AA_Engine import MAX_JUGADORES, JUGADORES, PUERTO_ESCUCHA

IP_SERVIDOR = socket.gethostbyname(socket.gethostname())
SERVIDOR = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
SERVIDOR.bind((IP_SERVIDOR, PUERTO_ESCUCHA))

FORMAT = 'utf-8'
ENQ = "\x05".encode(FORMAT)
ACK = "\x06".encode(FORMAT)
NAK = "\x15".encode(FORMAT)
STX = "\x02".encode(FORMAT)
ETX = "\x03".encode(FORMAT)
EOT = "\x04".encode(FORMAT)

def get_lrc(msg):
    lrc = 0
    for b in msg:
        lrc ^= b
    return lrc

def check_lrc(msg, lrc):
    return get_lrc(msg) == lrc

def package_message(msg):
    lrc = get_lrc(msg.encode(FORMAT))
    msg = STX.encode(FORMAT) + msg.encode(FORMAT) + ETX.encode(FORMAT) + str(lrc).encode(FORMAT)
    return msg

def send_message(cliente, msg_empaquetado):
    cliente.send(struct.pack(">H", len(msg_empaquetado)))
    cliente.send(msg_empaquetado)

def parse_message(msg):
    if len(msg) > 0:
        if msg[0] == STX:
            strings = msg[1:].split(ETX)

            if len(strings) == 2:
                if check_lrc(strings[0], int(strings[1])):
                    try:
                        return json.loads(strings[0])
                    except json.JSONDecodeError:
                        raise Exception()

    raise Exception()

def gestionar_cliente(conexion, direccion):
    print(f"Sirviendo a cliente con ip: {direccion}")

    cont = True
    while cont:
        # Leer los primeros 2 bytes que son al longitud del mensaje.
        msg_length = int.from_bytes(conexion.recv(2), signed=False)
        message = conexion.recv(msg_length).decode(FORMAT)

        if message == ENQ:
            send_message(conexion, ACK)
        elif message == EOT:
            send_message(conexion, EOT)
            cont = False
        elif message != ACK:
            try:
                peticion = parse_message(message)

                # Buscar usuario en BD.
                # Si es correcto enviar token.
                # Si no enviar NAK.
                # Guardar objeto JSON completo de usuario en JUGADORES.

            except:
                send_message(conexion, NAK)

    conexion.close()

def waitForPlayers():
    SERVIDOR.listen()
    print(f"Escuchando peticiones de autenticación en puerto {PUERTO_ESCUCHA}")

    num_jugadores = 0
    tiempo_inicial = time.time()
    # Mientras no se haya completado el número de jugadores y no hayan pasado 120 segundos
    # esperar a jugadores.
    while (num_jugadores <= MAX_JUGADORES) and (time.time() - tiempo_inicial < 120):
        conexion_cliente, dir_cliente = SERVIDOR.accept()

        if threading.active_count()-1 <= MAX_JUGADORES:
            hilo_autenticacion = threading.Thread(target=gestionar_cliente, args=(conexion_cliente, dir_cliente))
            hilo_autenticacion.start()