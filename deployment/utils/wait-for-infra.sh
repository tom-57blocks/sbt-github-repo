#!/bin/bash

counter=0
max_wait=60
echo "Waiting for test infra to come up ... "
until curl --max-time 1 -s  http://127.0.0.1:8081/subjects && mysql -h 127.0.0.1 -P 3307 -e "select 1" -u root
do 
 ((counter++))
 if [[ $counter == "$max_wait" ]]
 then 
    echo "Test Infra is not available after $max_wait seconds!!!"
    docker-compose -f deployment/docker-compose.yml down
    exit 1
 fi
 sleep 1
done

echo "Test infra is up and ready!"
