# Digital Steganography: Project Report

## 1. Abstract
This project hides confidential text inside digital images. The text is first
encrypted with AES-GCM using a key derived from a password, then embedded in
the least significant bits of the image pixels. The result looks the same as
the original image. A web interface lets registered users embed, extract and
review their operations.

## 2. Introduction
Encryption protects the content of a message but shows that a secret exists.
Steganography hides the existence of the message. Combining both gives two
layers of protection: an attacker must find the hidden data and then break the
encryption.

## 3. Objectives
- Hide and recover text in PNG/BMP images without visible change
- Protect the message with password-based AES-GCM encryption
- Detect a wrong password or a modified image
- Provide secure user accounts and an operation history
- Build the system as five independent modules joined by agreed interfaces

## 4. Existing and proposed system
Existing tools usually hide data without encryption, or encrypt without hiding.
The proposed system does both, adds authentication, and keeps a history.

## 5. System requirements
**Software:** JDK 11+, Maven, Tomcat 9, MySQL 8, a web browser.
**Hardware:** any computer able to run the above (4 GB RAM recommended).

## 6. System design
The application has four layers (web, services, steganography/cryptography,
database) connected only through the interfaces in `com.stego.common`.
See `architecture.md` for diagrams and flows.

## 7. Modules
| Module | Work |
|--------|------|
| M1 Web | Servlets, filters, JSP pages, CSS, JavaScript |
| M2 Steganography | LSB embedding, capacity check, PSNR, payload codec, image I/O |
| M3 Cryptography | AES-GCM, key derivation, extraction service |
| M4 Database | Schema, DAOs, password hashing, login-attempt limit |
| M5 Integration | Shared interfaces/models/exceptions, service wiring, build, documentation |

## 8. Module 5 implementation
**8.1 Contracts (`common/`).** Eight interfaces, three models and three
exceptions were defined first and shared with all members, so the other four
modules could be developed in parallel without waiting for each other.

**8.2 Wiring (`app/`).** `ServiceFactory` creates every implementation and
connects them (for example `ImageServiceImpl` receives the crypto service,
payload codec and stego engine). `AppContextListener` runs once at Tomcat
startup, builds the factory and stores it in the ServletContext under the name
`services`, so servlets do not create their own objects.

**8.3 Build (`pom.xml`).** Maven project with WAR packaging, Servlet/JSP API
(provided by Tomcat), JSTL, MySQL driver and JUnit. `mvn clean package` produces
`digital-steganography.war`.

**8.4 Documentation.** README, architecture description and this report.

## 9. Design decisions
- **Interfaces first:** removes dependencies between team members.
- **Single factory:** one place to change an implementation.
- **Checked exceptions for user-facing errors** (`AuthException`,
  `CapacityExceededException`, `TamperedDataException`) so callers must handle them.
- **Unchecked exceptions in DAOs** keep the DAO interface simple.
- **AES-GCM** gives confidentiality and tamper detection in one step.

## 10. Testing
| Test | Expected result |
|------|-----------------|
| `mvn clean package` | BUILD SUCCESS, WAR created |
| Startup with all modules merged | Log line "Stego services started" |
| Startup with a module missing | Clear error naming the missing class |
| Register, then login | Success; wrong password gives `AuthException` |
| Embed then extract with same password | Original message returned |
| Extract with wrong password | `TamperedDataException` |
| Message larger than image capacity | `CapacityExceededException` |
| History filters and paging | Correct records, newest first |

## 11. Conclusion
The system hides encrypted messages in images and recovers them reliably.
Defining shared interfaces first allowed five modules to be built separately
and integrated without changing each other's code.

## 12. Future enhancements
- Support for JPEG, audio and video carriers
- Hiding files, not only text
- Adaptive embedding in noisy image regions
- Two-factor authentication
- REST API and mobile client

## 13. References
- Oracle Java and Servlet documentation
- Apache Tomcat and Maven documentation
- NIST SP 800-38D (AES-GCM)
