/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.server.core.domain.scenario;

public interface TestCase {

    TestCaseMetadata metadata();

    default String id() {
        return metadata().id();
    }
}
