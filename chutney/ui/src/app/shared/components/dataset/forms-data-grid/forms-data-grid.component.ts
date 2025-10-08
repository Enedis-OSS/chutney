/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Input, OnDestroy } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { FileSaverService } from 'ngx-filesaver';
import { Subject } from 'rxjs';
import { TranslatePipe } from '@ngx-translate/core';
import { NgIf } from '@angular/common';

@Component({
    selector: 'chutney-forms-data-grid',
    templateUrl: './forms-data-grid.component.html',
    imports: [
        TranslatePipe,
        ReactiveFormsModule,
        NgIf
    ],
    styleUrls: ['./forms-data-grid.component.scss']
})
export class FormsDataGridComponent implements OnDestroy {

    @Input() dataGridForm!: FormArray; // FormArray of FormArrays
    @Input() enableImportExport: boolean = true;

    private unsubscribeSub$ = new Subject<void>();

    constructor(
        private fb: FormBuilder,
        private fileSaverService: FileSaverService
    ) {
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    get headers(): string[] {
        if (this.dataGridForm.length > 0) {
            const firstLine = this.dataGridForm.at(0) as FormArray;
            return firstLine.controls.map(control => control.value.key);
        }
        return [];
    }

    updateHeader(col: number, newHeader: string) {
        this.dataGridForm.controls.forEach((line: AbstractControl) => {
            const cell = (line as FormArray).at(col);
            cell.patchValue({key: newHeader, value: cell.value.value});
        });
    }

    addColumn() {
        const newHeader = `Column ${this.headers.length + 1}`;
        this.dataGridForm.controls.forEach((line: AbstractControl) => {
            (line as FormArray).push(this.createKeyValue(newHeader, ''));
        });
    }

    removeColumn(col: number) {
        this.dataGridForm.controls.forEach((line: AbstractControl) => {
            (line as FormArray).removeAt(col);
        });

        if (this.dataGridForm.length > 0 && (this.dataGridForm.at(0) as FormArray).length === 0) {
            this.clearForm();
        }
    }

    clearForm() {
        while (this.dataGridForm.length > 0) {
            this.dataGridForm.removeAt(0);
        }
    }

    addLine() {
        const newLine = this.fb.array(
            this.headers.map(header => this.createKeyValue(header, ''))
        );
        this.dataGridForm.push(newLine);
    }

    removeLine(i: number) {
        this.dataGridForm.removeAt(i);
    }

    exportMultiKeyValue() {
        const delimiter = ';';
        let fileContent = this.headers.join(delimiter) + '\n';

        this.dataGridForm.value.forEach((line: any[]) => {
            fileContent += line.map(kv => kv.value).join(delimiter) + '\n';
        });

        this.fileSaverService.saveText(fileContent, 'chutney_dataset_multi_keyvalues.csv');
    }

    importMultiKeyValue(files: FileList) {
        const file = files.item(0);
        const reader = new FileReader();
        reader.onload = () => {
            this.clearForm();
            const content = '' + reader.result;
            const lines = content.split('\n').filter(line => line.trim() !== '');

            const headers = this.cleanLastSemicolon(lines.shift()!).split(';');
            lines.forEach(line => {
                const values = this.cleanLastSemicolon(line).split(';');
                const kvs = headers.map((header, i) => this.createKeyValue(header, values[i]));
                this.dataGridForm.push(this.fb.array(kvs));
            });

            this.dataGridForm.enable();
        };
        reader.readAsText(file);
    }

    asFormArray(formArray: AbstractControl<any>): FormArray {
        return formArray as FormArray;
    }

    asFormGroup(formGroup: AbstractControl<any>) {
        return formGroup as FormGroup;
    }

    private cleanLastSemicolon(value: string): string {
        return value.endsWith(';') ? value.slice(0, -1) : value;
    }

    private createKeyValue(key?: string, value?: string): FormGroup {
        return this.fb.group({
            key: key || '',
            value: value || ''
        });
    }
}
