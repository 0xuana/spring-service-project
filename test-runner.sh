#!/bin/bash

echo "==========================================="
echo "Running Tests - Spring Boot Microservices"
echo "==========================================="
echo

# Test Employee Service
echo "1. Testing Employee Service..."
mvn -pl employee-service test -DfailIfNoTests=false

echo
echo "2. Testing Department Service..."
mvn -pl department-service test -DfailIfNoTests=false

echo
echo "==========================================="
echo "Test execution completed!"
echo "==========================================="