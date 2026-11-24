/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, ChangeDetectionStrategy } from '@angular/core';
import { JiraComponent } from './jira/jira.component';
import { LinkifierComponent } from './linkifier/linkifier.component';


@Component({
    selector: 'chutney-plugin-configuration',
    templateUrl: './plugin-configuration.component.html',
    styleUrls: ['./plugin-configuration.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [JiraComponent, LinkifierComponent]
})
export class PluginConfigurationComponent {

}
