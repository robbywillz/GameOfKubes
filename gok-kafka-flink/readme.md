# Real time Message Processing in containerized world

[Apache Kafka](https://kafka.apache.org)  - is used as message(broker) engine 

[Apache Flink](https://flink.apache.org) - is used as real time stream processor

[MiniKube](https://kubernetes.io/docs/getting-started-guides/minikube) - is used to orchestrate Kafka and Flink containers

### Data Flow

Postman - REST client(send json message) **->** Kafka REST producer **->** Kafka Broker(cluster) **->** Flink Stream processor


### Start Minikube

```
$ minikube start
```

### Kafka deployment

Create Zookeeper:
```
$ kubectl.exe create -f gok-kafka-yaml\zookeeper.yaml
$ kubectl.exe create -f gok-kafka-yaml\zookeeper-service.yaml
```

Create Kafka cluster:
```
$ kubectl.exe create -f gok-kafka-yaml\kafka-cluster.yaml
$ kubectl.exe create -f gok-kafka-yaml\kafka-service.yaml
```
Create REST service to send messages to Kafka cluster:
```
$ kubectl.exe create -f gok-kafka-yaml\kafka-rest.yaml
$ kubectl.exe create -f gok-kafka-yaml\kafka-rest-service.yaml
```

### Flink deployment

Create Job manager:
```
$ kubectl.exe create -f gok-flink-yaml\jobmanager-deployment.yaml
```
Create Task manager:
```
$ kubectl.exe create -f gok-flink-yaml\taskmanager-deployment.yaml
```
Create Job manager service:
```shell
$ kubectl.exe create -f gok-flink-yaml\jobmanager-service.yaml
```

### Submit job in Flink Job manager

Build *gok-flink-kafka-connector* project:
```
$ mvn clean install -Pbuild-jar
```
Above command will generate *target/gok-flink-kafka-connector-1.0.jar*

Start the minikube proxy to expose webserver to submit the job in Job manager:
```
$ kubectl.exe proxy
```
or if default port already in use,
```
$ kubectl.exe proxy --port 8080
```
Submit job:
1. Open browser and enter below link: http://localhost:8001/api/v1/proxy/namespaces/default/services/flink-jobmanager:8081/#/overview
2. Click *Submit Job*
3. Upload *gok-flink-kafka-connector/target/gok-flink-kafka-connector-1.0.jar*
4. Click the uploaded job and *Submit*

### Send message

Get REST service of kafka producer url from *minikube*:
```
$ minikube.exe service kafka-rest-service --url
```

Use *Postman - REST Client* Google Chrome extension or any REST(HTTP) client to send messages based on above URL with the URI:
```
http://192.168.99.100:30325/gok-kafka-producer-rest/webapi/myresource
```
Supported JSON message(stream processing based on *alarmText* and *severity*):
```json
{"eventTime":"2009-03-12T11:26:18+05:30", "alarmId":1, "alarmText":"System is down", "severity": "critical"}
```
### Check the processed message

Find Flink Task manager pod:
```
$ kubectl.exe get pod
```
Print the pod logs to check the processed message aggregated count:
```
$ kubectl.exe log <pod>
 ``` 
 Output:
 ```
 {"eventTime":"2009-03-12T11:26:18+05:30", "alarmId":1, "alarmText":"System is down", "severity": "critical"}, **Count: 1**
 ```
 
