/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, forwardRef, Input, OnDestroy } from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    FormArray,
    FormBuilder,
    FormControl,
    FormGroup,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    UntypedFormArray,
    ValidationErrors
} from '@angular/forms';
import { KeyValue } from '@model';
import { FileSaverService } from 'ngx-filesaver';
import { Subject, takeUntil } from 'rxjs';

@Component({
    selector: 'chutney-forms-data-grid',
    templateUrl: './forms-data-grid.component.html',
    styleUrls: ['./forms-data-grid.component.scss'],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => FormsDataGridComponent),
            multi: true
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => FormsDataGridComponent),
            multi: true
        }
    ],
    standalone: false
})
export class FormsDataGridComponent implements ControlValueAccessor, OnDestroy {

    dataGridForm: FormArray;
    headers: FormArray = this.fb.array([]);
    @Input() enableImportExport: boolean;

    private unsubscribeSub$: Subject<void> = new Subject();

    constructor(private fb: FormBuilder,
                private fileSaverService: FileSaverService) {
        this.dataGridForm = this.fb.array([]);
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    // Columns

    updateHeader(col: number, newHeader: string) {
        const lines = this.dataGridForm.controls;
        lines.forEach((line: FormArray) => {
            let cell = line.controls[col];
            cell.patchValue(new KeyValue(newHeader, cell.value.value));
        });
    }

    addColumn() {
        if (this.thereIsNoLine()) {
            this.insertLine(this.createLine([]))
        }

        this.headers.push(this.fb.control(''));
        this.dataGridForm.controls.forEach((line: FormArray) => {
            line.push(this.createKeyValue('', ''));
            line.updateValueAndValidity();
        });
        this.dataGridForm.updateValueAndValidity();
    }

    private thereIsNoLine() {
        return this.dataGridForm.controls.length === 0;
    }

    removeColumn(col: number) {
        this.headers.removeAt(col);
        this.dataGridForm.controls.forEach((line: FormArray) => {
            line.removeAt(col);
        });

        if (this.isLastColumn()) {
            this.clearForm()
        }
    }

    private isLastColumn() {
        return !this.dataGridForm.value.some((element) => element.length > 0);
    }

    private clearForm() {
        this.headers.clear();
        while (this.dataGridForm.length !== 0) {
            this.dataGridForm.removeAt(0)
        }
    }

    // Lines

    addLine() {
        if (this.thereIsNoHeader()) {
            this.headers.push(this.fb.control(''));
        }

        let line: Array<KeyValue> = this.headers.value.map(h => {
            return new KeyValue(h, '');
        });
        this.insertLine(this.createLine(line));
    }

    private thereIsNoHeader() {
        return this.headers.length === 0;
    }

    removeLine(i: number) {
        this.dataGridForm.removeAt(i);
    }

    // Import / Export

    exportMultiKeyValue() {
        const fileName = 'chutney_dataset_multi_keyvalues.csv';
        let fileContent = '';
        const delimiter = ';';

        fileContent = this.headerLine(delimiter);

        this.dataGridForm.value
            .map(line => {
                let lineContent = '\n';
                line.forEach(kv => {
                    lineContent += kv.value + delimiter;
                });
                return lineContent;
            })
            .forEach(line => fileContent += line  );

        this.fileSaverService.saveText(fileContent, fileName);
    }

    private headerLine(delimiter: string) {
        let headerLine = '';
        this.headers.value.forEach(header => {
            headerLine += header + delimiter;
        });
        return headerLine;
    }

    importMultiKeyValue(files: any) {
        const file = files.item(0);
        const fileReader = new FileReader();
        fileReader.onload = (e) => {
            this.clearForm();
            const content = '' + fileReader.result;
            const lines = content.split('\n');

            this.headers = this.fb.array(this.cleanLastSemicolon(lines.shift()).split(';')
                .map(header => this.fb.control(header)));

            lines.forEach(l => {
                const lineValues = this.cleanLastSemicolon(l).split(';');

                const kv = this.headers
                    .value
                    .map( (header, i) => [header, lineValues[i]])
                    .map( ([key, value]) => new KeyValue(key, value) );

                this.insertLine(this.createLine(kv));
            });

            this.dataGridForm.enable();
        };
        fileReader.readAsText(file);
    }

    private cleanLastSemicolon(value: string): string{
        if(value.length > 0 && value[value.length - 1] === ';') {
            value = value.slice(0, value.length - 1);
        }
        return value;
    }

    // CVA
    onChanged: any = () => {
    };

    onTouched: any = () => {
    };

    propagateChange: any = () => {
    };

    writeValue(val: Array<Array<KeyValue>>): void {
        this.clearForm();
        if (val != null && val.length > 0) {
            if (this.dataGridForm.length === 0) {
                val.map(l => this.createLine(l))
                   .forEach(l => this.insertLine(l));
            }

            this.headers = this.fb.array(this.getHeaders().map(header => this.fb.control(header)))
        }
    }

    private getHeaders(): Array<string> {
        if (this.dataGridForm.length > 0) {
            return this.dataGridForm.controls[0].value.map(kv => kv.key);
        }
        return [];
    }

    private createLine(line: Array<KeyValue>): FormArray {
        let lineArray = this.fb.array([]) as UntypedFormArray;
        line.map(kv => this.createKeyValue(kv.key, kv.value))
            .forEach(cell => {
                lineArray.push(cell);
            });
        return lineArray;
    }

    private createKeyValue(key?: string, value?: string): FormGroup {
        return this.fb.group({
            key: key ? key : '',
            value: value ? value : ''
        });
    }

    private insertLine(line: FormArray, i?: number) {
        if (i == null) {
            i = this.dataGridForm.length;
        }

        this.dataGridForm.insert(i, line);
    }

    registerOnChange(fn: any): void {
        this.propagateChange = fn;
        this.dataGridForm.valueChanges
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(fn);
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    setDisabledState?(isDisabled: boolean): void {
        isDisabled ? this.dataGridForm.disable() : this.dataGridForm.enable();
    }

    validate(c: AbstractControl): ValidationErrors | null {
        return this.dataGridForm.valid ? null : {
            invalidForm: {
                valid: false,
                message: 'fields are invalid'
            }
        };
    }

    protected readonly FormControl = FormControl;
}
