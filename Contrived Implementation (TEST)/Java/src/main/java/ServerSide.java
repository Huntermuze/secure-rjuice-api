import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;

public class ServerSide {
	public static void main(String[] args) throws Exception {
		SecretKey symmKey = null;

		try {
			symmKey = generateKey(256);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		Base64.Encoder encoder = Base64.getEncoder();
		String symmKeyStr = encoder.encodeToString(symmKey.getEncoded());

		System.out.println(symmKeyStr);

		Scanner scnr = new Scanner(System.in);


		String encryptedSymmKey = encrypt(symmKeyStr, transformPubKey(scnr.nextLine()));
		System.out.println(encryptedSymmKey);
	}

	public static byte[] encBytes(byte[] srcBytes, byte[] key, byte[] newIv) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
		IvParameterSpec iv = new IvParameterSpec(newIv);
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		return cipher.doFinal(srcBytes);
	}

	public static String encrypt(String plaintext, String pubKey) throws Exception {
		byte[] key = pubKey.getBytes(StandardCharsets.UTF_8);
		byte[] ivk = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		byte[] srcBytes = plaintext.getBytes(StandardCharsets.UTF_8);
		byte[] encrypted = encBytes(srcBytes, key, ivk);
		return Base64.getEncoder().encodeToString(encrypted);
	}

	// This will hash the public key obtained by python using the MD5 algorithm because the AES algorithm only accepts
	// keys with lengths 16, 24 or 32 bytes, but the ECC public key is 256 bytes long. Hence, we hash it with MD5, and
	// that produces a 24 byte hash.
	public static String transformPubKey(String pubKey) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest digester = MessageDigest.getInstance("MD5");
		digester.update(pubKey.getBytes(StandardCharsets.UTF_8));
		byte[] key = digester.digest();
		return Base64.getEncoder().encodeToString(key);
	}

	public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(n);
		return keyGenerator.generateKey();
	}
}
