# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#

#Build beam application and embedd in Spark container
FROM maven:3.6.1-jdk-8-alpine AS rule-engine-builder

RUN apk update && apk add build-base

# Add and build rule engine
# -------------------------
RUN mkdir -p /app/oisp-beam-rule-engine
ADD oisp-beam-rule-engine/pom.xml /app/oisp-beam-rule-engine/pom.xml
RUN mkdir /app/oisp-beam-rule-engine/checkstyle
ADD oisp-beam-rule-engine/checkstyle/checkstyle.xml /app/oisp-beam-rule-engine/checkstyle/checkstyle.xml
ADD oisp-beam-rule-engine/src /app/oisp-beam-rule-engine/src

WORKDIR /app

RUN cd oisp-beam-rule-engine && mvn checkstyle:check pmd:check clean package -Pflink-runner  -DskipTests

# Add and build metrics-aggregator
# --------------------------------

ADD metrics-aggregator/pom.xml /app/metrics-aggregator/pom.xml
RUN mkdir /app/metrics-aggregator/checkstyle
ADD metrics-aggregator/checkstyle/checkstyle.xml /app/metrics-aggregator/checkstyle/checkstyle.xml
ADD metrics-aggregator/src /app/metrics-aggregator/src

RUN cd metrics-aggregator && mvn checkstyle:check clean package -Pflink-runner  -DskipTests

# Add and build component-splitter
# --------------------------------
ADD component-splitter/pom.xml /app/component-splitter/pom.xml
ADD component-splitter/src /app/component-splitter/src
ADD component-splitter/checkstyle/checkstyle.xml /app/component-splitter/checkstyle/checkstyle.xml
RUN cd component-splitter && mvn clean package -Pflink-runner -DskipTests

FROM httpd:2.4
COPY --from=rule-engine-builder /app/oisp-beam-rule-engine/target/rule-engine-bundled-0.1.jar /usr/local/apache2/htdocs/rule-engine-bundled-0.1.jar
COPY --from=rule-engine-builder /app/metrics-aggregator/target/metrics-aggregator-bundled-0.1.jar /usr/local/apache2/htdocs/metrics-aggregator-bundled-0.1.jar
COPY --from=rule-engine-builder /app/component-splitter/target/component-splitter-bundled-0.1.jar /usr/local/apache2/htdocs/component-splitter-bundled-0.1.jar
