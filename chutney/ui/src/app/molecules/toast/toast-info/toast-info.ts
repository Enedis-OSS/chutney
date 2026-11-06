/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';


@Component({
    selector: 'chutney-toast-info',
    templateUrl: './toast-info.html',
    styleUrls: ['./toast-info.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ToastInfoComponent {

  @Input() message: String;

  constructor() { }

}
