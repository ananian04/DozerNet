# DozerNet — Web-Based JCB Management System

DozerNet replaces phone-based JCB (construction equipment) rental coordination with one
verified, centralised platform connecting **customers, private owners, operators and
administrators**.

SE2030 Software Engineering group project — Group **MLB-B4G2-09**.

## Tech stack

| Layer      | Technology                                              |
|------------|---------------------------------------------------------|
| Language   | Java 21                                                 |
| Framework  | Spring Boot 3 (MVC, Data JPA, Security, Validation)     |
| Views      | Thymeleaf + a custom CSS design system (Apple-inspired) |
| Landing 3D | Three.js hero (JCB model / procedural fallback)         |
| Database   | MySQL (via MySQL Workbench); H2 in-memory for dev/tests |
| Build      | Maven                                                   |
| Auth       | BCrypt password hashing + role-based access control     |

## Project structure (organised by member/module)

```
src/main/java/com/dozernet/
├── common/              shared: security, base entity, users, notification publisher, exceptions
├── module1_customer/    Customer Management (+ customer notifications) — G. Ananian (IT25102642)
├── module2_booking/     Booking & Rental           — Theekshana A.D. (IT25101810)
├── module3_fleet/       Fleet (JCB) Management      — Jayawardhana A.G.D.L. (IT25100798)
├── module4_operator/    Operator Management         — Perera M.A.S.A. (IT25103606)
├── module5_maintenance/ Maintenance & Service       — Ruvendi R.G.K. (IT25101701)
└── module6_payment/     Payment & Administration    — Thathsara J.P.T. (IT25103683)
```

See [MODULES.md](MODULES.md) for the full owner map and [docs/DESIGN_PATTERNS.md](docs/DESIGN_PATTERNS.md)
for the design patterns used.

## Running the app

### Option A — zero setup (H2 in-memory, default)

```bash
mvn spring-boot:run
```

Open http://localhost:8080. The H2 console is at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:dozernet`, user `sa`, no password).

### Option B — MySQL (for the real demo / MySQL Workbench)

1. In MySQL Workbench, create the schema: `CREATE DATABASE dozernet;`
2. Update `src/main/resources/application-mysql.properties` with your MySQL username/password.
3. Run with the MySQL profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## Demo accounts

Seeded automatically on first run. Password for **all** demo accounts: `password123`.

| Role     | Email                  |
|----------|------------------------|
| Admin    | admin@dozernet.lk      |
| Customer | customer@dozernet.lk   |
| Owner    | owner@dozernet.lk      |
| Operator | operator@dozernet.lk   |

## Testing

```bash
mvn test
```

## Notes

- Requires JDK 17+ (built and tested on JDK 21).
- Currency is Sri Lankan Rupees (Rs.); phone numbers use the Sri Lankan mobile format.
