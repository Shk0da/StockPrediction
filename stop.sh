#!/bin/bash
docker stop db;
docker rm db;
docker stop stockpredictionapplication;
docker rm stockpredictionapplication;