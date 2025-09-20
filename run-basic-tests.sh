#!/bin/bash

echo "======================================================"
echo "Running Basic Tests - Spring Boot Microservices"
echo "======================================================"

echo "Current directory: $(pwd)"
echo "Maven version: $(mvn --version | head -1)"
echo

# Test 1: Simple Maven test run for employee service
echo "------------------------------------------------------"
echo "1. Testing Employee Service (Simple)"
echo "------------------------------------------------------"
mvn -pl employee-service test -Dtest=EmployeeServiceSimpleTest -Dspring.profiles.active=test
if [ $? -eq 0 ]; then
    echo "✅ Employee Service Simple Test - SUCCESS"
else
    echo "❌ Employee Service Simple Test - FAILED"
    echo "Let's try a basic compile first..."
    mvn -pl employee-service compile
fi

echo
echo "------------------------------------------------------"
echo "2. Testing Department Service (Simple)"
echo "------------------------------------------------------"
mvn -pl department-service test -Dtest=DepartmentServiceTest -Dspring.profiles.active=test
if [ $? -eq 0 ]; then
    echo "✅ Department Service Test - SUCCESS"
else
    echo "❌ Department Service Test - FAILED"
    echo "Let's try a basic compile first..."
    mvn -pl department-service compile
fi

echo
echo "------------------------------------------------------"
echo "3. Full Test Run (if basics work)"
echo "------------------------------------------------------"
echo "Running all tests for employee-service..."
mvn -pl employee-service test -Dspring.profiles.active=test

echo
echo "Running all tests for department-service..."
mvn -pl department-service test -Dspring.profiles.active=test

echo
echo "======================================================"
echo "Basic test run completed!"
echo "======================================================"