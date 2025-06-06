/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.api.dto;

import fr.enedis.chutney.admin.domain.Backup;
import java.util.List;
import java.util.stream.Collectors;

public class BackupMapper {

    public static Backup fromDto(BackupDto dto) {
        return new Backup(dto.getBackupables());
    }

    public static BackupDto toDto(Backup backup) {
        return new BackupDto(backup.time, backup.backupables);
    }

    public static List<BackupDto> toDtos(List<Backup> backups) {
        return backups.stream()
            .map(BackupMapper::toDto)
            .collect(Collectors.toList());
    }
}
