/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.spi.time;

import java.util.Optional;

public interface DurationParser {

    Optional<Duration> parse(String literalDuration);

    String description();
}
