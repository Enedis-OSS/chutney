/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'chutney-forms-editable-text-area',
    templateUrl: './editable-text-area.component.html',
    styleUrls: ['./editable-text-area.component.scss'],
    standalone: false
})
export class EditableTextAreaComponent {

    @Input() id: string;
    @Input() placeholder: string;
    @Input() type = 'simple';
    @Input() model: string;
    @Input() defaultValue = '';
    @Output() modelChange = new EventEmitter<string>();

    constructor() { }

    onInputChange(newValue: string) {
        this.model = newValue;
        this.modelChange.emit(this.model);
    }
}
