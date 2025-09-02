#!/usr/bin/env bash
set -euo pipefail

# ================= Config (override via env) =================
: "${OUT_DIR:=certs}"
: "${DAYS_CA:=3650}"    # ~10 years
: "${DAYS_LEAF:=825}"   # ~27 months

# Subject fields
: "${C:=FR}"
: "${ST:=France}"
: "${L:=Paris}"
: "${O:=CHUTNEY TEST}"
: "${OU:=CHUTNEY TEST}"
: "${CLIENT_OU:=CLIENT CHUTNEY TEST}"
: "${CA_CN:=dev-root-ca}"
: "${SERVER_CN:=server}"
: "${CLIENT_CN:=client}"

# Server SANs
: "${SERVER_DNS_1:=localhost}"
: "${SERVER_DNS_2:=mongo}"
: "${SERVER_IP_1:=127.0.0.1}"
: "${SERVER_IP_2:=::1}"

# Java keystore/truststore settings
: "${KEYSTORE_PASS:=server}"
: "${ALIAS_CLIENT:=client}"
: "${ALIAS_CA:=dev-root-ca}"
# ============================================================

need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing dependency: $1" >&2; exit 1; }; }
need openssl

mkdir -p "$OUT_DIR"
cd "$OUT_DIR"

echo ">>> Cleaning previous artifacts…"
rm -f ca.{key,pem,srl} server.{key,csr,crt,pem,cnf,srl} client.{key,csr,crt,pem,cnf,srl} \
      client.keystore.p12 client.keystore.jks client.truststore.jks

# ---------- 1) Root CA ----------
echo ">>> Generating CA…"
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days "$DAYS_CA" \
  -subj "/C=$C/ST=$ST/L=$L/O=$O/OU=$OU/CN=$CA_CN" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -out ca.pem
chmod 600 ca.key
openssl x509 -in ca.pem -noout -text | grep -q "CA:TRUE" || { echo "CA not marked as CA:TRUE"; exit 1; }

# ---------- 2) Server cert ----------
cat > server.cnf <<EOF
[ req ]
default_bits       = 4096
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = v3_req

[ dn ]
C  = $C
ST = $ST
L  = $L
O  = $O
OU = $OU
CN = $SERVER_CN

[ v3_req ]
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[ alt_names ]
DNS.1 = $SERVER_DNS_1
DNS.2 = $SERVER_DNS_2
IP.1  = $SERVER_IP_1
IP.2  = $SERVER_IP_2
EOF

echo ">>> Generating server key + CSR…"
openssl req -new -newkey rsa:4096 -nodes -keyout server.key -out server.csr -config server.cnf

echo ">>> Signing server certificate…"
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out server.crt -days "$DAYS_LEAF" -sha256 -extfile server.cnf -extensions v3_req

# Reliable RSA check: compare modulus (not DER SPKI)
SRV_KEY_MD5=$(openssl rsa  -in server.key  -noout -modulus | openssl md5 | awk '{print $2}')
SRV_CRT_MD5=$(openssl x509 -in server.crt -noout -modulus | openssl md5 | awk '{print $2}')
[[ "$SRV_KEY_MD5" == "$SRV_CRT_MD5" ]] || { echo "ERROR: server cert/key modulus mismatch"; exit 1; }

cat server.crt server.key > server.pem
chmod 600 server.key server.pem

# ---------- 3) Client cert ----------
cat > client.cnf <<EOF
[ req ]
default_bits       = 4096
prompt             = no
default_md         = sha256
distinguished_name = dn
req_extensions     = v3_req

[ dn ]
C  = $C
ST = $ST
L  = $L
O  = $O
OU = $CLIENT_OU
CN = $CLIENT_CN

[ v3_req ]
keyUsage = digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
EOF

echo ">>> Generating client key + CSR…"
openssl req -new -newkey rsa:4096 -nodes -keyout client.key -out client.csr -config client.cnf

echo ">>> Signing client certificate…"
openssl x509 -req -in client.csr -CA ca.pem -CAkey ca.key -CAcreateserial \
  -out client.crt -days "$DAYS_LEAF" -sha256 -extfile client.cnf -extensions v3_req

# Reliable RSA check: compare modulus
CLI_KEY_MD5=$(openssl rsa  -in client.key  -noout -modulus | openssl md5 | awk '{print $2}')
CLI_CRT_MD5=$(openssl x509 -in client.crt -noout -modulus | openssl md5 | awk '{print $2}')
[[ "$CLI_KEY_MD5" == "$CLI_CRT_MD5" ]] || { echo "ERROR: client cert/key modulus mismatch"; exit 1; }

cat client.crt client.key > client.pem
chmod 600 client.key client.pem

# ---------- 4) Build client keystore/truststore ----------
echo ">>> Building client PKCS#12 and JKS keystores/truststore…"

# PKCS#12 (keystore). Try with chain; if OpenSSL refuses, fall back without chain.
if ! openssl pkcs12 -export \
  -in client.crt -inkey client.key \
  -name "$ALIAS_CLIENT" \
  -out client.keystore.p12 \
  -passout pass:"$KEYSTORE_PASS" \
  -certfile ca.pem -caname "$ALIAS_CA" -chain; then
  echo "    (pkcs12 export with chain failed; retrying without chain)"
  openssl pkcs12 -export \
    -in client.crt -inkey client.key \
    -name "$ALIAS_CLIENT" \
    -out client.keystore.p12 \
    -passout pass:"$KEYSTORE_PASS"
fi

# If keytool exists, create JKS keystore/truststore too
if command -v keytool >/dev/null 2>&1; then
  keytool -importkeystore \
    -srckeystore client.keystore.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass "$KEYSTORE_PASS" \
    -destkeystore client.keystore.jks \
    -deststoretype JKS \
    -deststorepass "$KEYSTORE_PASS" \
    -noprompt

  # Truststore with CA
  keytool -importcert \
    -alias "$ALIAS_CA" \
    -file ca.pem \
    -keystore client.truststore.jks \
    -storepass "$KEYSTORE_PASS" \
    -noprompt

  echo ">>> JKS contents (brief):"
  keytool -list -keystore client.keystore.jks -storepass "$KEYSTORE_PASS" | sed -n '1,40p' || true
  keytool -list -keystore client.truststore.jks -storepass "$KEYSTORE_PASS" | sed -n '1,40p' || true
else
  echo "!!! keytool not found. Skipping JKS generation. You can use client.keystore.p12 directly."
fi

# ---------- 5) Summary ----------
echo
echo "=== SUCCESS === Generated in: $(pwd)"
ls -l

echo
echo "Client DN (use as \$external user):"
openssl x509 -in client.crt -noout -subject -nameopt RFC2253 | sed 's/^subject= //'

echo
echo "Java example (system properties):"
cat <<EOT
  -Djavax.net.ssl.keyStore=$(pwd)/client.keystore.jks \\
  -Djavax.net.ssl.keyStorePassword=$KEYSTORE_PASS \\
  -Djavax.net.ssl.trustStore=$(pwd)/client.truststore.jks \\
  -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS
EOT

echo
echo "MongoDB server flags reminder:"
cat <<'EOT'
  mongod --tlsMode requireTLS \
    --tlsCertificateKeyFile /path/server.pem \
    --tlsCAFile /path/ca.pem
EOT
