package com.nc.sinpase.poc.modulith.covoit;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateHash {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String rawPassword = "Password1234xxx";
        String hash = encoder.encode(rawPassword);

        System.out.println("Mot de passe : " + rawPassword);
        System.out.println("Hash BCrypt : " + hash);
    }
}