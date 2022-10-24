# AGAINST-ALL
Práctica de SD.

## Documentación
Para la comunicación entre microservicios se utilizarán los siguientes caracteres:
| **Carácter** | **Código ASCII** | **Descripción**                                                         |
|--------------|:----------------:|:------------------------------------------------------------------------|
| STX          |         0x2      | Indica que a partir de este carácter comienza el contenido del mensaje. |
| ETX          |         0x3      | Indica que se ha acabado el contenido del mensaje.                      |
| EOT          |         0x4      | Indica que se ha cerrado la conexión.                                   |
| ENQ          |         0x5      | Petición para iniciar conexión.                                         |
| ACK          |         0x6      | Reconocimiento de petición / petición aceptada.                         |
| NAK          |        0x15      | Rechazo de petición por ser erronea.                                    |

En el cuerpo de las peticiones y respuestas, entre los carácteres STX Y ETX se enviarán objetos JSON con la información del mensaje. Y al final de los cada petición o respuesta se enviará un LRC que un número que valida que el mensaje se ha enviado correctamente.

### `AA_Weather`
AA_Weather recibirá peticiones con el formato `{"ciudade": x}` donde x será el nombre de la ciudad de la que se queire obtener la temperatura. Y responderá con un mensaje cuyo cuerpo tendrá un objeto JSON donde la clave será el nombre de la ciudad y el valor la temperatura en Cº de esa ciudad.  
Ejemplo:
`
{
    "Alicante": 26
}
`

### `AA_Engine`
Para representar un mapa se utiliza un array de arrays de ints. Cada celda del mapa tiene un significado según el valor que tenga:  
0 - Vacio  
1 - Alimento  
2 - Mina  
[3, 500] - NPC  
[501, Integer.MAX_VALUE] - Juagdores  

Los movimientos permitidos en el mapa son:  
N - Norte  
S - Sur 
E - Este  
W - Oeste   
NE - Noreste  
NW - Noroeste  
SE - Sureste  
SW - Suroeste  
KA - Keepalive - Si el jugador no envia ningún movimiento luego de 2 segundos se envia este movimiento para informarle al servidor que el usuario sigue conectado.  

### `AA_Engine - Autenticación`
AA_Engine recibirá peticiones de autenticación con el formato `{"alias": aliasJugador, "password": contraseñaJugador}`. Y responderá con un mensaje cuyo cuerpo tendrá un objeto JSON donde se incluirá una "token" que identificará al usuario durante la partida.   
Ejemplo: `{"token": 89323}`

### `AA_Engine - Juego`
AA_Engine hace uso de 2 topics de Apache Kafka. El primer topic se llama GAME y es donde AA_Engine publicará un objeto JSON que tendrá a su vez otros 3 objetos. El primer objeto "mapa" será un array de 20 arrays de 20 enteros que representa el mapa. El segundo objeto "jugadores" será un objeto cuyas claves serán los alias de los jugadores y cuyos valores un objeto que contendrá el nivel de los jugadores y la posición en el tablero. Si la posición de un jugador es [-1, -1] el jugador está muerto. El tercer objeto será un array de 4 ciudades.  
Ejemplo: `{"mapa": [[0, 1, 0, 893493, ...], [0, 123123, 0, 2, ...], ...], "jugadores": {"jugador1": {"nivel": 9, "posicion": [-1, -1]}, "jugador2": {"nivel": 10, "posicion": [0, 1]}}, "ciudades": ["ciudad1": 10, "ciudad2": 10, ...]}`  
El segundo topic se llamará PLAYERMOVEMENTS y es donde los jugadores publicaran sus movimientos para que AA_Engine los consuma. Los jugadores publicarán un JSON de este formato: Ejemplo: `{"[token]": movimiento}` donde [token] es la token de cada jugador.
