/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.environment.HttpEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.api.target.dto.TargetDto;
import fr.enedis.chutney.environment.domain.TargetFilter;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingTargetException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class TargetController implements TargetApi {

    public static final String TARGET_BASE_URI = "/api/v2/targets";

    private final EnvironmentApi envDelegate;
    private final TargetApi delegate;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    TargetController(TargetApi delegate, @Qualifier("environmentEmbeddedApplication") EnvironmentApi envDelegate) {
        this.envDelegate = envDelegate;
        this.delegate = delegate;
    }

    @PreAuthorize("hasAnyAuthority('TARGET_READ', 'ADMIN_ACCESS')")
    @GetMapping(path = HttpEnvironmentApi.BASE_URL + "/targets", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EnvironmentDto> listTargetsByEnvironments() throws EnvironmentNotFoundException, TargetNotFoundException {
        return envDelegate.listEnvironments().stream()
            .map(EnvironmentDto::copyTargetsByEnvironments)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @PreAuthorize("hasAuthority('TARGET_WRITE')")
    @PostMapping(value = HttpEnvironmentApi.BASE_URL + "/{environmentName}/targets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TargetDto importTarget(@PathVariable("environmentName") String environmentName, @RequestParam("file") MultipartFile file) {
        try {
            return importTarget(
                environmentName,
                objectMapper.readValue(file.getBytes(), TargetDto.class)
            );
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot deserialize file: " + file.getName(), e);
        }
    }

    @Override
    public TargetDto importTarget(String environmentName, TargetDto targetDto) {
        return delegate.importTarget(environmentName, targetDto);
    }

    @Override
    @PreAuthorize("hasAuthority('TARGET_READ')")
    @GetMapping(path = TARGET_BASE_URI + "/names", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> listTargetsNames() throws EnvironmentNotFoundException {
        return delegate.listTargetsNames();
    }

    @PreAuthorize("hasAuthority('TARGET_READ')")
    @GetMapping(path = TARGET_BASE_URI, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TargetDto> listTargets(TargetFilter filter) throws EnvironmentNotFoundException {
        return delegate.listTargets(filter);
    }

    @Override
    @PreAuthorize("hasAuthority('TARGET_READ')")
    @GetMapping(HttpEnvironmentApi.BASE_URL + "/{environmentName}/targets/{targetName}")
    public TargetDto getTarget(@PathVariable("environmentName") String environmentName, @PathVariable("targetName") String targetName) throws EnvironmentNotFoundException, TargetNotFoundException {
        return delegate.getTarget(environmentName, targetName);
    }

    @Override
    @PreAuthorize("hasAuthority('TARGET_WRITE')")
    @DeleteMapping(HttpEnvironmentApi.BASE_URL + "/{environmentName}/targets/{targetName}")
    public void deleteTarget(@PathVariable("environmentName") String environmentName, @PathVariable("targetName") String targetName) throws EnvironmentNotFoundException, TargetNotFoundException {
        delegate.deleteTarget(environmentName, targetName);
    }

    @Override
    @PreAuthorize("hasAuthority('TARGET_WRITE')")
    @DeleteMapping(TARGET_BASE_URI + "/{targetName}")
    public void deleteTarget(@PathVariable("targetName") String targetName) throws EnvironmentNotFoundException, TargetNotFoundException {
        delegate.deleteTarget(targetName);
    }


    @Override
    @PreAuthorize("hasAuthority('TARGET_WRITE')")
    @PostMapping(TARGET_BASE_URI)
    public void addTarget(@RequestBody TargetDto targetDto) throws EnvironmentNotFoundException, AlreadyExistingTargetException {
        delegate.addTarget(targetDto);
    }

    @Override
    @PreAuthorize("hasAuthority('TARGET_WRITE')")
    @PutMapping(TARGET_BASE_URI + "/{targetName}")
    public void updateTarget(@PathVariable("targetName") String targetName, @RequestBody TargetDto targetDto) throws EnvironmentNotFoundException, TargetNotFoundException {
        delegate.updateTarget(targetName, targetDto);
    }
}
