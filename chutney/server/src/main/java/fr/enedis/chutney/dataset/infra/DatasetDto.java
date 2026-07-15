/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.dataset.infra;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

class DatasetDto {

    @JsonIgnore public final String id;
    public final String name;
    public final String description;
    public final List<String> tags;
    public final Map<String, String> constants;
    public final List<Map<String, String>> datatable;

    @JsonCreator
    DatasetDto(
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("tags") List<String> tags,
        @JsonProperty("constants") Map<String, String> constants,
        @JsonProperty("datatable") List<Map<String, String>> datatable
    ) {
        this.id = name.replaceAll(" ", "_");
        this.name = name;
        this.description = description;
        this.tags = tags;
        this.constants = constants;
        this.datatable = datatable;
    }

}
