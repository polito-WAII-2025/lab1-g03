# RouteAnalyzer

## Description

This project uses Docker and Docker Compose to build and run the RouteAnalyzer application. The application is built using a multi-stage Dockerfile and can be easily managed using Docker Compose.

### Dockerfile

The `Dockerfile` is divided into two stages:

1. **Builder Stage**:
    - Uses the `openjdk:22-jdk-slim` image.
    - Sets up the working directory and copies the necessary Gradle files.
    - Installs `dos2unix` to convert line endings of `gradlew` from CRLF to LF.
    - Runs the Gradle build to generate the application JAR file.

2. **Runner Stage**:
    - Uses the `openjdk:22-jdk-slim` image.
    - Sets up the working directory and copies the built JAR file from the builder stage.
    - Specifies the command to run the JAR file using `java -jar`.

### Docker Compose

The `docker-compose.yml` file defines the service for the RouteAnalyzer application:

- **routeanalyzer**:
    - Builds the Docker image using the `Dockerfile`.
    - Mounts a volume for evaluation data.
    - Exposes port 8080 (for now not needed).
    - Specifies the entrypoint to run the application JAR file.

## Instructions for Running

1. **Build and Run with Docker Compose**:
    - Ensure Docker and Docker Compose are installed on your system.
    - Navigate to the project directory containing the `docker-compose.yml` file.
    - Run the following command to build and start the services:

      ```sh
      docker-compose up --build
      ```

2. **Access the Application**:
    - The application will be accessible at `http://localhost:8080`.

3. **Stop the Services**:
    - To stop the running services, use the following command:

      ```sh
      docker-compose down
      ```

This setup ensures that the RouteAnalyzer application is built and run consistently across different environments using Docker.