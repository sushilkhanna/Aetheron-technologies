package com.bikepooling.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = resolveCredentials();
                if (serviceAccount == null) {
                    log.warn("Firebase credentials not found. FCM push notifications will be disabled.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully.");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    /**
     * Resolves Firebase credentials in this order:
     * 1. External file path from FIREBASE_CREDENTIALS_PATH env var (for EC2 / production)
     * 2. Classpath resource firebase-service-account.json (for local dev)
     */
    private InputStream resolveCredentials() throws IOException {
        // 1. Try external file path (EC2: set FIREBASE_CREDENTIALS_PATH=/opt/bikepooling/firebase.json)
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            java.io.File file = new java.io.File(credentialsPath);
            if (file.exists()) {
                log.info("Loading Firebase credentials from external path: {}", credentialsPath);
                return new FileInputStream(file);
            }
            log.warn("Firebase credentials path configured but file not found: {}", credentialsPath);
        }

        // 2. Fall back to classpath (local dev)
        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                log.info("Loading Firebase credentials from classpath.");
                return resource.getInputStream();
            }
        } catch (IOException ignored) {
            // Classpath resource not available
        }

        return null;
    }
}