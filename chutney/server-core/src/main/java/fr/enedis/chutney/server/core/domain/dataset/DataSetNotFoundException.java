/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.dataset;

public class DataSetNotFoundException extends RuntimeException {
    public DataSetNotFoundException(String id) {
        super(buildMessageFromId(id));
    }

    public DataSetNotFoundException(String id, Throwable throwable) {
        super(buildMessageFromId(id), throwable);
    }

    private static String buildMessageFromId(String id) {
        return "Dataset [" + id + "] could not be found";
    }
}
