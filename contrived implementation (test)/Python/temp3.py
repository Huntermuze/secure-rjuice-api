import cryptography
import base64
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding

private_key = ec.generate_private_key(ec.SECP384R1())
public_key = private_key.public_key()

pem_priv_key = private_key.private_bytes(encoding=serialization.Encoding.PEM,
                                         format=serialization.PrivateFormat.PKCS8,
                                         encryption_algorithm=serialization.NoEncryption())

pem_pub_key = public_key.public_bytes(encoding=serialization.Encoding.PEM,
                                      format=serialization.PublicFormat.SubjectPublicKeyInfo)


def encrypt(pub_key, plaintext):
    return pub_key.encrypt(plaintext,
                           padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()),
                                        algorithm=hashes.SHA256(),
                                        label=None))


def decrypt(priv_key, ciphertext):
    return priv_key.decrypt(ciphertext,
                            padding.OAEP(mgf=padding.MGF1(algorithm=hashes.SHA256()),
                                         algorithm=hashes.SHA256(),
                                         label=None))


if __name__ == '__main__':
    print(pem_priv_key)
    print(pem_pub_key)

    encrypted = encrypt(public_key, 'THIS IS SYMMETRIC KEY BROTHER')
    print(encrypted)
    decrypted = decrypt(private_key, encrypted)
    print(decrypted)

