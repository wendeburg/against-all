import pymongo
import sys
from flask import Flask, request, jsonify
import logging
import hashlib

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

# Inicializar Flask y establecer las opciones de configuración
app = Flask(__name__)

# Configurar los logs para que se guarden los eventos en un archivo
logging.basicConfig(filename='AA_Registry.log', level=logging.DEBUG, format='%(asctime)s -  %(levelname)s - %(message)s')



@app.route('/hola')
def hello():
    # Loggear el evento
    logging.info(request.remote_addr + ' : Se ha recibido una peticion GET a /hola')
    return 'Hola, mundo!'


@app.post('/register')
def register():
    # Obtener información del usuario del cuerpo de la solicitud
    user_data = request.json

    # Cifrar la contraseña del usuario con el algoritmo MD5
    user_data['password'] = hashlib.md5(user_data['password'].encode('utf-8')).hexdigest()

    try:
        # Conectar a la base de datos de MongoDB
        mongo_client = pymongo.MongoClient("mongodb://"+IP_BD+":"+PORT_BD+"/")

        # Seleccionar la base de datos y la colección donde se guardarán los datos del usuario
        db = mongo_client["against-all-db"]
        users_collection = db["users"]
    except Exception as exc:
        print(str(exc))
        return bcolors.FAIL + 'La base de datos no responde' + bcolors.ENDC

    # Crear un nuevo documento con los datos del usuario
    user_document = {
        'alias': user_data['alias'],
        'password': user_data['password'],
        'nivel': '1',
        'ef': user_data['ef'],
        'ec': user_data['ec']
    }

    # Comparar el alias del usuario con los alias de los usuarios registrados
    for user in users_collection.find():
        if user['alias'] == user_data['alias']:
            # Loggear el evento
            logging.warning(request.remote_addr + ' : Se ha recibido una peticion de registro con alias ya registrado')
            return bcolors.FAIL + 'El alias ya está en uso' + bcolors.ENDC

    # Agregar el documento a la colección
    users_collection.insert_one(user_document)

    # Cerrar la conexión con la base de datos
    mongo_client.close()

    # Loggear el evento
    logging.info(request.remote_addr + ' : Se ha registrado un nuevo usuario')

    # Retornar un mensaje de éxito
    return bcolors.OKGREEN + 'Usuario registrado exitosamente' + bcolors.ENDC

# Función para editar usuarios en la base de datos
@app.post('/edit')
def edit():
    #Autenticación del usuario
    user_data = request.json
    try:
        # Conectar a la base de datos de MongoDB
        mongo_client = pymongo.MongoClient("mongodb://"+IP_BD+":"+PORT_BD+"/")

        # Seleccionar la base de datos y la colección donde se guardarán los datos del usuario
        db = mongo_client["against-all-db"]
        users_collection = db["users"]
    except Exception as exc:
        print(str(exc))
        return bcolors.FAIL + 'La base de datos no responde' + bcolors.ENDC
    
    #Buscar el usuario en la base de datos
    user_document = users_collection.find({'alias': user_data['alias_old']})
    if user_document.count() == 0:
        # Loggear el evento
        logging.warning(request.remote_addr + ' : Ha intentado editar con error: Usuario no encontrado')
        # Cerrar la conexión con la base de datos
        mongo_client.close()
        return bcolors.FAIL + 'El usuario no existe' + bcolors.ENDC
    else:
        user_document = user_document[0]
        #Verificar que la contraseña sea correcta
        if user_document['password'] != user_data['password_old']:
            # Loggear el evento
            logging.warning(request.remote_addr + ' : Ha intentado editar con error: Contraseña incorrecta')
            # Cerrar la conexión con la base de datos
            mongo_client.close()
            return bcolors.FAIL + 'Contraseña incorrecta' + bcolors.ENDC
        else:
            #Actualizar los datos del usuario
            user_document['alias'] = user_data['alias']
            user_document['password'] = user_data['password']
            user_document['ef'] = user_data['ef']
            user_document['ec'] = user_data['ec']
            users_collection.update_one({'alias': user_data['alias']}, {'$set': user_document})
            # Loggear el evento
            logging.info(request.remote_addr + ' : Usuario editado exitosamente')
            # Cerrar la conexión con la base de datos
            mongo_client.close()
            return bcolors.OKGREEN + 'Usuario actualizado exitosamente' + bcolors.ENDC


print("Registry starting...")

if (len(sys.argv) == 4):
    IP_BD = sys.argv[1]
    PORT_BD = sys.argv[2]
    MAX_CONEXIONES = int(sys.argv[3])

    try:
        app.run(host='0.0.0.0', port=5050)
    except Exception as exc:
        print("Something failed:", exc)
else:
    print ("Oops!. Something went bad. I need following args: <ip_bd> <puerto_bd> <conexiones_maximas_concurrentes>")