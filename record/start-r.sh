#!/bin/sh

rm -f tpid-release
nohup /home/library/jdk1.8/bin/java -Xms1536m -Xmx1536m -jar /home/library/insuranceapi/insuranceapi-release-1.0.0-SNAPSHOT.jar > /home/library/insuranceapi/logs/launch-release.log 2>&1 &
echo $! > tpid-release

tpid=`ps -ef|grep $APP_NAME|grep -v grep|awk '{print $2}'`
if [ ${tpid} ]; then
    echo Start Success!
else
    echo Start Failed!
fi

