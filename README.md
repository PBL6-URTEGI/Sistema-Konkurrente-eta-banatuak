# Sistemas Concurrentes y Distribuidos

## Stream

Hace una petición a la API de SAIH Ebro, consigue las señales de diferentes embalses y las envía a Kafka. Para ejecutar el programa:

```
cd /path/proyecto
mvn clean compile
```

Ejecutar el publisher:

```
mvn exec:java -Dexec.mainClass="stream.zona1.publisher.DataPublisher"
```

Ejecutar el connector:

```
mvn exec:java -Dexec.mainClass="stream.zona1.subscriber.KafkaConnector"
```

Para escuchar en Kafka:
```
docker exec --workdir /opt/kafka -it <contenedor> sh
bin/kafka-topics.sh --create --topic stream_zona1 --bootstrap-server localhost:9092
bin/kafka-console-consumer.sh --topic stream_zona1 --from-beginning --bootstrap-server localhost:9092
```

## Batch

### FTP

Descarga 11 CSVs del servidor FTP del SAIH Cantábrico y calcula las medias de diferentes métricas de todas sus estaciones. Para ejecutarlo:

```
cd /path/proyecto
mvn clean compile
mvn exec:java -Dexec.mainClass="batch.ftp.ConnectToServer"
```

### Predicciones

Compara la última metadata de las predicciones con la guardada en local. Si los dos archivos difieren o no existe el archivo de metadata en local, hace llamada a la API de SAIH Ebro para conseguir todas las predicciones de cinco días. El programa filtra las predicciones con fecha anterior a la actual. Para ejecutarlo:

```
cd /path/proyecto
mvn clean compile
mvn exec:java -Dexec.mainClass="batch.predictions.CheckMetadata"
```

-----------------

Si el repositorio ya se ha clonado y se quiere actualizar a la última versión del código en una máquina virtual:

```
git reset --hard
git clean -fd
git pull
mvn clean compile
```
