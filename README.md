# Sistemas Concurrentes y Distribuidos

El repositorio está dividido en dos carpetas: batch y stream. En cada una de las carpetas se encuentran dos proyectos.

# Batch

## FTP
Hace una petición al servidor FTP del SAIH Cantábrico y descarga todos los ficheros CSV del mismo día (aquellos con el prefijo *aaaammdd_*, por ejemplo, *20250602_*). Antes de descargar nada, recorre el directorio */resources/ftp* para:

  1. Borrar ficheros anticuados (aquellos con un prefijo diferente a la fecha actual).
  2. No volver a descargar ficheros actuales ya descargados.

La descarga de los ficheros se hace de manera paralela, utilizando tantos hilos como núcleos tenga el ordenador. Se descargan un total de 11 ficheros CSV cada día, con unas 270 líneas cada uno, y cada descarga suele tener una duración de entre uno y tres segundos. Al paralelizarse el proceso, se consigue amenizar el tiempo de ejecución en esta parte del programa de ~20 a ~5 segundos (suponiendo que el ordenador cuenta con ocho núcleos).
La lectura de estos CSV también está paralelizada.

## Predictions
Hace una petición a la API de SAIH Ebro y consigue la última metadata de predicciones. Esta metadata se compara con la alojada en el directorio */resources* en un fichero llamado *metadata.txt*. Si este fichero no existe (es la primera vez que se hace una llamada a esta API) o sus contenidos difieren (las predicciones se han actualizado entre llamadas a la API), se hace una llamada a la API para conseguir el último listado de predicciones. La API devuelve un JSON con más de cien mil objetos con información como el código de la señal, la fecha y el valor predichos.

El programa hace una filtración de estos objetos y descarta los objetos con una fecha igual o anterior a la actual. Guarda los valores no desechados en un hashmap concurrente, y tras leerse todos los valores, hace la media de valores por cada señal. Exporta los objetos no desechados a un nuevo JSON para que sea ingerido más tarde por Apache Nifi. Este proceso no ha sido paralelizado, ya que la lectura y filtración del JSON se lleva a cabo rápidamente con un solo hilo (aproximadamente 400 milisegundos).

# Stream

Los dos proyectos (zona1 y zona2) son copias idénticas con la única diferencia de las señales declaradas. Hacen una petición a la API de SAIH Ebro y consiguen los datos de las señales indicadas (nivel de embalse, volúmen de embalse, porcentaje de volúmen de embalse, precipitaciones en las últimas 24 horas y precipitaciones acumuladas). Esta separación de proyectos se ha realizado debido a la estructura del sistema: se dedica una nube a cada zona de la cuenca del Ebro, entonces, se ha hecho la división de las señales de los diferentes embalses a sus respectivas zonas, teniendo en cuenta que la API permite realizar una llamada de un máximo de cien señales cada vez y un máximo de cinco llamadas por minuto.

En cada proyecto se encuentra un publicador y un suscriptor.

- *DataPublisher.java:* Realiza la llamada a la API, parsea el JSON que devuelve y manda la información procesada al suscriptor mediante RabbitMQ. Para hacer esto se hace uso de un exchanger tipo fanout llamado *stream_zona1/2*. Está diseñado para ejecutarse en local, o en una máquina virtual que tenga RabbitMQ instalado.

- *KafkaConnector.java:* Recibe la información procesada del publicador mediante el exchanger apropiado de RabbitMQ, y actúa de puente para reenviar esta información a Kafka. Esto es necesario debido a la incompatibilidad entre RabbitMQ y Apache Kafka, debido a que son dos brokers que funcionan de manera independiente. Esta clase de Java declara y configura un productor de Kakfa y manda la información recibida desde el publicador a Kafka con el tópico *stream_zona1/2*. Está diseñado para ejecutarse en el nodo de Proxmox que tenga el contenedor de Kafka configurado. No necesita tener RabbitMQ instalado, pues utiliza la instancia creada por el publicador al conectarse a él mediante su IP (*factory.setHost("\<IP instancia de RabbitMQ>")*). Se ha configurado *rabbit* *rabbit* como su usuario y contraseña, ya que RabbitMQ solo puede utilizar las credenciales *guest* localmente.

Estos son los comandos necesarios para poder ejecutar el suscriptor desde Proxmox:

1. Descarga Java y Maven:

```
apt install default-jdk -y
apt install maven -y
```

2. Clona el repositorio:

```
git clone https://github.com/PBL6-URTEGI/Sistema-Konkurrente-eta-banatuak.git
```

3. Ejecuta el suscriptor (es posible que primero tengas que cambiar la IP en el fichero del suscriptor):

```
cd Sistema-Konkurrente-eta-banatuak/stream
mvn exec:java -Dexec.mainClass="stream.zona1.subscriber.KafkaConnector"
```

Para actualizar el código a la última versión:

```
git reset --hard
git clean -fd
git pull
```
