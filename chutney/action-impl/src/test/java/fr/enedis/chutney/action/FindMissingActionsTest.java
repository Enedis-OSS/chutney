/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action;

import static org.assertj.core.api.Assertions.assertThat;

import fr.enedis.chutney.action.spi.Action;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

class FindMissingActionsTest {

    @TempDir
    static Path testFolder;

    @Test
    void should_find_missing_actions_in_meta_inf_file() throws IOException {

        var reflections = new Reflections(
            "fr.enedis.chutney.action", Scanners.SubTypes);
        Set<Class<? extends Action>> actionsSubTypes = reflections.getSubTypesOf(Action.class);

        Set<String> actionsByReflection = actionsSubTypes
            .stream()
            .filter(c -> !Modifier.isAbstract(c.getModifiers()))
            .map(Class::getCanonicalName)
            .collect(Collectors.toSet());

        var path = testFolder.resolve(this.getClass().getCanonicalName() + ".class");

        try(InputStream chutneyActionsStream = this.getClass().getClassLoader().getResourceAsStream("META-INF/extension/chutney.actions")) {
            assertThat(chutneyActionsStream).isNotNull();
            Files.copy(chutneyActionsStream, path, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try(Stream<String> lines = Files.lines(path)) {
            Set<String> actionsFromFile = lines.collect(Collectors.toSet());
            actionsFromFile.forEach(line -> assertThat(actionsByReflection).contains(line));
            actionsByReflection.forEach(line -> assertThat(actionsFromFile).contains(line));
        }

    }
}
