#!/bin/bash

# Kafka가 준비될 때까지 대기
echo "kafka 준비 대기 중.."
cub kafka-ready -b kafka:9092 1 20

# 필요한 토픽 생성
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic order-created --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic stock-decreased --partitions 3 --replication-factor 1
kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic stock-decrease-failed --partitions 3 --replication-factor 1

echo "Kafka 토픽 생성 완료"