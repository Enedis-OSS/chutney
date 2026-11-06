/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'chutney-collapsible-panel',
    templateUrl: './collapsible-panel.component.html',
    styleUrls: ['./collapsible-panel.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class CollapsiblePanelComponent {

    collapsed = true;
    @Input() title: string;
}
