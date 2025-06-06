/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.environment.api.variable;

import fr.enedis.chutney.environment.api.variable.dto.EnvironmentVariableDto;
import fr.enedis.chutney.environment.domain.exception.EnvVariableNotFoundException;
import fr.enedis.chutney.environment.domain.exception.EnvironmentNotFoundException;
import fr.enedis.chutney.environment.domain.exception.VariableAlreadyExistingException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentVariableController implements EnvironmentVariableApi {

    private final String BASE_URI = "/api/v2/variables";
    private final EnvironmentVariableApi delegate;

    public EnvironmentVariableController(EnvironmentVariableApi delegate) {
        this.delegate = delegate;
    }


    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @PostMapping(BASE_URI)
    public void addVariable(@RequestBody List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, VariableAlreadyExistingException {
        delegate.addVariable(values);
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @PutMapping(BASE_URI + "/{key}")
    public void updateVariable(@PathVariable("key") String key, @RequestBody List<EnvironmentVariableDto> values) throws EnvironmentNotFoundException, EnvVariableNotFoundException {
        delegate.updateVariable(key, values);
    }

    @Override
    @PreAuthorize("hasAuthority('ENVIRONMENT_ACCESS')")
    @DeleteMapping(BASE_URI + "/{key}")
    public void deleteVariable(@PathVariable("key") String key) throws EnvVariableNotFoundException {
        delegate.deleteVariable(key);
    }
}
