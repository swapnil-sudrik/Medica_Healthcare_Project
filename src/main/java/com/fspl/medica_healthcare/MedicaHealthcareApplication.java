package com.fspl.medica_healthcare;

import com.fspl.medica_healthcare.utils.EncryptionUtil;
import com.fspl.medica_healthcare.utils.LogDirectoryCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class MedicaHealthcareApplication {
	public static void main(String[] args) {
		LogDirectoryCreator.createLogFolder();
		SpringApplication.run(MedicaHealthcareApplication.class, args);
		System.err.println("Welcome to MEDICA HEALTHCARE");
	}
}
