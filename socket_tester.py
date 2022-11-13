import sys
import socket
import struct
import json
from urllib import response

FORMAT = 'utf-8'
ENQ = "\x05"
ACK = "\x06"
NAK = "\x15"
STX = "\x02"
ETX = "\x03"
EOT = "\x04"
SV_ADRESS = (sys.argv[1], int(sys.argv[2])) # ip : port
CONNECTION = None

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

def send_char(char):
    character = None

    match char:
        case "NAK":
            character = NAK
        case "ACK":
            character = ACK
        case "ENQ":
            character = ENQ
        case "STX":
            character = STX
        case "ETX":
            character = ETX
        case "EOT":
            character = EOT

    if character is not None:
        CONNECTION.send(struct.pack(">H", len(character)))
        CONNECTION.send(character.encode(FORMAT))

def package_message(msg):
    lrc = get_lrc(msg.encode(FORMAT))
    msg = STX.encode(FORMAT) + msg.encode(FORMAT) + ETX.encode(FORMAT) + str(lrc).encode(FORMAT)
    return msg

def send_message(msg, msg_length):
    CONNECTION.send(msg_length)
    CONNECTION.send(msg)

def read_sv_response():
    print("RESPONSE RECIEVED:")

    responseLength = CONNECTION.recv(2)
    response = CONNECTION.recv(int.from_bytes(responseLength, "big", signed=False))

    print(response)

    print("\nDECODED RESPONSE:")
    decodedResponse = response.decode(FORMAT)
    print(decodedResponse)

    return decodedResponse


def send_message_handler():
    print("Enter the message to send to the server:")
    message = json.loads(input())
    packagedMessage = package_message(json.dumps(message))

    send_message(packagedMessage, struct.pack(">H", len(packagedMessage)))

    print("\nMESSAGE SENT:\n")
    print(packagedMessage)

    res = read_sv_response()
    
    if (res == ACK):
        read_sv_response()
    
    
def send_character_handler():
    cont = True
    possible_characters = ["ENQ", "ACK", "NAK", "STX", "ETX", "EOT"]

    while (cont):
        print("Write the character to send:")
        print("ENQ, ACK, NAK, STX, ETX, EOT")
        character = input()

        if character in possible_characters:
            print("CHARACTER SENT:\n")
            print(character)
            
            send_char(character)

            if character != "ACK":
                read_sv_response()

            cont = False
        else:
            print("Written character wasn't an option, try again")


print(">>>>>> SOCKET SERVER TESTER <<<<<<")
print(">>>>>>>>>>>> SD 2022 <<<<<<<<<<<<<\n")

cont = True

while (cont):
    print("Select an option:")
    print("1. Start connection")
    print("2. Send message")
    print("3. Send character")
    print("4. End connection")
    print("5. Exit program")

    selected_option = input()
    match selected_option:
        case "1": 
            CONNECTION = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            CONNECTION.connect(SV_ADRESS)
            print(f"CONNECTION STARTED WITH SERVER ${SV_ADRESS}\n")
        case "2": send_message_handler()
        case "3": send_character_handler()
        case "4": 
            CONNECTION.close()
            print(f"CONNECTION ENDED WITH SERVER ${SV_ADRESS}\n")
        case "5": cont = False
        case _: print("No such option exists, try again\n")