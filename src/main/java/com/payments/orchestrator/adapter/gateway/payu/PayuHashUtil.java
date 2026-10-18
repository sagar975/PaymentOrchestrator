package com.payments.orchestrator.adapter.gateway.payu;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility to generate PayU hash for request authentication.
 * <p>
 * PayU uses SHA-512 hash instead of OAuth or API keys.
 * Hash format:
 * key|txnid|amount|productinfo|firstname|email|
 * udf1|udf2|udf3|udf4|udf5||||||salt
 */
public class PayuHashUtil {

    private PayuHashUtil() {
        // utility class — no instantiation
    }

    /**
     * Generates SHA-512 hash for PayU payment request.
     *
     * @param key         PayU merchant key
     * @param txnId       unique transaction id
     * @param amount      payment amount as string
     * @param productInfo product description
     * @param firstName   customer first name
     * @param email       customer email
     * @param salt        PayU merchant salt
     * @return SHA-512 hash string
     */
    public static String generateHash(String key, String txnId, String amount, String productInfo, String firstName, String email, String salt) {

        String hashString = key + "|" + txnId + "|" + amount + "|" + productInfo + "|" + firstName + "|" + email + "|||||||||||" + salt;

        return sha512(hashString);
    }

    /**
     * Computes SHA-512 hash of the given input string.
     *
     * @param input string to hash
     * @return lowercase hex SHA-512 hash
     */
    private static String sha512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 not available", e);
        }
    }
}