MVN = mvn
MVN_FLAGS = -B

# Define targets and dependencies
.PHONY: clean compile run-client run-server

# Build target
build:
	$(MVN) $(MVN_FLAGS) clean install

# Compile target
compile:
	$(MVN) $(MVN_FLAGS) compile

# Run client target
run-client:
	$(MVN) $(MVN_FLAGS) javafx:run

# Run server target
run-server:
	$(MVN) $(MVN_FLAGS) exec:java@exec-java1

# Clean target
clean:
	$(MVN) $(MVN_FLAGS) clean
