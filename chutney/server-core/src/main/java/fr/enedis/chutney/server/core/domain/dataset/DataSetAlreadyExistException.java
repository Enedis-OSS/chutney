/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.dataset;

public class DataSetAlreadyExistException  extends RuntimeException {
    public DataSetAlreadyExistException(String name) {
        super("Dataset [" + name + "] already exists");
    }
}
