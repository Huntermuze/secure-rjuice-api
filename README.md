# Minecraft Secure (CCA) API

  - build an efficient Chosen Ciphertext Attack (CCA) secure protocol
    - uses blockcipher to provide confidentiality and message integrity at the same time
    - Transmitting encrypted values to a remote machine via an open channel, i.e. an insecure 
    - Decrypting those values at the destination and ensuring their correctness.

  - Use a WLAN sniffing tool to establish the security weaknesses of the original Python API that you have been using in Assignment 1 and 2
  - demonstrate this via a video submission detailed in Phase 3.
In this phase you will need to design an efficient CCA secure protocol (which uses blockcipher) technique to simultaneously achieve confidentiality and message integrity. Note that you can choose and **implement only one** technique.

### Symmetric
  - DES
    - Defeated
    - 3DES
      - Replaced by AES
  - AES
    - Trusted as the standard by the U.S. Government
    - Considered impervious to all attacks, except for brute force
    - AES-128
    - AES-192
    - AES-256
  - Blowfish
    - Slow in certain apps
    - Vulnerable to Birthday Attacks in HTTPS
    - Splits messages into blocks of 64 bits
    - Known for tremendous speed
    - Free availability in the public domain
  - Twofish
    - Symmetric
    - Keys up to 256 bits in length
  - QUAD
  - RC
    - 2, 4, 5, 6 - Known vulnerabilities. Doesn't seem that reliable

### Asymmetric
  - Diffie-Hellman
    - NIST recommends RSA for lesser CPU power and smaller data exchange for digital signing and SSL
  - DSA
  - ElGamal
  - PKCS
  - RSA
    - slow
    - often used to encrypt/decrypt the symmetric keys
  - ECC (Elliptic Curve Cryptography)
    - alternative to RSA
      - offer same level of strength
      - smaller key sizes
      - reduced computational and storage requirements


## Usage
For instructions on installing our code with the implemented security technique, please look in the folder ``phase_3`` and open the ``Installation Instructions`` file.
