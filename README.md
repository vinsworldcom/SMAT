# SSL Mutual Authentication Test - SMAT

## Overview

Key and certificate files are generated for self-signed certificate
authority (CA).  An intermdiate signing key and certificate is created and
used to sign the server and client certificates.

The keys and certificates are imported (`keytool`) to Java Key Store in
PKCS #12 format.  An OpenSSL server (`openssl s_server`) is opened and
the Java app connects to it through mutual authenticaiton.

## Prepare the Keys and Certificates

All the required example keys and certificates are found in the `ca/` 
directory.  They were created with my [`ssl-ca-make`](https://github.com/VinsWorldcom/ssl-ca-make) project.  
Passwords on the keys and keystores is 'password'.

## Import to Java Key Store

We need to create the Java Key stores for trust and identity from the keys and 
certificates in the `ca/` directory.

    cd ca

### Generate the TrustStore

    keytool -import -alias ServerTest -file ServerCert_signedByCAIntermediary.crt -keystore truststore.p12 -storetype pkcs12

### Generate the IdentityStore

In the IdentityStore will put our private key, our certificate and the CA
chain under an alias which our client is going to use to authenticate itself
with the server.

1. Concatenate all certificates into one PEM file

    ```
    cat server-chain.crt ClientCert_signedByCAIntermediary.crt ClientCert_signedByCAIntermediary.key > fullclient.crt
    ```

2. Generate the PKCS12 keystore

    ```
    openssl pkcs12 -export -in fullclient.crt -out fullclient.p12 -name ClientTest -noiter -nomaciter
    ```

3. Import the PKCS12 to IdentityStore.

    ```
    keytool -importkeystore -srckeystore fullclient.p12 -srcstoretype pkcs12 -srcalias ClientTest -destkeystore keystore.p12 -deststoretype pkcs12 -destalias ClientTest
    ```

## Run the Test

In a Command Prompt or PowerShell window:

    openssl s_server -CAfile ca\server-chain.crt -key ca\ServerCert_signedByCAIntermediary.key -cert ca\ServerCert_signedByCAIntermediary.crt -accept 44430 -www -Verify 3

In another PowerShell window:

    mvn package
    java -cp "$(cat cp.txt);target\*" com.VinsWorld.app.SMAT
