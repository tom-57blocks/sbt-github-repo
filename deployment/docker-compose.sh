#!/bin/sh

docker compose -f deployment/docker-compose.yml "$@"
