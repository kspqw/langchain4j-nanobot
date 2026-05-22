#!/bin/bash
cd D:/originate-project/langchain4j-nanobot
java -cp "target/classes:target/dependency/*" com.nanobot.NanobotApplication 2>&1 > /tmp/nanobot.log &
PID=$!
sleep 15
echo "Process PID: $PID"
cat /tmp/nanobot.log
kill $PID 2>/dev/null || true