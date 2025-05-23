/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class StreamsTest {

    @Test
    public void build_a_finite_stream_based_on_enumeration() {
        Vector<String> items = new Vector<>(Arrays.asList("test1", "test2"));

        Stream<String> stringStream = Streams.toStream(items.elements());

        assertThat(stringStream.count()).isEqualTo(2);
    }
}
