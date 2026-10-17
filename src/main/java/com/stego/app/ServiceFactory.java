package com.stego.app;

import com.stego.common.AuthService;
import com.stego.common.CryptoService;
import com.stego.common.ExtractionService;
import com.stego.common.HistoryDao;
import com.stego.common.ImageService;
import com.stego.common.PayloadCodec;
import com.stego.common.StegoEngine;
import com.stego.common.UserDao;

import java.lang.reflect.Constructor;

public class ServiceFactory {

    private final AuthService auth;
    private final ImageService images;
    private final ExtractionService extraction;
    private final HistoryDao history;

    public ServiceFactory(AuthService auth, ImageService images,
                          ExtractionService extraction, HistoryDao history) {
        this.auth = auth;
        this.images = images;
        this.extraction = extraction;
        this.history = history;
    }

    public AuthService auth() { return auth; }
    public ImageService images() { return images; }
    public ExtractionService extraction() { return extraction; }
    public HistoryDao history() { return history; }

    public static ServiceFactory create() {
        Object db = make("com.stego.db.DbConnection");
        UserDao userDao = (UserDao) make("com.stego.db.UserDaoImpl", db);
        HistoryDao historyDao = (HistoryDao) make("com.stego.db.HistoryDaoImpl", db);
        Object hasher = make("com.stego.db.PasswordHasher");
        Object tracker = make("com.stego.db.LoginAttemptTracker");
        AuthService auth = (AuthService) make("com.stego.db.AuthServiceImpl", userDao, hasher, tracker);

        CryptoService crypto = (CryptoService) make("com.stego.crypto.AesGcmCryptoService");
        PayloadCodec codec = (PayloadCodec) make("com.stego.stego.PayloadCodecImpl");
        StegoEngine engine = (StegoEngine) make("com.stego.stego.LsbStegoEngine");

        ImageService images = (ImageService) make("com.stego.stego.ImageServiceImpl", crypto, codec, engine);
        ExtractionService extraction =
                (ExtractionService) make("com.stego.crypto.ExtractionServiceImpl", crypto, codec, engine);

        return new ServiceFactory(auth, images, extraction, historyDao);
    }

    private static Object make(String className, Object... args) {
        try {
            Class<?> cls = Class.forName(className);
            for (Constructor<?> c : cls.getConstructors()) {
                Class<?>[] types = c.getParameterTypes();
                if (types.length != args.length) {
                    continue;
                }
                boolean fits = true;
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].isInstance(args[i])) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    return c.newInstance(args);
                }
            }
            throw new IllegalStateException(
                    "No public constructor in " + className + " takes " + args.length + " matching argument(s)");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Class not found: " + className + " (is that module merged?)", e);
        } catch (ReflectiveOperationException e) {
            Throwable why = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Cannot create " + className + ": " + why, why);
        }
    }
}
