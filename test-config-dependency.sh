#!/bin/bash

echo "=== Testing Config Server Dependency ==="
echo

echo "1. Testing that employee-service fails without Config Server..."
echo "Starting employee-service without config-server (should fail)..."

# Start employee service without config server (expect failure)
cd employee-service
timeout 30s mvn spring-boot:run > ../test-without-config.log 2>&1 &
EMPLOYEE_PID=$!

sleep 15

# Check if process is still running (it should have failed)
if kill -0 $EMPLOYEE_PID 2>/dev/null; then
    echo "❌ Employee service unexpectedly started without config server"
    kill $EMPLOYEE_PID
else
    echo "✅ Employee service correctly failed to start without config server"
fi

echo
echo "2. Testing that services start with Config Server..."
echo "Starting config-server first..."

cd ../config-server
mvn spring-boot:run > ../config-server.log 2>&1 &
CONFIG_PID=$!

echo "Waiting for config server to start..."
sleep 20

echo "Now starting employee-service (should succeed)..."
cd ../employee-service
timeout 60s mvn spring-boot:run > ../test-with-config.log 2>&1 &
EMPLOYEE_PID=$!

sleep 25

# Check if process is running (it should be successful)
if kill -0 $EMPLOYEE_PID 2>/dev/null; then
    echo "✅ Employee service successfully started with config server"
    kill $EMPLOYEE_PID
else
    echo "❌ Employee service failed to start even with config server"
fi

# Clean up
kill $CONFIG_PID 2>/dev/null

echo
echo "=== Test Complete ==="
echo "Check test-without-config.log and test-with-config.log for details"