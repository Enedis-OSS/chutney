/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'chutney-authoring-info',
    templateUrl: './authoring-info.component.html',
    styleUrls: ['./authoring-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AuthoringInfoComponent {
    @Input() testCase;

    constructor() {
    }
}
