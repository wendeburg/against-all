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
| NAK          |        0x25      | Rechazo de petición por ser erronea.                                    |

En el cuerpo de las peticiones y respuestas, entre los carácteres STX Y ETX se enviarán objetos JSON con la información del mensaje. Y al final de los cada petición o respuesta se enviará un LRC que un número que valida que el mensaje se ha enviado correctamente.

### `AA_Weather`
AA_Weather recibirá peticiones con el formato `{"ciudades": x}` donde x será un número que indica la cantidad de ciduades que se quieren junto con sus temperaturas. Y responderá con un mensaje cuyo cuerpo tendrá un objeto JSON donde las claves serán el nombre de la ciudad y el valor la temperatura en Cº de esa ciudad.  
Ejemplo:
`
{
    "Alicante": 26,
    "Sydney": 32
}
`

### `AA_Engine`
Para representar un mapa se utiliza un array de arrays de ints. Cada celda del mapa tiene un significado según el valor que tenga:
0 - Vacio
1 - Alimento
2 - Mina
[3, 500] - NPC
[501, Integer.MAX_VALUE] - Juagdores

### `AA_Engine - Autenticación`
AA_Engine recibirá peticiones de autenticación con el formato `{"alias": aliasJugador, "password": contraseñaJugador}`. Y responderá con un mensaje cuyo cuerpo tendrá un objeto JSON donde se incluirá una "token" que identificará al usuario durante la partida.   
Ejemplo: `{"token": 89323}`
