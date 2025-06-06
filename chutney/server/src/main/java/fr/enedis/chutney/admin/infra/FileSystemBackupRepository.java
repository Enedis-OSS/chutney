/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.admin.infra;

import static fr.enedis.chutney.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import fr.enedis.chutney.admin.domain.Backup;
import fr.enedis.chutney.admin.domain.BackupNotFoundException;
import fr.enedis.chutney.admin.domain.BackupRepository;
import fr.enedis.chutney.server.core.domain.admin.Backupable;
import fr.enedis.chutney.server.core.domain.tools.ZipUtils;
import fr.enedis.chutney.tools.Try;
import fr.enedis.chutney.tools.file.FileUtils;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

@Component
public class FileSystemBackupRepository implements BackupRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemBackupRepository.class);

    static final Path ROOT_DIRECTORY_NAME = Paths.get("backups", "zip");
    static final String BACKUP_FILE_EXTENSION = ".zip";

    private final Path backupsRootPath;

    private final List<Backupable> backupables;




    public FileSystemBackupRepository(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String backupsRootPath,
                                      List<Backupable> backupables) {
        this.backupsRootPath = Paths.get(backupsRootPath).resolve(ROOT_DIRECTORY_NAME).toAbsolutePath();
        initFolder(this.backupsRootPath);

        this.backupables = backupables;
    }

    @Override
    public void getBackupData(String backupId, OutputStream outputStream) throws IOException {
        Path backupPath = backupsRootPath.resolve(backupId);
        try (ZipOutputStream zipOutPut = new ZipOutputStream(new BufferedOutputStream(outputStream, 4096))) {
            ZipUtils.compressDirectoryToZipfile(backupPath.getParent(), Paths.get(backupId), zipOutPut);
        } catch (FileNotFoundException fnfe) {
            throw new BackupNotFoundException(backupId);
        }
    }

    @Override
    public List<String> getBackupables() {
        return backupables.stream().map(Backupable::name).collect(Collectors.toList());
    }

    @Override
    public String save(Backup backup) {
        String backupId = backup.getId();

        LOGGER.info("Backup [{}] initiating", backupId);
        Path backupPath = backupsRootPath.resolve(backupId);
        Try.exec(() -> Files.createDirectory(backupPath)).runtime();

        backupables.stream()
            .filter(backupable -> backup.backupables.contains(backupable.name()))
            .forEach(backupable -> backup(backupPath,backupable));

        LOGGER.info("Backup [{}] completed", backupId);
        return backup.getId();
    }

    @Override
    public Backup read(String backupId) {
        Path backupPath = backupsRootPath.resolve(backupId);
        if (backupPath.toFile().exists()) {
            try {
                List<String> foundBackupables = backupables.stream()
                    .map(backupable -> backupable.name())
                    .filter(backupName -> backupPath.resolve(backupName + BACKUP_FILE_EXTENSION).toFile().exists())
                    .collect(Collectors.toList());
                return new Backup(backupId, foundBackupables);
            } catch (RuntimeException re) {
                throw new BackupNotFoundException(backupId);
            }
        } else {
            throw new BackupNotFoundException(backupId);
        }
    }

    @Override
    public void delete(String backupId) {
        Path backupPath = backupsRootPath.resolve(backupId);
        if (Files.exists(backupPath)) {
            Try.exec(() -> FileSystemUtils.deleteRecursively(backupPath)).runtime();
            LOGGER.info("Backup [{}] deleted", backupId);
        } else {
            throw new BackupNotFoundException(backupId);
        }
    }

    @Override
    public List<Backup> list() {
        List<Backup> backups = new ArrayList<>();
        FileUtils.doOnListFiles(backupsRootPath, pathStream -> {
            pathStream.forEach(path -> {
                try {
                    backups.add(read(path.getFileName().toString()));
                } catch (BackupNotFoundException bnfe) {
                    LOGGER.warn("Ignoring unparsable backup [{}]", path.getFileName().toString(), bnfe);
                }
            });
            return Void.TYPE;
        });
        backups.sort(Comparator.comparing(b -> ((Backup) b).time).reversed());
        return backups;
    }

    private void backup(Path packupPath, Backupable backupable) {
        try (OutputStream outputStream = Files.newOutputStream(packupPath.resolve(backupable.name() + BACKUP_FILE_EXTENSION))) {
            backupable.backup(outputStream);
        } catch (Exception e) {
            LOGGER.error("Cannot backup [{}]", backupable.name(), e);
        }
        LOGGER.info("Backup [{}] completed", backupable.name());
    }

}
