package net.zhuoweizhang.raspberryjuice;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Optional;

/**
 * ASSIGNMENT 3 ADDITION - Created by Huntermuze
 * This class hosts the methods responsible for decrypting ciphertext, generating the keys, calculating the HMAC, and
 * verifying the two MACs to prove authenticity. In addition, it also encapsulates both the public and private keys,
 * the symmetric key used for the HMAC algorithm and the mode of encryption (RSA/SHA256).
 *
 * NOTE: This is a package private class. It is not to be used outside the implementation for API security. Hence, no
 * accompanying COMPLETE (minus this class doc) javadocs will be provided for this class because it is not intended
 * for public use. Details can be found in short comments below.
 */

class Encryption {

	private final String encryptionMode;
	private final String secretKey;
	private final PublicKey publicKey;
	private final PrivateKey privateKey;

	Encryption(String encryptionMode, String secretKey) {
		this.encryptionMode = encryptionMode;
		this.secretKey = secretKey;
		KeyPair keys = generateKeys();
		this.publicKey = keys.getPublic();
		this.privateKey = keys.getPrivate();
	}

	// Generates the public and private keys using the RSA algorithm with 2048 bit size keys.
	private KeyPair generateKeys() {
		KeyPairGenerator kpg = null;

		try {
			kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		Logger.INSTANCE.log("KEYS SUCCESSFULLY GENERATED");

		return kpg.generateKeyPair();
	}

	// Decrypts ciphertext using the private key. Returned in an optional in case of failure, so that caller can
	// be null-safe.
	Optional<String> decryptCiphertext(String ciphertext, Key privateKey) {
		byte[] tempBytes = null;
		try {
			Cipher cipher = Cipher.getInstance(encryptionMode);
			cipher.init(Cipher.DECRYPT_MODE, privateKey);

			// Ciphertext decoding
			byte[] secretText_decode = Base64.getDecoder().decode(ciphertext.getBytes());
			tempBytes = cipher.doFinal(secretText_decode);
		} catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
			e.printStackTrace();
		}
		return Optional.of(new String(tempBytes));
	}

	// Calculates the MAC using the HMAC algorithm with SHA256. Returned in an optional in case of failure, so
	// that caller can be null-safe.
	Optional<String> calculateMac(String message) {
		byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		byte[] msg = message.getBytes(StandardCharsets.UTF_8);
		byte[] hmacSha256 = null;

		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
			hmacSha256 = mac.doFinal(msg);
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			e.printStackTrace();
		}

		// Convert to hex.
		return Optional.of(convertByteToHex(hmacSha256));
	}

	// Verifies the authenticity of the two received MACs by checking the similarity between the base64 string
	// representations of the MACs.
	boolean verifyAuthenticity(String sentMac, String calculatedMac) {
		boolean authenticity = sentMac.equals(calculatedMac);

		Logger.INSTANCE.log("## MAC VERIFICATION ##");
		Logger.INSTANCE.log("MAC RECEIVED: " + sentMac);
		Logger.INSTANCE.log("MAC CALCULATED: " + calculatedMac);
		Logger.INSTANCE.log("## AUTHENTIC: " + authenticity + " ##");

		return authenticity;
	}

	// Converts an array of bytes into a String of hexadecimal characters, where each byte transforms into two hex chars.
	private String convertByteToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) sb.append(String.format("%02X", b));

		return sb.toString();
	}

	// Getters
	PublicKey getPublicKey() {
		return publicKey;
	}

	PrivateKey getPrivateKey() {
		return privateKey;
	}
}
