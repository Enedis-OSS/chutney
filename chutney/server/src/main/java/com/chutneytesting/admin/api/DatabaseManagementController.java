/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.chutneytesting.admin.api;

import com.chutneytesting.admin.domain.DBVacuum;
import com.chutneytesting.admin.domain.DBVacuum.VacuumReport;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/database")
public class DatabaseManagementController {

    private final DBVacuum dbVacuum;

    DatabaseManagementController(DBVacuum dbVacuum) {
        this.dbVacuum = dbVacuum;
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @PostMapping(path = "/compact", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Long> vacuum() {
        VacuumReport report = dbVacuum.vacuum();
        return List.of(report.beforeSize(), report.afterSize());
    }

    @PreAuthorize("hasAuthority('ADMIN_ACCESS')")
    @GetMapping(path = "/size", produces = MediaType.APPLICATION_JSON_VALUE)
    public Long dbSize() {
        return dbVacuum.size();
    }
}
