/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;

import fr.enedis.chutney.environment.api.environment.EnvironmentApi;
import fr.enedis.chutney.environment.api.environment.HttpEnvironmentApi;
import fr.enedis.chutney.environment.api.environment.dto.EnvironmentDto;
import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDto;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.TargetNotFoundException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import java.util.LinkedList;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentVariableController implements EnvironmentVariableApi {

    public static final String VARIABLE_BASE_URI = "/api/v2/variables";

    private final EnvironmentApi envDelegate;
    private final EnvironmentVariableApi delegate;

    public EnvironmentVariableController(EnvironmentVariableApi delegate, @Qualifier("environmentEmbeddedApplication") EnvironmentApi envDelegate) {
        this.envDelegate = envDelegate;
        this.delegate = delegate;
    }

    @PreAuthorize("hasAuthority('VARIABLE_READ')")
    @GetMapping(path = HttpEnvironmentApi.BASE_URL + "/variables", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EnvironmentDto> listVariablesByEnvironments() throws EnvironmentNotFoundException, TargetNotFoundException {
        return envDelegate.listEnvironments().stream()
            .map(EnvironmentDto::copyVariablesByEnvironments)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @PreAuthorize("hasAuthority('VARIABLE_WRITE')")
    @PostMapping(VARIABLE_BASE_URI)
    public void addVariable(@RequestBody List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, VariableAlreadyExistingException {
        delegate.addVariable(values);
    }

    @Override
    @PreAuthorize("hasAuthority('VARIABLE_WRITE')")
    @PutMapping(VARIABLE_BASE_URI + "/{key}")
    public void updateVariable(@PathVariable("key") String key, @RequestBody List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, EnvVariableNotFoundException {
        delegate.updateVariable(key, values);
    }

    @Override
    @PreAuthorize("hasAuthority('VARIABLE_WRITE')")
    @DeleteMapping(VARIABLE_BASE_URI + "/{key}")
    public void deleteVariable(@PathVariable("key") String key) throws EnvVariableNotFoundException {
        delegate.deleteVariable(key);
    }
}
