import random
import binascii
import hashlib
import secrets
import base64
from Crypto.Util.Padding import unpad
from Crypto.Cipher import AES
from tinyec import registry
from Crypto.PublicKey import ECC

CURVE = registry.get_curve('brainpoolP256r1')


def generate_student_ids():
    student_id_list = ['2', '2', '1', '9', '0', '4', '9', '7']
    len_list = len(student_id_list)
    filler_num = 1
    student_string = ''

    for num in range(len_list):
        element = random.randint(0, len_list - filler_num)
        student_string += student_id_list[element]
        student_id_list.pop(element)
        filler_num += 1

    return student_string


def decrypt_aes(encrypted_symm_key, priv_key, iv):
    generator = AES.new(priv_key.encode(), AES.MODE_CBC, iv.encode())
    recovery = generator.decrypt(base64.b64decode(encrypted_symm_key))
    return unpad(recovery, AES.block_size)


ecc_object = ECC.generate(curve='P-256')
# Used solely to decrypt.
ecc_priv_key = ecc_object.export_key(format='PEM')
# Used solely to encrypt.
ecc_pub_key = ecc_object.public_key().export_key(format='PEM')

print(ecc_priv_key)
print(ecc_pub_key)
encrypted_symm_key = input('Enter the encrypted symmetric key!\n')
print('Decrypted symmetric key:', decrypt_aes(encrypted_symm_key, ecc_priv_key, "\x00"*AES.block_size))
