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
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.ReflectionSupport;

class FindMissingActionsTest {

    @Test
    void should_find_missing_actions_in_meta_inf_file() throws IOException {

        List<Class<?>> allClassesInPackage = ReflectionSupport.findAllClassesInPackage("fr.enedis.chutney.action",
            (clazz) -> !Modifier.isAbstract(clazz.getModifiers()) && ClassUtils.getAllInterfaces(clazz).contains(Action.class),
            (str) -> true);
        Set<String> actionsByReflection = allClassesInPackage
            .stream()
            .map(Class::getCanonicalName)
            .collect(Collectors.toSet());

        Path path;

        try {
            path = Path.of(Objects.requireNonNull(
                this.getClass().getClassLoader().getResource("META-INF/extension/chutney.actions"))
                .toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try(Stream<String> lines = Files.lines(path)) {
            Set<String> actionsFromFile = lines.collect(Collectors.toSet());
            assertThat(actionsFromFile).containsExactlyInAnyOrderElementsOf(actionsByReflection);
        }

    }
}
