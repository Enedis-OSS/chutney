/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { NgbPopover } from '@ng-bootstrap/ng-bootstrap/popover';
import { MomentModule } from 'ngx-moment';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'chutney-authoring-info',
    templateUrl: './authoring-info.component.html',
    styleUrls: ['./authoring-info.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [NgbPopover, MomentModule, TranslatePipe]
})
export class AuthoringInfoComponent {
    @Input() testCase;

    constructor() {
    }
}
