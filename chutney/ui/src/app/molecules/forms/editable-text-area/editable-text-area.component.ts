/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InputFocusDirective } from '@shared/directives/input-focus.directive';

@Component({
    selector: 'chutney-forms-editable-text-area',
    templateUrl: './editable-text-area.component.html',
    styleUrls: ['./editable-text-area.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [FormsModule, InputFocusDirective]
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
