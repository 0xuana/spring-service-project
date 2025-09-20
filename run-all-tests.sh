#!/bin/bash

echo "======================================================"
echo "Running All Tests for Spring Boot Microservices"
echo "======================================================"
echo

# Set strict error handling
set -e

# Function to print section headers
print_section() {
    echo
    echo "------------------------------------------------------"
    echo "$1"
    echo "------------------------------------------------------"
}

# Function to check if a command succeeded
check_success() {
    if [ $? -eq 0 ]; then
        echo "✅ $1 - SUCCESS"
    else
        echo "❌ $1 - FAILED"
        exit 1
    fi
}

print_section "1. Cleaning Previous Build Artifacts"
mvn clean
check_success "Clean build artifacts"

print_section "2. Running Employee Service Tests"
mvn -pl employee-service test -Dspring.profiles.active=test
check_success "Employee Service Tests"

print_section "3. Running Department Service Tests"
mvn -pl department-service test -Dspring.profiles.active=test
check_success "Department Service Tests"

print_section "4. Generating Coverage Reports"
mvn -pl employee-service jacoco:report
check_success "Employee Service Coverage Report"

mvn -pl department-service jacoco:report
check_success "Department Service Coverage Report"

print_section "5. Checking Coverage Thresholds"
mvn -pl employee-service jacoco:check
check_success "Employee Service Coverage Check (80% line, 75% branch)"

mvn -pl department-service jacoco:check
check_success "Department Service Coverage Check (80% line, 75% branch)"

print_section "6. Test Summary"
echo "📊 Coverage Reports Generated:"
echo "   • Employee Service: employee-service/target/site/jacoco/index.html"
echo "   • Department Service: department-service/target/site/jacoco/index.html"
echo
echo "🎯 Coverage Targets:"
echo "   • Line Coverage: 80% minimum"
echo "   • Branch Coverage: 75% minimum"
echo
echo "📋 Test Types Executed:"
echo "   ✅ Unit Tests (Service layer business rules)"
echo "   ✅ Web Slice Tests (@WebMvcTest for controllers)"
echo "   ✅ SpringBootTest (Full context without DB)"
echo "   ✅ Exception Handling Tests (ProblemDetail validation)"
echo "   ✅ Validation Tests (Bean validation and business rules)"
echo
echo "🚀 All tests completed successfully!"
echo "======================================================"