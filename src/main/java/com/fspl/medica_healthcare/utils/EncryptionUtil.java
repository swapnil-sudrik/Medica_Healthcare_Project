package com.fspl.medica_healthcare.utils;

import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {
    private static final String SECRET_KEY = "Y6#pA!m8^bXz@2QwL9sT$RvC%FkG7dNh"; // Store securely
    private static final String RANDOM_STRING = "55675434"; // Must be at least 8 characters

    private final TextEncryptor encryptor;

    public EncryptionUtil() {
        this.encryptor = Encryptors.text(SECRET_KEY, RANDOM_STRING);
    }

    public String encrypt(String text) {
        return encryptor.encrypt(text);
    }

    public String decrypt(String encryptedText) {
        return encryptor.decrypt(encryptedText);
    }

}
