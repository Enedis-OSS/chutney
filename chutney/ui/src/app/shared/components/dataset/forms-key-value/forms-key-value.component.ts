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
    selector: 'chutney-forms-key-value',
    templateUrl: './forms-key-value.component.html',
    imports: [
        ReactiveFormsModule,
        TranslatePipe,
        NgIf
    ],
    styleUrls: ['./forms-key-value.component.scss']
})
export class FormsKeyValueComponent implements OnDestroy {

    @Input() keyValuesForm!: FormArray;
    @Input() enableImportExport: boolean = true;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(
        private fb: FormBuilder,
        private fileSaverService: FileSaverService,
    ) {}

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    insertNewKeyValue(i?: number) {
        this.addKeyValue('', '', i != null ? i + 1 : undefined);
    }

    private addKeyValue(key?: string, value?: string, i?: number): void {
        const group = this.createKeyValue(key, value);
        if (i != null) {
            this.keyValuesForm.insert(i, group);
        } else {
            this.keyValuesForm.push(group);
        }
    }

    private createKeyValue(key?: string, value?: string): FormGroup {
        return this.fb.group({
            key: key || '',
            value: value || ''
        });
    }

    removeKeyValueLine(i?: number) {
        if (i != null && i >= 0 && i < this.keyValuesForm.length) {
            this.keyValuesForm.removeAt(i);
        }
    }

    private clearForm() {
        while (this.keyValuesForm.length > 0) {
            this.keyValuesForm.removeAt(0);
        }
    }

    exportKeyValue() {
        const fileName = 'chutney_dataset_keyvalues.csv';
        const delimiter = ';';
        const fileContent = this.keyValuesForm.value
            .map((element: any) => `${element.key}${delimiter}${element.value}`)
            .join('\n');

        this.fileSaverService.saveText(fileContent, fileName);
    }

    importKeyValue(files: FileList) {
        const file = files.item(0);
        const fileReader = new FileReader();
        fileReader.onload = () => {
            this.clearForm();
            const content = '' + fileReader.result;
            const lines = content.split('\n');
            lines.forEach(line => {
                const [key, value] = line.split(';');
                if (key && value) {
                    this.addKeyValue(key, value);
                }
            });
            this.keyValuesForm.enable();
        };
        fileReader.readAsText(file);
    }

    asFormGroup(formGroup: AbstractControl<any>): FormGroup {
        return formGroup as FormGroup;
    }
}
