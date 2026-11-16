<!--
  ~ SPDX-FileCopyrightText: 2017-2026 Enedis
  ~
  ~ SPDX-License-Identifier: Apache-2.0
  ~
  -->

# Client key stores for mutual TLS tests

Self-signed client certificates (extended key usage `clientAuth`), valid 20 years from November 2026.
Used by `AmqpSslTest`.

| File                     | Type | Store password | Key alias | Key password |
|:-------------------------|:-----|:---------------|:----------|:-------------|
| `client.jks`             | JKS  | `client`       | `client`  | `client`     |
| `client-with-keypwd.jks` | JKS  | `client`       | `client`  | `key_pwd`    |

`client.jks` is also the trust store of the test broker. It holds both client certificates as trusted entries,
under the aliases `client-cert` and `client-with-keypwd-cert`.

## Generation

```shell
keytool -genkeypair -alias client -keyalg RSA -keysize 2048 -validity 7300 \
  -dname "CN=client, OU=CHUTNEY TEST, O=CHUTNEY TEST, L=Paris, ST=France, C=FR" \
  -ext eku=clientAuth -storetype JKS -keystore client.jks -storepass client -keypass client

keytool -genkeypair -alias client -keyalg RSA -keysize 2048 -validity 7300 \
  -dname "CN=client with key password, OU=CHUTNEY TEST, O=CHUTNEY TEST, L=Paris, ST=France, C=FR" \
  -ext eku=clientAuth -storetype JKS -keystore client-with-keypwd.jks -storepass client -keypass key_pwd

keytool -exportcert -alias client -keystore client.jks -storepass client -rfc -file client.pem
keytool -importcert -noprompt -alias client-cert -file client.pem -keystore client.jks -storepass client

keytool -exportcert -alias client -keystore client-with-keypwd.jks -storepass client -rfc -file client-with-keypwd.pem
keytool -importcert -noprompt -alias client-with-keypwd-cert -file client-with-keypwd.pem -keystore client.jks -storepass client
```
