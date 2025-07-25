# Unbound Platform: Backend Libraries & Dependencies

---

## Core Libraries

| Library | Purpose |
|---------|---------|
| **spring-boot-starter-web** | REST API, web server, controllers |
| **spring-boot-starter-data-jpa** | ORM, database access, repositories |
| **spring-boot-starter-security** | Authentication, authorization, JWT integration |
| **lombok** | Boilerplate reduction (getters, setters, builders, etc.) |
| **mysql-connector-java** | MySQL database driver |
| **spring-boot-starter-mail** | Sending emails (registration, payment, reminders) |

## Security & Validation

| Library | Purpose |
|---------|---------|
| **jjwt-api, jjwt-impl, jjwt-jackson** | JWT creation, parsing, validation |
| **jakarta.validation-api** | Bean validation (annotations like @NotNull, @Email) |
| **hibernate-validator** | Implementation of Jakarta Validation |

## Payments & Media

| Library | Purpose |
|---------|---------|
| **razorpay-java** | Razorpay payment gateway integration |
| **openpdf** | PDF generation (certificates) |

## Testing

| Library | Purpose |
|---------|---------|
| **spring-boot-starter-test** | Testing (unit, integration) |
| **spring-security-test** | Security-related testing |

---

## Notes
- All libraries are managed via Maven in `pom.xml`.
- Spring Boot manages most configuration and dependency injection.
- Lombok annotations reduce boilerplate code in entities and DTOs.
- Razorpay and OpenPDF are used for payment and certificate features, respectively.
- Validation is enforced using Jakarta Validation and Hibernate Validator.
- JWT is used for stateless authentication and role-based access control. 