# Rental Platform

A platform for publishing rental housing listings. Tenants can book accomodations, leave reviews, and landlord manage their listings.

## Tech stack

- **Java 17**
- **Spring Boot**
  - Web
  - Data
  - Security(JWT)
- **PostgreSQL**
- **Redis**
- **AWS S3**
- **Maven**
- **JUnit and Mockito**
- **Testcontainers**
- **Docker**

## Installation and setup

1. **Clone the repository**
  ```bash
    htpps://github.com/merfonteen/rental-housing.git
    cd rental-housing
  ````
2. **Make sure you have the following installed:**  
   **Java 17:** [Download Java](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)  
   **Docker and Docker Compose:** [Install Docker](https://www.docker.com/get-started/)  
   **PostgreSQL:** [Download PostgreSQL](https://www.postgresql.org/) if you are going to use docker, this doesn't need to be installed  
   **Redis:** [Download Redis](https://redis.io/) if you are going to use docker, this doesn't need to be installed  

4. **Running the application without Docker:**    
   Update the `application.yml` and `docker-compose.yml` file with your credentials.
   ```bash
    mvn clean install
    mvn spring-boot:run
   ````

5. **Run the application using Docker Compose:**
   ````bash
     docker-compose up --build
   ````

6. **Access the application at:**  
   **Web Application:** [http://localhost:8080](http://localhost:8080)  
   **API Documentation:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui.html)





   
