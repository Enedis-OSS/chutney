/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.feature.api;

import fr.enedis.chutney.feature.api.dto.FeatureDto;
import fr.enedis.chutney.server.core.domain.feature.Feature;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(FeatureController.BASE_URL)
public class FeatureController {

    public static final String BASE_URL = "/api/v2/features";
    private final List<Feature> features;

    public FeatureController(List<Feature> features) {
        this.features = features;
    }

    @GetMapping
    public List<FeatureDto> getAll() {
        return features.stream().map(feature -> new FeatureDto(feature.name(), feature.active())).toList();
    }
}
