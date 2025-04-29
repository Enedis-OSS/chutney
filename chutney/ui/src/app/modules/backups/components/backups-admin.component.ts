/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, OnDestroy, OnInit } from '@angular/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup } from '@angular/forms';

import { BackupsService } from '@core/services/backups.service';
import { Backup } from '@core/model/backups.model';
import { Subject, takeUntil, timer } from 'rxjs';
import { FileSaverService } from 'ngx-filesaver';

@Component({
    selector: 'chutney-backups-admin',
    templateUrl: './backups-admin.component.html',
    styleUrls: ['./backups-admin.component.scss']
})
export class BackupsAdminComponent implements OnInit, OnDestroy {

    private unsubscribeSub$: Subject<void> = new Subject();

    backups: Backup[] = [];
    backupForm: FormArray;
    backupables: string[];

    constructor(
        private backupsService: BackupsService,
        private formBuilder: FormBuilder,
        private fileSaverService: FileSaverService
    ) {
        this.backupsService.getBackupables()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(backupables => {
                this.backupables = backupables.sort();
                this.initBackupForm();
            })
    }

    ngOnInit(): void {
        this.loadBackups();
    }

    ngOnDestroy(): void {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    launchBackup() {
        const backupFormValue = this.getBackupFormValue();
        const backup = new Backup(backupFormValue.filter(backupable => backupable.selected)
            .map(backupable => backupable.backupable));
        this.backupsService.save(backup)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => this.reloadAfter(0));
    }

    private getBackupFormValue(): { backupable: string, selected: boolean } [] {
        const backupFormValue: { backupable: string, selected: boolean } [] = this.backupForm.value;
        return backupFormValue;
    }

    deleteBackup(backup: Backup) {
        this.backupsService.delete(backup.id)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(() => this.reloadAfter(100));
    }

    download(backup: Backup) {
        this.backupsService.download(backup.id)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(res => {
                const blob = new Blob([res], {type: 'application/zip'});
                this.fileSaverService.save(blob, backup.id + '.zip');
            });
    }

    isOneBackupSelected(): boolean {
        const backupFormValue = this.getBackupFormValue();
        return !!backupFormValue.filter(backupable => backupable.selected).length;
    }

    private loadBackups() {
        this.backupsService.list()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe(res => this.backups = res);
    }

    private initBackupForm() {
        this.backupForm = this.formBuilder.array(
            this.backupables.map(backupable => this.formBuilder.group({
                backupable: backupable,
                selected: true
            })));
    }

    private reloadAfter(time: number) {
        if (time > 0) {
            timer(time)
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe(() =>
                    this.loadBackups()
                );
        } else {
            this.loadBackups();
        }
    }

    asFormGroup(formGroup: AbstractControl): FormGroup {
        return formGroup as FormGroup
    }
}
