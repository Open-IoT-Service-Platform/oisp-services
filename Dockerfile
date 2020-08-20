# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#

#Build beam application and embedd in Spark container
FROM maven:3.6.1-jdk-8-alpine AS rule-engine-builder

RUN apk update && apk add build-base

ADD oisp-beam-rule-engine/pom.xml /app/pom.xml
RUN mkdir /app/checkstyle
ADD oisp-beam-rule-engine/checkstyle/checkstyle.xml /app/checkstyle/checkstyle.xml
ADD oisp-beam-rule-engine/src /app/src

WORKDIR /app

RUN mvn checkstyle:check pmd:check clean package -Pflink-runner  -DskipTests

FROM httpd:2.4
COPY --from=rule-engine-builder /app/target/rule-engine-bundled-0.1.jar /usr/local/apache2/htdocs/rule-engine-bundled-0.1.jar