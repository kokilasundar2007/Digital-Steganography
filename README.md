# Digital-Steganography
Java Digital Steganography Project
# Digital Steganography

A Java web application that hides an encrypted secret message inside an image (LSB steganography + AES-GCM) and extracts it again. Runs on Apache Tomcat 10.1
.

## Features
- User registration and login (hashed passwords, login-attempt limit)
- Embed a message into a PNG/BMP image protected by a password
- Extract a hidden message (wrong password or modified image is detected)
- History of all embed/extract operations with search, filter and paging

## Requirements
| Tool | Version |
|------|---------|
| JDK | 11 or 17 |
| Maven | 3.8+ |
| Tomcat | 9.x (uses `javax.servlet`; Tomcat 10 will NOT work) |
| Database | MySQL 8 |

## Build
```bash
mvn clean package
```
Output: `target/digital-steganography.war`

## Run
1. Create the database: run `sql/schema.sql`.
2. Put your DB settings in `src/main/resources/db.properties`.
3. Copy `target/digital-steganography.war` into `<tomcat>/webapps/`.
4. Start Tomcat (`bin/startup.bat` on Windows, `bin/startup.sh` on Linux/Mac).
5. Open http://localhost:8080/digital-steganography/

## Project layout
| Package / folder | Owner | Purpose |
|------------------|-------|---------|
| `common/` | M5 | Interfaces, models, exceptions shared by everyone |
| `app/` | M5 | `ServiceFactory`, `AppContextListener` (wiring at startup) |
| `web/`, `webapp/` | M1 | Servlets, filters, JSP pages, CSS, JS |
| `stego/` | M2 | LSB engine, image handling, payload codec |
| `crypto/` | M3 | AES-GCM, key derivation, extraction service |
| `db/` | M4 | JDBC, DAOs, authentication, password hashing |
| `docs/` | M5 | Report and architecture |

## Team
| Module | Responsibility |
|--------|----------------|
| M1 | Web layer (servlets, JSP) |
| M2 | Steganography engine |
| M3 | Cryptography and extraction |
| M4 | Database and authentication |
| M5 | Integration, contracts, build, documentation |
