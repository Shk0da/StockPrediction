#!/bin/bash
docker stop db;
docker rm db;
docker stop stockpredictionapplication;
docker rm stockpredictionapplication;
docker build src/main/resources/docker/db -t db &&\
docker run --name db -h db -d db &&\
docker build src/main/resources/docker/app -t stockpredictionapplication &&\
docker run --name stockpredictionapplication -p 80:80 -p 8080:8080 --link db:db -d stockpredictionapplication