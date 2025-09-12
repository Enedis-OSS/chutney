#
# SPDX-FileCopyrightText: 2017-2024 Enedis
#
# SPDX-License-Identifier: Apache-2.0
#
#

#!/usr/bin/env bash
# One shot script
# To be manually executed
# Minimal cert generation for MongoDB tests
# Keeps ONLY: server.pem, ca.pem, client.keystore.jks, client.truststore.jks
# Deletes all intermediate/unwanted files, echoing each deletion.

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
need keytool   # Required to produce JKS outputs

mkdir -p "$OUT_DIR"
cd "$OUT_DIR"

echo ">>> Cleaning previous artifacts…"
rm -f ca.{key,pem,srl} \
      server.{key,csr,crt,pem,cnf,srl} \
      client.{key,csr,crt,pem,cnf,srl} \
      client.keystore.{p12,jks} client.truststore.jks

# ---------- 1) Root CA ----------
echo ">>> Generating CA…"
openssl genrsa -out ca.key 4096
openssl req -x509 -new -nodes -key ca.key -sha256 -days "$DAYS_CA" \
  -subj "/C=$C/ST=$ST/L=$L/O=$O/OU=$OU/CN=$CA_CN" \
  -addext "basicConstraints=critical,CA:TRUE" \
  -addext "keyUsage=critical,keyCertSign,cRLSign" \
  -out ca.pem
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

# Check modulus match
SRV_KEY_MD5=$(openssl rsa  -in server.key  -noout -modulus | openssl md5 | awk '{print $2}')
SRV_CRT_MD5=$(openssl x509 -in server.crt -noout -modulus | openssl md5 | awk '{print $2}')
[[ "$SRV_KEY_MD5" == "$SRV_CRT_MD5" ]] || { echo "ERROR: server cert/key modulus mismatch"; exit 1; }

# Build server.pem (cert + key) for MongoDB's --tlsCertificateKeyFile
cat server.crt server.key > server.pem

# ---------- 3) Client cert (for JKS keystore) ----------
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

# Check modulus match
CLI_KEY_MD5=$(openssl rsa  -in client.key  -noout -modulus | openssl md5 | awk '{print $2}')
CLI_CRT_MD5=$(openssl x509 -in client.crt -noout -modulus | openssl md5 | awk '{print $2}')
[[ "$CLI_KEY_MD5" == "$CLI_CRT_MD5" ]] || { echo "ERROR: client cert/key modulus mismatch"; exit 1; }

# ---------- 4) Build client keystore/truststore (JKS only) ----------
echo ">>> Building client PKCS#12 and JKS keystores/truststore…"

# Try exporting PKCS#12 WITH chain; if that fails (OpenSSL variants), retry WITHOUT chain
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

# Convert to JKS keystore
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

# ---------- 5) Show Client DN BEFORE cleanup ----------
echo
echo "Client DN (use as \$external user):"
openssl x509 -in client.crt -noout -subject -nameopt RFC2253 | sed 's/^subject= //'

# ---------- 6) Cleanup (simple loop; echo each deletion) ----------
echo
echo ">>> Cleaning up intermediates and unwanted files…"

# Only list files you want to remove here (keep-set is implied by omission)
all_candidates=(
  ca.key ca.srl
  server.key server.csr server.crt server.cnf server.srl
  client.key client.csr client.crt client.cnf client.srl
  client.keystore.p12
  client.pem
  tmp.* *.old
)

for f in "${all_candidates[@]}"; do
  for path in $f; do
    if [ -e "$path" ]; then
      echo "Deleting $path"
      rm -f "$path"
    fi
  done
done

# ---------- 7) Summary ----------
echo
echo "=== SUCCESS === Generated in: $(pwd)"
ls -l server.pem ca.pem client.keystore.jks client.truststore.jks || true

echo
echo "MongoDB server flags reminder:"
cat <<'EOT'
  mongod --tlsMode requireTLS \
    --tlsCertificateKeyFile /path/server.pem \
    --tlsCAFile /path/ca.pem
EOT

echo
echo "Java example (system properties):"
cat <<EOT
  -Djavax.net.ssl.keyStore=$(pwd)/client.keystore.jks \\
  -Djavax.net.ssl.keyStorePassword=$KEYSTORE_PASS \\
  -Djavax.net.ssl.trustStore=$(pwd)/client.truststore.jks \\
  -Djavax.net.ssl.trustStorePassword=$KEYSTORE_PASS
EOT

