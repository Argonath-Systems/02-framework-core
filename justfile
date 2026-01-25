# Core Library - Common utilities for argonathsystems frameworks

# Default recipe
default:
    @just --list

# Build the project
build:
    mvn clean compile -DskipTests

# Run tests
test:
    mvn test

# Run tests with verbose output
test-verbose:
    mvn test -Dsurefire.useFile=false

# Package the project
package:
    mvn clean package -DskipTests

# Install to local Maven repository
install:
    mvn clean install

# Run checkstyle
checkstyle:
    mvn checkstyle:check

# Generate Javadoc
docs:
    mvn javadoc:javadoc

# Clean build artifacts
clean:
    mvn clean

# Check for Hytale imports (should find NONE)
check-imports:
    @echo "Checking for Hytale imports..."
    @! grep -r "import.*hytale" src/ && echo "✓ No Hytale imports found"

# Verify no external dependencies leak
verify-deps:
    mvn dependency:tree

# Full build with all checks
full-build: clean checkstyle test package
    @echo "✓ Full build complete"
