/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input } from '@angular/core';

@Component({
    selector: 'chutney-error-panel',
    templateUrl: './error-panel.component.html',
    standalone: false
})
export class ErrorPanelComponent {

  @Input() errorMessage: string;
}
