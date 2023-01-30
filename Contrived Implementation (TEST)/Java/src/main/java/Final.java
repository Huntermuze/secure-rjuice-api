import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.Scanner;

public class Final {

	private final String ENCRYPTION_MODE = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	private final String SECRET_KEY = "iid2SyjaVH2zSbfVByb7mEeaQikSmMeE9YdOq8Y1kPQ=";

	/***
	 * Generation of secret key by openssl
	 */

	private KeyPair generateKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);

		return kpg.generateKeyPair();
	}

	private String decrypt(String secretText, Key key) {
		try {
			//Generate public key
			Cipher cipher = Cipher.getInstance(ENCRYPTION_MODE);
			cipher.init(Cipher.DECRYPT_MODE, key);
			// Ciphertext decoding
			byte[] secretText_decode = Base64.getDecoder().decode(secretText.getBytes());
			byte tempBytes[] = cipher.doFinal(secretText_decode);
			String text = new String(tempBytes);
			return text;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private String calculateMac(String message) {
		byte[] secretKey = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
		byte[] msg = message.getBytes(StandardCharsets.UTF_8);

		byte[] hmacSha256;
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
			mac.init(secretKeySpec);
			hmacSha256 = mac.doFinal(msg);
		} catch (Exception e) {
			throw new RuntimeException("Failed to calculate hmac-sha256", e);
		}

		// Convert to hex.
		StringBuilder sb = new StringBuilder();
		for (byte b : hmacSha256) sb.append(String.format("%02X", b));

		return sb.toString();
	}

	private boolean verifyAuthenticity(String sentMac, String calculatedMac) {
		return sentMac.equals(calculatedMac);
	}


	public static void main(String[] args) throws Exception {
		Final rsa = new Final();
		KeyPair kp = rsa.generateKeys();

		PublicKey publicKey = kp.getPublic();
		PrivateKey privateKey = kp.getPrivate();

		String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		System.out.println("Public Key Base 64: " + publicKeyBase64);

		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter the ciphertext:");
		String ciphertext = scanner.nextLine();
		System.out.println("Enter the MAC:");
		String sentMac = scanner.nextLine();
		String calculatedMac = rsa.calculateMac(ciphertext);

		//Public key encryption, private key decryption
		System.out.println("Ciphertext: " + ciphertext);
		String plaintext = rsa.decrypt(ciphertext, privateKey);
		System.out.println("Plaintext: " + plaintext);
		System.out.println("Calculated Mac: " + calculatedMac);

		System.out.println("Are the macs equal: " + rsa.verifyAuthenticity(sentMac, calculatedMac));
	}
}
