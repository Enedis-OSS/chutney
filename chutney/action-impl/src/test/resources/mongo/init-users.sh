#
# SPDX-FileCopyrightText: 2017-2024 Enedis
#
# SPDX-License-Identifier: Apache-2.0
#
#

# copied to /docker-entrypoint-initdb.d/ inside mongo docker container

#!/usr/bin/env bash
set -euo pipefail

user='CN=client,OU=CLIENT CHUTNEY TEST,O=CHUTNEY TEST,L=Paris,ST=France,C=FR'

DN="$user" DB="local" mongosh --quiet --eval '
  const dn  = process.env.DN;
  const ext = db.getSiblingDB("$external");
  try {
    ext.createUser({ user: dn, roles: [{ role:"readWrite", db:process.env.DB }] });
    print("Created:", dn);
  } catch (e) {
    print("createUser failed (maybe exists):", e.codeName || e);
  }
  print("$external users:"); printjson(ext.getUsers());
'


