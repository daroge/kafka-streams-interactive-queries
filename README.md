# kafka-streams-interactive-queries

This app demonstrates with code how dynamically streams can be implemented leveraging kafka Streams. The **Kafka Streams API** brought a new kafka client that enables stateless and stateful processing of incomming datas, with state being stored internally.
That internal **state store** are key value stores, where the key will be on a certain partition. Each partition can be replicated to multiple brokers for failover. Thus using kafka streams it is possible to implement shared state applications that are fault-tolerant and highly-available applications.  

This app collects data from the OpenSky Network and publish them to Kafka brokers in a reactive manner where they can be consumed for processing. Using interactive queries provided by kafka, the internal app´s state has got access to obtain the desired value.

----
The picture below depicts the app

![](kafkastreamsinteractivequeries/src/main/resources/images/app-view.png)

----
- #### For running this example
  - run **mvn clean package** in maven parent pom 
  - use the given docker-compose.yml 
  - after all things are up und running, with **curl http://localhost:7070/flights/countFrom/Germany** one can get the number of planes currently in air form Germany.
