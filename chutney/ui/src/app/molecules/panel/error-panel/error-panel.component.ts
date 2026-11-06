/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'chutney-error-panel',
    templateUrl: './error-panel.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ErrorPanelComponent {

  @Input() errorMessage: string;
}
