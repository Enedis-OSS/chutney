/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.ssh.sftp;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ChutneySftpClient extends AutoCloseable {

    void upload(String source, String destination) throws IOException;

    void download(String source, String destination) throws IOException;

    List<String> listDirectory(String directory) throws IOException;

    Map<String, Object> getAttributes(String file) throws IOException;

}
