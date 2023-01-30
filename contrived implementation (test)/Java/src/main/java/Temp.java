import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class Temp {

	public static void main(String[] args) {
		SecretKey symmKey = null;

		try {
			symmKey = generateKey(256);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		Base64.Encoder encoder = Base64.getEncoder();
		String symmKeyStr = encoder.encodeToString(symmKey.getEncoded());

		Scanner scnr = new Scanner(System.in);

		System.out.println("Symmetric key: " + symmKeyStr);
		System.out.println("Encrypted Symmetric Key: " + encrypt(scnr.nextLine(), symmKeyStr));
	}

	// String plaintext -> Base64-encoded String ciphertext
	public static String encrypt(String key, String plaintext) {
		try {
			// Generate a random 16-byte initialization vector
			byte[] initVector = new byte[16];
			(new Random()).nextBytes(initVector);
			IvParameterSpec iv = new IvParameterSpec(initVector);

			// prep the key
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

			// prep the AES Cipher
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			// Encode the plaintext as array of Bytes
			byte[] cipherbytes = cipher.doFinal(plaintext.getBytes());

			// Build the output message initVector + cipherbytes -> base64
			byte[] messagebytes = new byte[initVector.length + cipherbytes.length];

			System.arraycopy(initVector, 0, messagebytes, 0, 16);
			System.arraycopy(cipherbytes, 0, messagebytes, 16, cipherbytes.length);

			// Return the cipherbytes as a Base64-encoded string
			return Base64.getEncoder().encodeToString(messagebytes);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	// Base64-encoded String ciphertext -> String plaintext
	public static String decrypt(String key, String ciphertext) {
		try {
			byte[] cipherbytes = Base64.getDecoder().decode(ciphertext);

			byte[] initVector = Arrays.copyOfRange(cipherbytes, 0, 16);

			byte[] messagebytes = Arrays.copyOfRange(cipherbytes, 16, cipherbytes.length);

			IvParameterSpec iv = new IvParameterSpec(initVector);
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

			// Convert the ciphertext Base64-encoded String back to bytes, and
			// then decrypt
			byte[] byte_array = cipher.doFinal(messagebytes);

			// Return plaintext as String
			return new String(byte_array, StandardCharsets.UTF_8);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(n);
		return keyGenerator.generateKey();
	}

}