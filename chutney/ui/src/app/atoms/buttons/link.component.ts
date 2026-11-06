/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'chutney-link',
    template: `
    <a class="chutney-link" >
        {{model}}
    </a>
`,
    styleUrls: ['./link.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class LinkComponent {

    @Input() model: string;

    constructor() { }
}
