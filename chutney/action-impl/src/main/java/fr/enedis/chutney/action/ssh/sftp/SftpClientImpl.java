/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.sftp;

import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;

import fr.enedis.chutney.action.spi.injectable.Logger;
import fr.enedis.chutney.action.spi.injectable.Target;
import fr.enedis.chutney.action.ssh.SshClientFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.client.SftpErrorDataHandler;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;

public class SftpClientImpl implements ChutneySftpClient {

    private final ClientSession session;
    private final SftpClient sftpClient;

    private SftpClientImpl(ClientSession session, SftpClient sftpClient) {
        this.session = session;
        this.sftpClient = sftpClient;
    }

    @Override
    public void upload(String source, String destination) throws IOException {
        Path file = Paths.get(source);
        byte[] fileContent = Files.readAllBytes(file);

        try (BufferedOutputStream out = new BufferedOutputStream(sftpClient.write(destination))) {
            out.write(fileContent);
        }
    }

    @Override
    public void download(String source, String destination) throws IOException {
        try (InputStream read = sftpClient.read(source)) {
            byte[] fileContent = read.readAllBytes();
            File file = new File(destination);
            file.getParentFile().mkdirs();

            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                out.write(fileContent);
            }
        }
    }

    @Override
    public List<String> listDirectory(String directory) throws IOException {
        SftpClient.Handle handle = sftpClient.openDir(directory);
        Iterable<SftpClient.DirEntry> files = sftpClient.listDir(handle);

        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(files.iterator(), Spliterator.ORDERED), false)
            .map(SftpClient.DirEntry::getFilename)
            .filter(f -> !".".equals(f) && !"..".equals(f))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getAttributes(String file) throws IOException {
        SftpClient.Attributes stat = sftpClient.stat(file);
        return Map.of(
            "CreationDate", ofInstant(stat.getCreateTime().toInstant(), systemDefault()),
            "lastAccess", ofInstant(stat.getAccessTime().toInstant(), systemDefault()),
            "lastModification", ofInstant(stat.getModifyTime().toInstant(), systemDefault()),
            "type", FileType.from(stat).label,
            "owner:group", stat.getOwner() + ":" + stat.getGroup()
        );
    }

    @Override
    public void close() throws Exception {
        sftpClient.close();
        session.close();
    }

    public static ChutneySftpClient buildFor(Target target, long timeout, Logger logger) throws IOException {
        ClientSession session = SshClientFactory.buildSSHClientSession(target, timeout);
        return new SftpClientImpl(session, buildSftpClient(session, logger));
    }

    private static SftpClient buildSftpClient(ClientSession session, Logger logger) throws IOException {
        SftpClientFactory factory = DefaultSftpClientFactory.INSTANCE;
        SftpClient client = factory.createSftpClient(session, new ActionSftpErrorDataHandler(logger));
        return client.singleSessionInstance();
    }

    /*
     * According to SFTP version 4 - section 3.1 the server MAY send error data through the STDERR pipeline.
     * By default, the code ignores such data.
     * However, users may register a SftpErrorDataHandler that will be invoked whenever such data is received from the server.
     * */
    private static class ActionSftpErrorDataHandler implements SftpErrorDataHandler {

        private final Logger logger;

        public ActionSftpErrorDataHandler(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void errorData(byte[] buf, int start, int len) {
            logger.error(new String(buf, StandardCharsets.UTF_8));
        }

    }

    private enum FileType {

        FILE("regular file"),
        DIRECTORY("directory"),
        SYMBOLIC_LINK("symbolic link"),
        OTHER("other");

        public final String label;

        FileType(String label) {
            this.label = label;
        }

        static FileType from(SftpClient.Attributes stat) {
            if (stat.isRegularFile()) return FILE;
            if (stat.isDirectory()) return DIRECTORY;
            if (stat.isSymbolicLink()) return SYMBOLIC_LINK;
            return OTHER;
        }
    }
}
