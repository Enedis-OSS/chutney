/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.mongo;

import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.tools.CloseableResource;
import com.mongodb.client.MongoDatabase;

public interface MongoDatabaseFactory {

    /**
     * @throws IllegalArgumentException when given {@link Target} does not supply needed parameters
     */
    CloseableResource<MongoDatabase> create(Target target) throws IllegalArgumentException;
}
