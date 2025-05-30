/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.campaign.infra;

import static fr.enedis.chutney.ServerConfigurationValues.CONFIGURATION_FOLDER_SPRING_VALUE;
import static fr.enedis.chutney.campaign.domain.Frequency.toFrequency;
import static fr.enedis.chutney.tools.file.FileUtils.initFolder;

import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign;
import fr.enedis.chutney.campaign.domain.PeriodicScheduledCampaign.CampaignExecutionRequest;
import fr.enedis.chutney.campaign.domain.ScheduledCampaignRepository;
import fr.enedis.chutney.campaign.infra.SchedulingCampaignDto.CampaignExecutionRequestDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * Scheduling campaign persistence.
 */
@Repository
public class SchedulingCampaignFileRepository implements ScheduledCampaignRepository {

    private static final Path ROOT_DIRECTORY_NAME = Paths.get("scheduling");
    private static final String SCHEDULING_CAMPAIGNS_FILE = "schedulingCampaigns.json";

    private final Path storeFolderPath;
    private final Path resolvedFilePath;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule()
            .addDeserializer(SchedulingCampaignDto.class, new SchedulingCampaignsDtoDeserializer()))
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    private final ReadWriteLock rwLock;

    SchedulingCampaignFileRepository(@Value(CONFIGURATION_FOLDER_SPRING_VALUE) String storeFolderPath) throws UncheckedIOException {
        this.rwLock = new ReentrantReadWriteLock(true);
        this.storeFolderPath = Paths.get(storeFolderPath).resolve(ROOT_DIRECTORY_NAME);
        this.resolvedFilePath = this.storeFolderPath.resolve(SCHEDULING_CAMPAIGNS_FILE);
        initFolder(this.storeFolderPath);
    }

    @Override
    public PeriodicScheduledCampaign add(PeriodicScheduledCampaign periodicScheduledCampaign) {
        final Lock writeLock;
        (writeLock = rwLock.writeLock()).lock();
        try {
            Map<String, SchedulingCampaignDto> schedulingCampaigns = readFromDisk();
            Long nextId = getCurrentMaxId(schedulingCampaigns) + 1L;
            schedulingCampaigns.put(String.valueOf(nextId), toDto(nextId, periodicScheduledCampaign));
            writeOnDisk(resolvedFilePath, schedulingCampaigns);

            return periodicScheduledCampaign;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeById(Long id) {
        final Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            Map<String, SchedulingCampaignDto> schedulingCampaigns = readFromDisk();
            schedulingCampaigns.remove(String.valueOf(id));
            writeOnDisk(resolvedFilePath, schedulingCampaigns);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void removeCampaignId(Long id) {
        final Lock writeLock = rwLock.writeLock();
        writeLock.lock();
        try {
            Map<String, SchedulingCampaignDto> schedulingCampaigns = readFromDisk();
            Map<String, SchedulingCampaignDto> schedulingCampaignsFiltered = new HashMap<>();
            schedulingCampaigns.forEach((key, schedulingCampaignDto) -> {
                Optional<CampaignExecutionRequestDto> foundRequest = schedulingCampaignDto.campaignExecutionRequestDto.stream().filter(cer -> cer.campaignId().equals(id)).findFirst();
                // Remove id and title if the campaignId has been found
                foundRequest.ifPresent(schedulingCampaignDto.campaignExecutionRequestDto::remove);
                if (!schedulingCampaignDto.campaignExecutionRequestDto.isEmpty()) { // Set the schedule only if a campaign is present after removal
                    schedulingCampaignsFiltered.put(key, schedulingCampaignDto);
                }
            });
            writeOnDisk(resolvedFilePath, schedulingCampaignsFiltered);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public List<PeriodicScheduledCampaign> getAll() {
        return readFromDisk().values().stream()
            .map(this::fromDto)
            .collect(Collectors.toList());
    }

    private Map<String, SchedulingCampaignDto> readFromDisk() {
        Map<String, SchedulingCampaignDto> stringSchedulingCampaignDTO = new HashMap<>();
        final Lock readLock;
        (readLock = rwLock.readLock()).lock();
        try {
            if (Files.exists(resolvedFilePath)) {
                byte[] bytes = Files.readAllBytes(resolvedFilePath);
                stringSchedulingCampaignDTO.putAll(objectMapper.readValue(bytes, new TypeReference<HashMap<String, SchedulingCampaignDto>>() {
                }));
            }
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot read configuration file: " + resolvedFilePath, e);
        } finally {
            readLock.unlock();
        }

        return stringSchedulingCampaignDTO;
    }

    private void writeOnDisk(Path filePath, Map<String, SchedulingCampaignDto> schedulingCampaignDTO) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(schedulingCampaignDTO);
            try {
                Files.write(filePath, bytes);
            } catch (IOException e) {
                throw new UnsupportedOperationException("Cannot write in configuration directory: " + storeFolderPath, e);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot serialize " + schedulingCampaignDTO, e);
        }
    }

    private PeriodicScheduledCampaign fromDto(SchedulingCampaignDto dto) {
        return new PeriodicScheduledCampaign(Long.valueOf(dto.id), dto.schedulingDate, toFrequency(dto.frequency), dto.environment, dto.campaignExecutionRequestDto.stream().map(aa -> new CampaignExecutionRequest(aa.campaignId(), aa.campaignTitle(), aa.datasetId())).toList());
    }

    private SchedulingCampaignDto toDto(long id, PeriodicScheduledCampaign periodicScheduledCampaign) {
        return new SchedulingCampaignDto(String.valueOf(id), periodicScheduledCampaign.nextExecutionDate, periodicScheduledCampaign.frequency.label, periodicScheduledCampaign.environment,  periodicScheduledCampaign.campaignExecutionRequests.stream().map(aa -> new CampaignExecutionRequestDto(aa.campaignId(), aa.campaignTitle(), aa.datasetId())).toList());
    }

    private Long getCurrentMaxId(Map<String, SchedulingCampaignDto> schedulingCampaigns) {
        return schedulingCampaigns.keySet().stream().mapToLong(Long::valueOf).max().orElse(0);
    }
}
