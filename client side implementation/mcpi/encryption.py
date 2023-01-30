import base64
import hmac
import hashlib

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

# ASSIGNMENT 3 ADDITION - Created by Huntermuze

"""
A simple class that handles the execution of the encryption including formulating the public key, encrypting messages,
and calculating the mac.
"""
class Encryption:
    # This secret key will be used for MAC encryption. Since the Diffie-Hellman key exchange and TLS handshake did not
    # work well with ECC & AES, it was decided to just generate a symmetric key using AES and hard-code it. Whilst
    # security implications exist for such decision, it was worth the trade-off because we were not making any progress
    # due to the lack of time with the other methods.
    SECRET_KEY = bytes('iid2SyjaVH2zSbfVByb7mEeaQikSmMeE9YdOq8Y1kPQ=', 'utf-8')

    def __init__(self, public_key_string):
        self.public_key = self._generate_public_key(public_key_string)

    # Calculates the MAC of the encrypted ciphertext (using Encrypt-then-Mac algorithm) => uses HMAC sha256 algorithm.
    def calculate_mac(self, msg):
        return hmac.new(Encryption.SECRET_KEY, msg, hashlib.sha256).hexdigest().upper()

    # Encrypts a message using the RSA asymmetric public key and SHA256 algorithm for encryption. Returns the
    # ciphertext in base64, as a series of bytes (array of bytes) for interoperability with the MAC hash.
    def encrypt_message(self, msg):
        ciphertext = self.public_key.encrypt(
           bytes(msg, 'utf-8'),
           padding.OAEP(
               mgf=padding.MGF1(algorithm=hashes.SHA1()),
               algorithm=hashes.SHA256(),
               label=None))
        return base64.b64encode(ciphertext)

    # Formulates the public key by de-serializing an inputted string, representing a key in base64. It then, creates a
    # public key object out of the string representation, which is to be used for later encryption in the cryptography lib.
    def _generate_public_key(self, public_key_string):
        public_key = "-----BEGIN PUBLIC KEY-----\n" + public_key_string + "\n-----END PUBLIC KEY-----\n"
        return serialization.load_pem_public_key(public_key.encode("utf-8"))
