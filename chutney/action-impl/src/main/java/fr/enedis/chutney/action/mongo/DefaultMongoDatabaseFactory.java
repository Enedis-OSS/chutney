/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import fr.enedis.chutney.action.common.SecurityUtils;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.security.GeneralSecurityException;
import org.apache.commons.lang3.StringUtils;

public class DefaultMongoDatabaseFactory implements MongoDatabaseFactory {

    public CloseableResource<MongoDatabase> create(Target target) throws IllegalArgumentException {
        String databaseName = target.property("databaseName").orElse("");
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException("Missing Target property 'databaseName'");
        }

        String connectionString = String.format("mongodb://%s:%d/", target.host(), target.port());

        final MongoClient mongoClient;
        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        target.keyStore().ifPresent(keystore ->
            mongoClientSettings.applyToSslSettings(builder -> {
              try {
                builder
                  .invalidHostNameAllowed(true)
                  .enabled(true)
                  .context(SecurityUtils.buildSslContext(target).build());
              } catch (GeneralSecurityException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
              }
            })
        );
        mongoClientSettings.applyConnectionString(new ConnectionString(connectionString));
        if (target.user().isPresent()) {
            String user = target.user().get();
            String password = target.userPassword().orElse("");
            mongoClientSettings.credential(
                MongoCredential.createCredential(user, databaseName, password.toCharArray())
            );
        }
        mongoClient = MongoClients.create(mongoClientSettings.build());
        return CloseableResource.build(mongoClient.getDatabase(databaseName), mongoClient::close);
    }

}
