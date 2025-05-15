/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.domain;

@SuppressWarnings("serial")
public class BackupNotFoundException extends RuntimeException {

    public BackupNotFoundException(String backupId) {
        super("Backup [" + backupId + "] not found !");
    }

}
