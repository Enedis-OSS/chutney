/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class Backup {

    public final static DateTimeFormatter backupIdTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public final LocalDateTime time;
    public final List<String> backupables;

    public Backup(List<String> backupables) {
        if (backupables == null || backupables.isEmpty()) {
            throw new IllegalArgumentException("Nothing to backup !!");
        }

        this.time = getLocalDateTime();
        this.backupables = backupables;
    }

    public Backup(String id, List<String> backupables) {
        this.time = toLocalDate(id);
        this.backupables = backupables;
    }

    private LocalDateTime toLocalDate(String id) {
        return StringUtils.isNotBlank(id) ? LocalDateTime.parse(id, backupIdTimeFormatter) : getLocalDateTime();
    }

    private LocalDateTime getLocalDateTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public String getId() {
        return this.time.format(backupIdTimeFormatter);
    }
}
