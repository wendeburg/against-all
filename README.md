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
| NACK         |        21        | Rechazo de petición.                                                    |

En el cuerpo de los mensajes, entre los carácteres STX Y ETX se enviarán objetos JSON con la información del mensaje.

### `AA_Weather`
AA_Weather recibirá un mensaje con un carácter ENQ y en el cuerpo del mensaje tendrá un número que será el número de ciudades con sus temperaturas que el servidor deberá devolver.  
Y responderá con un mensaje empezando por el carácter ACK y en el cuerpo del mensaje tendrá un objeto JSON donde las claves serán el nombre de la ciudad y el valor la temperatura en Cº de esa ciudad. Ejemplo:
`
{
    "Alicante": 26,
    "Sydney": 32
}
`
