/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.function;

import fr.enedis.chutney.action.spi.SpelFunction;
import java.util.Arrays;

public class MathFunctions {

    private static final Maths instance = new Maths();

    @SpelFunction
    public static Double min(Number... a) {
        return Arrays.stream(a).mapToDouble(Number::doubleValue).min().orElseThrow();
    }

    @SpelFunction
    public static Double max(Number... a) {
        return Arrays.stream(a).mapToDouble(Number::doubleValue).max().orElseThrow();
    }

    @SpelFunction
    public static Maths math() {
        return instance;
    }

    public static class Maths {
        private Maths() {
        }

        public Double abs(Number a) {
            return Math.abs(a.doubleValue());
        }

        public Double min(Number a, Number b) {
            return Math.min(a.doubleValue(), b.doubleValue());
        }

        public Double max(Number a, Number b) {
            return Math.max(a.doubleValue(), b.doubleValue());
        }

        public Double pow(Number a, Number b) {
            return Math.pow(a.doubleValue(), b.doubleValue());
        }

        public Double log(Number a) {
            return Math.log(a.doubleValue());
        }

        public Double exp(Number a) {
            return Math.exp(a.doubleValue());
        }

        public Double cbrt(Number a) {
            return Math.cbrt(a.doubleValue());
        }

        public Double sqrt(Number a) {
            return Math.sqrt(a.doubleValue());
        }

        public Double floor(Number a) {
            return Math.floor(a.doubleValue());
        }

        public Double ceil(Number a) {
            return Math.ceil(a.doubleValue());
        }

        public Long round(Number a) {
            return Math.round(a.doubleValue());
        }

        public Double signum(Number a) {
            return Math.signum(a.doubleValue());
        }

        public Double cos(Number a) {
            return Math.cos(a.doubleValue());
        }

        public Double acos(Number a) {
            return Math.acos(a.doubleValue());
        }

        public Double cosh(Number a) {
            return Math.cosh(a.doubleValue());
        }

        public Double sin(Number a) {
            return Math.sin(a.doubleValue());
        }

        public Double asin(Number a) {
            return Math.asin(a.doubleValue());
        }

        public Double sinh(Number a) {
            return Math.sinh(a.doubleValue());
        }

        public Double tan(Number a) {
            return Math.tan(a.doubleValue());
        }

        public Double atan(Number a) {
            return Math.atan(a.doubleValue());
        }

        public Double tanh(Number a) {
            return Math.tanh(a.doubleValue());
        }

        public Double rand() {
            return Math.random();
        }

        public Double scalb(Number a, Number b) {
            return Math.scalb(a.doubleValue(), b.intValue());
        }

        public Double e() {
            return Math.E;
        }

        public Double pi() {
            return Math.PI;
        }
    }
}
