#!/bin/bash
mvn clean package -DskipTests &&\
cp target/stockpredictionapplication.war src/main/resources/docker/app &&\
chmod 765 src/main/resources/docker/app/stockpredictionapplication.war &&\
docker build src/main/resources/docker/db -t db &&\
docker run --name db -h db -d db &&\
docker build src/main/resources/docker/app -t stockpredictionapplication &&\
docker run --name stockpredictionapplication -p 8080:8080 --link db:db -d stockpredictionapplication