package com.fspl.medica_healthcare.utils;


import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class EncryptionUtil {

    private static final String SECRET_KEY = "Y6#pA!m8^bXz@2QwL9sT$RvC%FkG7dNh"; // Store securely
    //private static final String RANDOM_STRING = "55675434"; // Must be at least 8 characters
    private static final String CIPHER_KEY = "64919698"; // Ensures same encryption output for same input


    private final TextEncryptor encryptor;

    public EncryptionUtil() {

        this.encryptor = Encryptors.queryableText(SECRET_KEY, CIPHER_KEY);
    }

    public String encrypt(String text) {

        return encryptor.encrypt(text);
    }

    public String decrypt(String encryptedText) {
        return encryptor.decrypt(encryptedText);
    }





}
