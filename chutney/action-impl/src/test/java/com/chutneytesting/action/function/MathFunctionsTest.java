/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.action.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MathFunctionsTest {

    @Test
    void min() {
        assertThat(MathFunctions.min(-1, 2d, -6f, 100d, 5)).isEqualTo(-6);
    }

    @Test
    void max() {
        assertThat(MathFunctions.max(-1, 2d, -6f, 100d, 5)).isEqualTo(100);
    }

    @Nested
    @DisplayName("Math api")
    class MathApi {

        private static final MathFunctions.Maths sut = MathFunctions.math();

        @Test
        void abs() {
            IntStream.of(-2, 2).forEach(i -> assertThat(sut.abs(i)).isEqualTo(2));
            LongStream.of(-2, 2).forEach(i -> assertThat(sut.abs(i)).isEqualTo(2));
            DoubleStream.of(-2, 2).forEach(i -> assertThat(sut.abs(i)).isEqualTo(2));
        }

        @Test
        void min() {
            assertThat(sut.min(9, 4d)).isEqualTo(4);
            assertThat(sut.min(9L, 4)).isEqualTo(4);
            assertThat(sut.min(9d, 4f)).isEqualTo(4);
            assertThat(sut.min(9f, 4)).isEqualTo(4);
            assertThat(sut.min(9, 4)).isEqualTo(4);
            assertThat(sut.min(9f, 4L)).isEqualTo(4);
        }

        @Test
        void max() {
            assertThat(sut.max(9, 4d)).isEqualTo(9);
            assertThat(sut.max(9L, 4)).isEqualTo(9);
            assertThat(sut.max(9d, 4f)).isEqualTo(9);
            assertThat(sut.max(9f, 4)).isEqualTo(9);
            assertThat(sut.max(9, 4)).isEqualTo(9);
            assertThat(sut.max(9f, 4L)).isEqualTo(9);
        }

        @Test
        void pow() {
            assertThat(sut.pow(2, 2d)).isEqualTo(4);
            assertThat(sut.pow(2f, 2L)).isEqualTo(4);
        }

        @Test
        void log() {
            assertThat(sut.log(sut.e())).isEqualTo(1);
        }

        @Test
        void exp() {
            assertThat(sut.exp(1f)).isEqualTo(sut.e());
        }

        @Test
        void cbrt() {
            assertThat(sut.cbrt(8)).isEqualTo(2);
            assertThat(sut.cbrt(8d)).isEqualTo(2);
            assertThat(sut.cbrt(8f)).isEqualTo(2);
            assertThat(sut.cbrt(8L)).isEqualTo(2);
        }

        @Test
        void sqrt() {
            assertThat(sut.sqrt(4)).isEqualTo(2);
            assertThat(sut.sqrt(4d)).isEqualTo(2);
            assertThat(sut.sqrt(4f)).isEqualTo(2);
            assertThat(sut.sqrt(4L)).isEqualTo(2);
        }

        @Test
        void floor() {
            assertThat(sut.floor(4)).isEqualTo(4);
            assertThat(sut.floor(4.5d)).isEqualTo(4);
            assertThat(sut.floor(4.9f)).isEqualTo(4);
            assertThat(sut.floor(4L)).isEqualTo(4);
        }

        @Test
        void ceil() {
            assertThat(sut.ceil(4)).isEqualTo(4);
            assertThat(sut.ceil(4.5d)).isEqualTo(5);
            assertThat(sut.ceil(4.9f)).isEqualTo(5);
            assertThat(sut.ceil(4L)).isEqualTo(4);
        }

        @Test
        void round() {
            assertThat(sut.round(4)).isEqualTo(4);
            assertThat(sut.round(4.4d)).isEqualTo(4);
            assertThat(sut.round(4.9f)).isEqualTo(5);
            assertThat(sut.round(4L)).isEqualTo(4);
        }

        @Test
        void signum() {
            assertThat(sut.signum(4)).isEqualTo(1);
            assertThat(sut.signum(0d)).isEqualTo(0);
            assertThat(sut.signum(-4.9f)).isEqualTo(-1);
        }

        @Test
        void cos() {
            assertThat(sut.cos(0)).isEqualTo(1);
        }

        @Test
        void sin() {
            assertThat(sut.sin(sut.pi())).isCloseTo(0, Offset.offset(0.0000000001));
        }

        @Test
        void tan() {
            assertThat(sut.tan(0)).isEqualTo(0);
        }

        @Test
        void acos() {
            assertThat(sut.acos(1)).isEqualTo(0);
        }

        @Test
        void asin() {
            assertThat(sut.asin(0)).isEqualTo(0);
        }

        @Test
        void atan() {
            assertThat(sut.atan(0)).isEqualTo(0);
        }

        @Test
        void cosh() {
            assertThat(sut.cosh(0)).isEqualTo(1);
        }

        @Test
        void sinh() {
            assertThat(sut.sinh(0)).isEqualTo(0);
        }

        @Test
        void tanh() {
            assertThat(sut.tanh(0)).isEqualTo(0);
        }

        @Test
        void rand() {
            assertThat(sut.rand()).isBetween(0d, 1d);
        }

        @Test
        void scalb() {
            assertThat(sut.scalb(3d, 2)).isEqualTo(12);
            assertThat(sut.scalb(3f, 2.4)).isEqualTo(12);
            assertThat(sut.scalb(3, 2d)).isEqualTo(12);
            assertThat(sut.scalb(3L, 2.1f)).isEqualTo(12);
        }
    }
}
