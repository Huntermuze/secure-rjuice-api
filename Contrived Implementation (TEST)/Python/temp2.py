import os
import base64
import hashlib
from Crypto.Util.Padding import unpad
from Crypto.Cipher import AES
from tinyec import registry
from Crypto.PublicKey import ECC

CURVE = registry.get_curve('brainpoolP256r1')


# AES 'pad' byte array to multiple of BLOCK_SIZE bytes
def pad(byte_array):
    BLOCK_SIZE = 16
    pad_len = BLOCK_SIZE - len(byte_array) % BLOCK_SIZE
    return byte_array + (bytes([pad_len]) * pad_len)


# Remove padding at end of byte array
def unpad(byte_array):
    last_byte = byte_array[-1]
    return byte_array[0:-last_byte]


def encrypt(key, message):
    """as
    Input String, return base64 encoded encrypted String
    """

    byte_array = message.encode("UTF-8")

    padded = pad(byte_array)

    # generate a random iv and prepend that to the encrypted result.
    # The recipient then needs to unpack the iv and use it.
    iv = os.urandom(AES.block_size)
    cipher = AES.new(key.encode("UTF-8"), AES.MODE_CBC, iv)
    encrypted = cipher.encrypt(padded)
    # Note we PREPEND the unencrypted iv to the encrypted message
    return base64.b64encode(iv + encrypted).decode("UTF-8")


def decrypt(key, message):
    """
    Input encrypted bytes, return decrypted bytes, using iv and key
    """

    byte_array = base64.b64decode(message)

    iv = byte_array[0:16]  # extract the 16-byte initialization vector

    messagebytes = byte_array[16:]  # encrypted message is the bit after the iv

    cipher = AES.new(key.encode("UTF-8"), AES.MODE_CBC, iv)

    decrypted_padded = cipher.decrypt(messagebytes)

    decrypted = unpad(decrypted_padded)

    return decrypted.decode("UTF-8")


def sanitise_key(key):
    blacklisted = {'-----BEGIN PUBLIC KEY-----', '-----END PUBLIC KEY-----', '-----BEGIN PRIVATE KEY-----',
                   '-----BEGIN PUBLIC KEY-----'}
    for x in blacklisted:
        key = key.replace(x, '')

    return key.strip()


if __name__ == '__main__':
    ecc_object = ECC.generate(curve='P-256')
    # Used solely to decrypt.
    ecc_priv_key = hashlib.md5(sanitise_key(ecc_object.export_key(format='PEM')).encode()).hexdigest()
    # Used solely to encrypt.
    ecc_pub_key = hashlib.md5(sanitise_key(ecc_object.public_key().export_key(format='PEM')).encode()).hexdigest()

    print('PRIV KEY:', ecc_priv_key)
    print('PUB KEY:', ecc_pub_key)
    encrypted_symm_key = input('Enter the encrypted symmetric key!\n')
    encrypted_symm_key = encrypt(ecc_pub_key, encrypted_symm_key)
    print('Decrypted symmetric key:', decrypt(ecc_priv_key, encrypted_symm_key))