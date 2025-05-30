/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.environment;

import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.domain.exception.AlreadyExistingEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.CannotDeleteEnvironmentException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.InvalidEnvironmentNameException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/environments")
public class HttpEnvironmentApi implements EnvironmentApi {

    private final EnvironmentApi delegate;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    HttpEnvironmentApi(EnvironmentApi delegate) {
        this.delegate = delegate;
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<EnvironmentDto> listEnvironments() {
        return delegate.listEnvironments();
    }

    @Override
    @PreAuthorize("hasAuthority('SCENARIO_EXECUTE') or hasAuthority('CAMPAIGN_WRITE') or hasAuthority('CAMPAIGN_EXECUTE')")
    @GetMapping(path = "/names", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<String> listEnvironmentsNames() {
        return delegate.listEnvironmentsNames();
    }

    @Override
    public String defaultEnvironmentName() {
        throw new UnsupportedOperationException();
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public EnvironmentDto createEnvironment(@RequestBody EnvironmentDto environmentDto) throws InvalidEnvironmentNameException, AlreadyExistingEnvironmentException {
        return delegate.createEnvironment(environmentDto, false);
    }

    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EnvironmentDto importEnvironment(@RequestParam("file") MultipartFile file) {
        try {
            return importEnvironment(
                objectMapper.readValue(file.getBytes(), EnvironmentDto.class)
            );
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot deserialize file: " + file.getName(), e);
        }
    }

    @Override
    public EnvironmentDto importEnvironment(EnvironmentDto environmentDto) {
        return delegate.importEnvironment(environmentDto);
    }

    @Override
    public EnvironmentDto createEnvironment(@RequestBody EnvironmentDto environmentDto, boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @DeleteMapping("/{environmentName}")
    public void deleteEnvironment(@PathVariable("environmentName") String environmentName) throws EnvironmentNotFoundException, CannotDeleteEnvironmentException {
        delegate.deleteEnvironment(environmentName);
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @PutMapping("/{environmentName}")
    public void updateEnvironment(@PathVariable("environmentName") String environmentName, @RequestBody EnvironmentDto environmentDto) throws InvalidEnvironmentNameException, EnvironmentNotFoundException {
        delegate.updateEnvironment(environmentName, environmentDto);
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @GetMapping("/{environmentName}")
    public EnvironmentDto getEnvironment(@PathVariable("environmentName") String environmentName) throws EnvironmentNotFoundException {
        return delegate.getEnvironment(environmentName);
    }
}
