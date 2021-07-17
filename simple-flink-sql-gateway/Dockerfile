FROM  maven:3.8.1-openjdk-8
ADD udf/ udf
RUN cd udf/Epoch2SQL && mvn package
RUN cd udf/GetJsonField && mvn package
FROM flink:1.13.0
RUN apt update && apt install nodejs npm -y && rm -rf /var/lib/apt/lists
RUN mkdir -p /opt/gateway
ENV SIMPLE_FLINK_SQL_GATEWAY_ROOT=/opt/flink
WORKDIR /opt/gateway
ADD gateway.js gateway.js
ADD package.json package.json
ADD lib/ lib
RUN mkdir jars && cd jars && wget https://repo1.maven.org/maven2/org/apache/flink/flink-sql-connector-kafka_2.11/1.13.0/flink-sql-connector-kafka_2.11-1.13.0.jar
RUN npm install
COPY --from=0  /udf/Epoch2SQL/target/Epoch2SQL-1.0-SNAPSHOT.jar /udf/GetJsonField/target/getJsonField-0.9.jar jars/
ADD sql-client-defaults.yaml /opt/flink/conf/
USER 9999
ENTRYPOINT [ "nodejs ./gateway.js" ]
