/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;

import fr.enedis.chutney.campaign.infra.SchedulingCampaignDto.CampaignExecutionRequestDto;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;

class SchedulingCampaignsDtoDeserializer extends StdDeserializer<SchedulingCampaignDto> {

    private static final ObjectMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected SchedulingCampaignsDtoDeserializer() {
        super(SchedulingCampaignDto.class);
    }

    @Override
    public SchedulingCampaignDto deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonNode node = parser.readValueAsTree();

        String id = node.get("id").asText();
        LocalDateTime schedulingDate = getSchedulingDate(node);
        String frequency = getFrequency(node);
        List<Long> campaignsId = getLongList(node.get("campaignsId"));
        List<String> campaignsTitle = getStringList(node.get("campaignsTitle"));
        String environment = node.has("environment") ? node.get("environment").asText() : null;
        List<String> datasetIds = getStringList(node.get("datasetsId"));
        List<String> jiraIds = getStringList(node.get("jiraIds"));

        List<CampaignExecutionRequestDto> campaignExecutionRequestDto =
            IntStream.range(0, campaignsId.size())
                .mapToObj(i -> new CampaignExecutionRequestDto(
                    campaignsId.get(i),
                    campaignsTitle.get(i),
                    guardArrayOfBoundException(datasetIds, i),
                    guardArrayOfBoundException(jiraIds, i))
                )
                .collect(Collectors.toList());


        return new SchedulingCampaignDto(id, schedulingDate, frequency, environment, campaignExecutionRequestDto);
    }

    private String guardArrayOfBoundException(List<String> list, int i) {
        if (i < list.size()) {
            return list.get(i);
        }
        return "";
    }

    private List<Long> getLongList(JsonNode node) {
        return extractList(node, JsonNode::asLong);
    }

    private List<String> getStringList(JsonNode node) {
        return extractList(node, JsonNode::asText);
    }

    private <T> List<T> extractList(JsonNode node, Function<JsonNode, T> mapper) {
        if (node == null) {
            return emptyList();
        }
        return StreamSupport.stream(node.spliterator(), false)
            .map(mapper)
            .collect(toCollection(ArrayList::new));
    }

    private LocalDateTime getSchedulingDate(JsonNode node) throws JacksonException {
        return MAPPER.treeToValue(node.get("schedulingDate"), LocalDateTime.class);
    }

    private String getFrequency(JsonNode node) {
        return node.has("frequency") ? node.get("frequency").asText() : null;
    }
}
