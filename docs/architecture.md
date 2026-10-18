# Architecture

## 1. Layers
```
Browser
   |
   v
web/ (M1)  servlets, filters, JSP        <- only talks to the 4 services below
   |
   v
app/ (M5)  ServiceFactory                <- created once by AppContextListener
   |
   +--> AuthService         (db/ M4)
   +--> ImageService        (stego/ M2) --> CryptoService (M3), PayloadCodec (M2), StegoEngine (M2)
   +--> ExtractionService   (crypto/ M3) -> CryptoService (M3), PayloadCodec (M2), StegoEngine (M2)
   +--> HistoryDao          (db/ M4)
```
All modules depend only on `com.stego.common` (interfaces, models, exceptions).
No module calls another module's implementation class directly.

## 2. Startup
1. Tomcat starts and runs `AppContextListener.contextInitialized`.
2. It calls `ServiceFactory.create()`, which builds every implementation.
3. The factory is stored in the ServletContext under the name `"services"`.
4. Each servlet reads it: `(ServiceFactory) getServletContext().getAttribute("services")`.

## 3. Interfaces (the contract)
| Interface | Implemented by | Used by | Methods |
|-----------|---------------|---------|---------|
| `AuthService` | M4 `AuthServiceImpl` | M1 | `login`, `register` |
| `ImageService` | M2 `ImageServiceImpl` | M1 | `embed` |
| `ExtractionService` | M3 `ExtractionServiceImpl` | M1 | `extract` |
| `HistoryDao` | M4 `HistoryDaoImpl` | M1 | `record`, `find`, `count`, `countByType`, `countByTypeSince`, `countSince` |
| `UserDao` | M4 `UserDaoImpl` | M4 | `findByUsername`, `existsByUsername`, `existsByEmail`, `create` |
| `CryptoService` | M3 `AesGcmCryptoService` | M2, M3 | `encrypt`, `decrypt` |
| `PayloadCodec` | M2 `PayloadCodecImpl` | M2, M3 | `encode`, `decode` |
| `StegoEngine` | M2 `LsbStegoEngine` | M2, M3 | `capacityInBytes`, `embed`, `extract` |

## 4. Models
- `User` (id, username, fullName, email, passwordHash)
- `HistoryRecord` (id, userId, operationType, filename, createdAt, success)
- `Payload` (version, salt, iv, cipherText)

## 5. Exceptions
| Exception | Thrown when |
|-----------|-------------|
| `AuthException` | wrong login, username or email already used |
| `CapacityExceededException` | message does not fit in the image |
| `TamperedDataException` | wrong password, modified image, damaged payload |

## 6. Embed flow
1. `EmbedServlet` calls `ImageService.embed(imageBytes, message, password)`.
2. `CryptoService.encrypt` gives a `Payload` (salt, iv, ciphertext).
3. `PayloadCodec.encode` turns the payload into bytes.
4. `StegoEngine.embed` hides the bytes in the pixels (throws `CapacityExceededException` if too big).
5. The image is saved as PNG bytes and returned; `HistoryDao.record` logs the result.

## 7. Extract flow
1. `ExtractServlet` calls `ExtractionService.extract(imageBytes, password)`.
2. `StegoEngine.extract` reads the hidden bytes.
3. `PayloadCodec.decode` rebuilds the `Payload`.
4. `CryptoService.decrypt` returns the text, or throws `TamperedDataException`.
5. `HistoryDao.record` logs the result.

## 8. Rules agreed by the team
- Packages: `com.stego.common`, `.common.model`, `.common.exception`, `com.stego.app`.
- User ids are `int` everywhere.
- DAO methods throw no checked exceptions (wrap `SQLException` in `RuntimeException`).
- Nothing marked "M1 uses" is renamed without telling M1.
- Constructors used by `ServiceFactory` must be public and match the list in its code.
