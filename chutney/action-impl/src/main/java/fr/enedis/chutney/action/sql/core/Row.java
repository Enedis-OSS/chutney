/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.sql.core;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Row {
    @JsonProperty
    final List<Cell> cells;

    public Row(List<Cell> values) {
        this.cells = values;
    }

    public Object get(Column column) {
        return cells.stream()
            .filter(c -> c.column.equals(column))
            .findFirst()
            .orElse(Cell.NONE)
            .value;
    }

    public Object get(String header) {
        return cells.stream()
            .filter(c -> c.column.hasName(header))
            .findFirst()
            .orElse(Cell.NONE)
            .value;
    }

    public Object get(int index) {
        return cells.stream()
            .filter(c -> c.column.index == index)
            .findFirst()
            .orElse(Cell.NONE)
            .value;
    }

    public String print(Map<Column, Integer> maxLength) {
        StringBuilder sb = new StringBuilder();
        if (!cells.isEmpty()) {
            sb.append("|");
            cells.forEach(c ->
                sb.append(" ")
                .append(c.print(maxLength.get(c.column)))
                .append(" |")
            );
        }
        sb.append("\n");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return cells.equals(row.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells);
    }

    @Override
    public String toString() {
        return "Row{" +
            "cells=" + cells +
            '}';
    }

    public Map<String, Object> asMap() {
        return cells.stream()
            .collect(toMap(c -> c.column.name, c -> c.value, (c1, c2) -> c1, HashMap::new));
    }
}
