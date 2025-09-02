#
# SPDX-FileCopyrightText: 2017-2024 Enedis
#
# SPDX-License-Identifier: Apache-2.0
#
#

# /docker-entrypoint-initdb.d/10-create-x509-user.sh
#!/usr/bin/env bash
set -euo pipefail

# Create X.509 user (no auth yet)
mongosh --quiet --eval '
  const dn = "CN=client,OU=CLIENT CHUTNEY TEST,O=CHUTNEY TEST,L=Paris,ST=France,C=FR";
  const ext = db.getSiblingDB("$external");
  try {
    ext.createUser({ user: dn, roles: [{ role:"readWrite", db:"local" }] });
    print("✅ Created:", dn);
  } catch(e) {
    print("ℹ️ createUser failed (maybe exists):", e.codeName || e);
  }
  print("📋 $external users:"); printjson(ext.getUsers());
'
