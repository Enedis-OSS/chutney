/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'chutney-forms-editable-badge',
    templateUrl: './editable-badge.component.html',
    styleUrls: ['./editable-badge.component.scss'],
    standalone: false
})
export class EditableBadgeComponent {

    editMode = false;

    @Input() id: string;
    @Input() placeholder: string;
    @Input() type = 'simple';
    @Input() model: string;
    @Input() maxlength = 150;
    @Input() defaultValue = '';
    @Output() modelChange = new EventEmitter<string>();

    constructor() { }

    onInputChange(newValue: string) {
        this.model = newValue;
        this.modelChange.emit(this.model);
    }
}
