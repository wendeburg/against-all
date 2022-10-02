# AGAINST-ALL
Práctica de SD.

## Documentación
Para la comunicación entre microservicios se utilizarán los siguientes caracteres:
| **Carácter** | **Código ASCII** | **Descripción**                                                         |
|--------------|:----------------:|:------------------------------------------------------------------------|
| STX          |         2        | Indica que a partir de este carácter comienza el contenido del mensaje. |
| ETX          |         3        | Indica que se ha acabado el contenido del mensaje.                      |
| EOT          |         4        | Indica que se ha cerrado la conexión.                                   |
| ENQ          |         5        | Petición para iniciar conexión.                                         |
| ACK          |         6        | Reconocimiento de petición / petición aceptada.                         |
| NAK          |        21        | Rechazo de petición por ser erronea.                                    |

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
