import base64

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
import hmac
import hashlib

secret_key = 'iid2SyjaVH2zSbfVByb7mEeaQikSmMeE9YdOq8Y1kPQ='


def calculate_mac(key, msg):
    return hmac.new(key, msg, hashlib.sha256).hexdigest().upper()


def generate_public_key(pub_input):
    public_key = "-----BEGIN PUBLIC KEY-----\n" + pub_input + "\n-----END PUBLIC KEY-----\n"
    return serialization.load_pem_public_key(public_key.encode("utf-8"))


def encrypt(msg, pub_key):
    ciphertext = pub_key.encrypt(
        msg,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA1()),
            algorithm=hashes.SHA256(),
            label=None))
    return base64.b64encode(ciphertext)


if __name__ == '__main__':
    public_key = generate_public_key(input('Enter public key: '))
    message = b'I do not like this subject'
    ciphertext = encrypt(message, public_key)
    mac = calculate_mac(bytes(secret_key, 'utf-8'), ciphertext)

    print('Cipher:', ciphertext.decode('utf-8'))
    print('MAC:', mac)
