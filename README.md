# User & Pet Management API  
Spring Boot 3 + Java 21 + H2 + Swagger

## 📋 Overview
A RESTful API to manage **Users**, **Pets**, and their **ownerships**, with specific rules:
- A user can own multiple pets.
- A pet can have multiple owners **only if they share the same address**.
- Handles **homonyms** (users with identical name and first name).
- Address de-duplication on creation.
- Supports queries like:
  - Pets owned by a user.
  - Pets from a specific city.
  - Users that own a specific kind of pet from a specific city.
  - Pets owned by women in a specific city.
- Supports "death" management for users and pets (soft delete).

---

## 🛠 Tech Stack
- **Java 21**
- **Spring Boot 3**
- **Spring Data JPA**
- **H2 Database** (in-memory)
- **Lombok**
- **springdoc-openapi** for Swagger UI

---

## 🚀 Setup & Run

### Prerequisites
- Java 21+
- Maven 3.8+

### Steps
```bash
# Clone repo
git clone <repo-url>
cd user-pet-manager

# Run
mvn spring-boot:run
```

Application starts at: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)

---

## 🔍 API Documentation
Swagger UI: [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8080/api/v1/v3/api-docs](http://localhost:8080/api/v1/v3/api-docs)  

---

## 🗄 Database Access
H2 console: [http://localhost:8080/api/v1/h2-console](http://localhost:8080/api/v1/h2-console)  
JDBC URL: `jdbc:h2:mem:testdb`  
User: `sa` / Password: *(empty)*

---

## 📂 Package Structure
```
src/main/java/com/example
│── config        # Swagger/OpenAPI config
│── controller    # REST controllers
│── dto           # Request/Response DTOs
│── error         # Global exception handler
│── mapper        # (Optional) Entity→DTO mappers
│── model         # JPA entities + enums
│── repository    # Spring Data JPA repos
│── service       # Business logic
```

---

## 🗃 Data Model

### Entities:
- **User**
  - id, name, firstName, age, gender, address, deceased
- **Address**
  - id, city, type, addressName, number
  - Unique constraint `(city, type, addressName, number)` for de-duplication
- **Pet**
  - id, name, age, type, address, deceased
- **UserPetOwnership**
  - id, user, pet
  - Unique constraint `(user_id, pet_id)`

**Key relationship rule**:  
Pet’s `address_id` must match all its owners’ `address_id`.

---

## 💡 Design Decisions
- **Address de-duplication** implemented in `AddressService.findOrCreate()` with:
  - Canonicalization (trim, collapse spaces, lower-case)
  - DB unique constraint
  - Race condition handling with retry
- **Pet–Address rule enforcement**:
  - In `OwnershipController.link()`, validate user address matches pet address before saving ownership.
- **Soft delete** for "death" management (boolean flags).
- **No AddressController** — addresses are created via User/Pet creation.

---

## 📜 API Endpoints

### Users
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/users` | Create user with address de-dup |
| PUT    | `/users/{id}` | Update user & address |
| PATCH  | `/users/{id}/death` | Mark user as deceased |
| GET    | `/users/by-name` | Find all users by name & firstName (handles homonyms) |

### Pets
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/pets` | Create pet with address de-dup |
| PUT    | `/pets/{id}` | Update pet & address |
| PATCH  | `/pets/{id}/death` | Mark pet as deceased |
| GET    | `/pets/by-city` | List pets from specific city |

### Ownerships
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/ownerships` | Link existing user & pet (same address rule) |
| GET    | `/ownerships/pets-by-user` | List pets owned by a user |
| GET    | `/ownerships/pets-by-city` | List pets from a city |
| GET    | `/ownerships/users-by-pet-type-and-city` | List users owning a pet type in city |
| GET    | `/ownerships/pets-by-women-in-city` | List pets owned by women in city |

---

## 🧪 Sample Data
See `src/main/resources/data.sql` for examples:
```sql
INSERT INTO address (city, type, address_name, number) VALUES
('paris', 'road', 'antoine lavoisier', '10'),
('mumbai', 'street', 'marine drive', '200');

INSERT INTO users (name, first_name, age, gender, address_id, is_deceased)
VALUES ('Doe', 'John', 30, 'MALE', 1, FALSE);

INSERT INTO pet (name, age, type, is_deceased, address_id)
VALUES ('Buddy', 5, 'DOG', FALSE, 1);

INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (1, 1);
```

---

## 🧾 Evaluation Criteria Mapping
- **Code Quality & Java 21 features**: Records for DTOs, canonicalization, proper layering
- **API Design**: RESTful URLs, status codes, grouped endpoints
- **Database & Data Modeling**: Normalized tables, constraints, address de-dup
- **Testing**: (You can add JUnit + MockMvc tests)
- **Documentation**: This README + Swagger UI

---

## 📌 Running Tests
```bash
mvn test
```
*(Add unit tests for services and integration tests for controllers)*
