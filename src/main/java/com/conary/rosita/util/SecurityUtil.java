package com.conary.rosita.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad criptografica para generar y verificar hashes SHA-256
 * de contrasenas de forma compatible con la funcion SHA2(..., 256) de MySQL.
 * Totalmente compatible con Java 8.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Calcula el hash SHA-256 en formato hexadecimal en minusculas.
     *
     * @param textoPlano Cadena de texto a procesar.
     * @return Hash SHA-256 de 64 caracteres hexadecimales.
     */
    public static String hashSHA256(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(textoPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error fatal: Algoritmo SHA-256 no disponible en la JVM", e);
        }
    }

    /**
     * Verifica si una contrasena en texto plano coincide con el hash almacenado.
     *
     * @param passwordPlana Contrasena ingresada por el usuario.
     * @param hashAlmacenado Hash hexadecimal almacenado en base de datos.
     * @return true si coinciden exactamente.
     */
    public static boolean verificarPassword(String passwordPlana, String hashAlmacenado) {
        if (passwordPlana == null || hashAlmacenado == null) {
            return false;
        }
        String hashCalculado = hashSHA256(passwordPlana);
        return hashCalculado.equalsIgnoreCase(hashAlmacenado.trim());
    }
}

