/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package blackbox;

import fr.enedis.chutney.tools.file.FileUtils;
import java.nio.file.Path;

final class BlackboxWorkspace {

    private static boolean cleaned;

    private BlackboxWorkspace() {
    }

    // Blackbox tests share one cached Spring context: deleting its workspace once it has started breaks its file stores.
    static synchronized void cleanOnce() {
        if (!cleaned) {
            FileUtils.deleteFolder(Path.of("./target/.chutney"));
            cleaned = true;
        }
    }
}
