/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.enedis.chutney.action.common.SecurityUtils;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

public class DefaultMongoDatabaseFactory implements MongoDatabaseFactory {

    private static final String DATABASE_NAME_PROPERTY = "databaseName";
    private static final String DATASOURCE_PREFIX = "datasource.";

    public CloseableResource<MongoDatabase> create(Target target) throws IllegalArgumentException {
        String databaseName = target.property(DATABASE_NAME_PROPERTY).orElse("");
        if (StringUtils.isEmpty(databaseName)) {
            throw new IllegalArgumentException(String.format("Missing Target property '%s'", DATABASE_NAME_PROPERTY));
        }
        String connectionString = String.format("mongodb://%s%s:%d/%s", credentials(target), target.host(), target.port(), queryParams(target));
        MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder();
        mongoClientSettings.applyConnectionString(new ConnectionString(connectionString));
        target.keyStore().ifPresent(keystore ->
            mongoClientSettings.applyToSslSettings(builder -> {
                try {
                    builder
                        .enabled(true)
                        .context(SecurityUtils.buildSslContext(target).build());
                } catch (GeneralSecurityException e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            })
        );
        MongoClient mongoClient = MongoClients.create(mongoClientSettings.build());
        return CloseableResource.build(mongoClient.getDatabase(databaseName), mongoClient::close);
    }

    private String queryParams(Target target) {
        return target.prefixedProperties(DATASOURCE_PREFIX, true)
            .entrySet().stream()
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .map(entry -> entry.getKey().concat("=").concat(entry.getValue()))
            .collect(Collectors.collectingAndThen(
                Collectors.joining("&"),
                joined -> StringUtils.isNotEmpty(joined) ? "?".concat(joined) : joined
            ));
    }

    private String credentials(Target target) {
        return Stream.of(target.user(), target.userPassword())
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.collectingAndThen(
                    Collectors.joining(":"),
                    joined -> StringUtils.isNotEmpty(joined) ? joined.concat("@") : joined
                )
            );
    }

}
