/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package fr.enedis.chutney.action.http.jackson;

import com.github.tomakehurst.wiremock.WireMockServer;
import tools.jackson.databind.module.SimpleModule;

public class WiremockModule extends SimpleModule {

    private static final String NAME = "ChutneyWiremockModule";

    public WiremockModule() {
        super(NAME);
        addSerializer(WireMockServer.class, new WireMockServerSerializer());
    }
}
