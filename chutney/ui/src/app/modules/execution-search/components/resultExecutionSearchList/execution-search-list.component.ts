/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild, } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { Params, Router } from '@angular/router';
import { ExecutionStatus } from '@core/model/scenario/execution-status';
import { Execution } from '@model';
import { NgbDate, NgbDateStruct } from '@ng-bootstrap/ng-bootstrap';
import { TranslateService } from '@ngx-translate/core';
import { DateFormatPipe } from 'ngx-moment';
import { Subscription } from 'rxjs';
import { debounceTime, map, tap } from 'rxjs/operators';
import { MultiSelectComponent } from 'ng-multiselect-dropdown';

@Component({
    selector: 'execution-search-report-list',
    templateUrl: './execution-search-list.component.html',
    styleUrls: ['./execution-search-list.component.scss'],
    standalone: false
})
export class ExecutionSearchListComponent
    implements OnChanges, OnDestroy
{
    ExecutionStatus = ExecutionStatus;
    filteredExecutions: Execution[] = [];
    filtersForm: FormGroup;

    status: { id: string; text: string }[] = [];
    environments: { id: string; text: string }[] = [];
    executors: { id: string; text: string }[] = [];
    campaigns: { id: string; text: string }[] = [];
    tags: { id: string; text: string }[] = [];
    selectSettings = {
        text: '',
        enableCheckAll: false,
        enableSearchFilter: true,
        autoPosition: false,
        classes: 'dropdown-list1',
    };

    private filters$: Subscription;

    private readonly iso_Date_Delimiter = '-';
    @ViewChild('statusDropdown', { static: false })
    statusDropdown: MultiSelectComponent;

    @ViewChild('envsDropdown', { static: false })
    envsDropdown: MultiSelectComponent;
    @ViewChild('executorsDropdown', { static: false })
    executorsDropdown: MultiSelectComponent;
    @ViewChild('campsDropdown', { static: false })
    campsDropdown: MultiSelectComponent;
    @ViewChild('tagsDropdown', { static: false })
    tagsDropdown: MultiSelectComponent;

    @Input() executions: Execution[] = [];
    @Output() onExecutionSelect = new EventEmitter<{
        execution: Execution;
        focus: boolean;
    }>();
    @Input() filters: Params;
    @Output() filtersChange = new EventEmitter<Params>();

    constructor(
        private router: Router,
        private formBuilder: FormBuilder,
        private datePipe: DateFormatPipe,
        private translateService: TranslateService
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        this.initFiltersOptions();
        this.applyFilters();
        this.onFiltersChange();
    }

    getDateFilterValue() {
        let date: NgbDateStruct = this.filtersForm.value.date;
        return new Date(date.year, date.month - 1, date.day);
    }

    noExecutionAt() {
        return (date: NgbDate) =>
            !this.executions.filter((exec) =>
                this.matches(exec, { date: date })
            ).length;
    }

    openReport(execution: Execution, focus: boolean = true) {
        this.onExecutionSelect.emit({ execution, focus });
    }
    getFormControl(name: string): FormControl {
        return this.filtersForm.get(name) as FormControl;
    }

    ngOnDestroy(): void {
        this.filters$.unsubscribe();
    }

    private initFiltersOptions() {
        this.status = [
            ...new Set(this.executions.map((exec) => exec.status)),
        ].map((status) =>
            this.toSelectOption(
                status,
                this.translateService.instant(ExecutionStatus.toString(status))
            )
        );
        this.environments = [
            ...new Set(this.executions.map((exec) => exec.environment)),
        ].map((env) => this.toSelectOption(env));
        this.executors = [
            ...new Set(this.executions.map((exec) => exec.user)),
        ].map((user) => this.toSelectOption(user));
        this.campaigns = [
            ...new Set(
                this.executions
                    .filter((exec) => !!exec.campaignReport)
                    .map((exec) => exec.campaignReport.campaignName)
            ),
        ].map((camp) => this.toSelectOption(camp));
        this.tags = [...new Set(this.executions.flatMap(exec => exec.tags))].map(tag => this.toSelectOption(tag));
    }

    private applyFilters() {
        this.applyFiltersOnHeaders();
        this.applyFiltersOnExecutions();
    }

    private applyFiltersOnHeaders() {
        this.filtersForm = this.formBuilder.group({
            keyword: this.filters['keyword'],
            date: this.formBuilder.control(
                this.toNgbDate(this.filters['date'])
            ),
            status: this.formBuilder.control(
                this.selectedOptionsFromUri(this.filters['status'], (status) =>
                    this.translateService.instant(
                        ExecutionStatus.toString(status)
                    )
                )
            ),
            environments: this.formBuilder.control(
                this.selectedOptionsFromUri(this.filters['env'])
            ),
            executors: this.formBuilder.control(
                this.selectedOptionsFromUri(this.filters['exec'])
            ),
            campaigns: this.formBuilder.control(
                this.selectedOptionsFromUri(this.filters['camp'])
            ),
            tags: this.formBuilder.control(
                this.selectedOptionsFromUri(this.filters['tags'])
            ),
        });
    }

    private applyFiltersOnExecutions() {
        this.filteredExecutions = this.executions.filter((exec) =>
            this.matches(exec, this.filtersForm.value)
        );
    }

    private onFiltersChange() {
        this.filters$ = this.filtersForm.valueChanges
            .pipe(
                debounceTime(500),
                map((value) => this.toQueryParams(value)),
                tap((params) => this.filtersChange.emit(params))
            )
            .subscribe();
    }

    private selectedOptionsFromUri(
        param: string,
        labelResolver?: (param) => string
    ) {
        if (param) {
            return param
                .split(',')
                .map((part) =>
                    this.toSelectOption(
                        part,
                        labelResolver ? labelResolver(part) : part
                    )
                );
        }
        return [];
    }

    private toSelectOption(id: string, label: string = id) {
        return { id: id, text: label };
    }

    private toQueryParams(filters: any): Params {
        const params: Params = {};
        if (filters.keyword) {
            params['keyword'] = filters.keyword;
        }
        if (filters.status && filters.status.length) {
            params['status'] = filters.status
                .map((status) => status.id)
                .toString();
        }
        if (filters.date) {
            params['date'] = this.toIsoDate(filters.date);
        }
        if (filters.environments && filters.environments.length) {
            params['env'] = filters.environments
                .map((env) => env.id)
                .toString();
        }
        if (filters.campaigns && filters.campaigns.length) {
            params['camp'] = filters.campaigns.map((env) => env.id).toString();
        }
        if (filters.executors && filters.executors.length) {
            params['exec'] = filters.executors.map((env) => env.id).toString();
        }
        if (filters.tags && filters.tags.length) {
            params['tags'] = filters.tags.map(tag => tag.id).toString();
        }
        return params;
    }

    private toIsoDate(ngbDate: NgbDateStruct) {
        let dd = String(ngbDate.day).padStart(2, '0');
        let mm = String(ngbDate.month).padStart(2, '0');
        let yyyy = ngbDate.year;
        return [yyyy, mm, dd].join(this.iso_Date_Delimiter);
    }

    private toNgbDate(isoString: string) {
        if (isoString) {
            const date = isoString.split('-');
            return {
                day: parseInt(date[2], 10),
                month: parseInt(date[1], 10),
                year: parseInt(date[0], 10),
            };
        }
        return null;
    }

    private matches(exec: Execution, filters: any): boolean {
        let keywordMatch = true;
        if (filters.keyword) {
            let space = ' ';
            let searchScope =
                exec.user +
                space +
                exec.environment +
                space +
                this.datePipe.transform(exec.time, 'DD MMM. YYYY HH:mm') +
                space +
                exec.tags.join(space) +
                space +
                exec.executionId +
                space +
                this.translateService.instant(
                    ExecutionStatus.toString(exec.status)
                ) +
                space;
            if (exec.campaignReport) {
                searchScope += space + exec.campaignReport.campaignName;
            }

            if (exec.error) {
                searchScope += space + exec.error;
            }
            keywordMatch = searchScope
                .toLowerCase()
                .includes(filters.keyword.toLowerCase());
        }
        let statusMatch = true;
        if (filters.status && filters.status.length) {
            statusMatch = !!filters.status.find(
                (status) => status.id === exec.status
            );
        }
        let dateMatch = true;
        if (filters.date) {
            const dateFilter = new Date(
                filters.date.year,
                filters.date.month - 1,
                filters.date.day
            );
            dateMatch =
                dateFilter.toDateString() ===
                new Date(exec.time).toDateString();
        }

        let userMatch = true;
        if (filters.executors && filters.executors.length) {
            userMatch = !!filters.executors.find(
                (executor) => executor.id === exec.user
            );
        }

        let envMatch = true;
        if (filters.environments && filters.environments.length) {
            envMatch = !!filters.environments.find(
                (env) => env.id === exec.environment
            );
        }

        let campaignMatch = true;
        if (filters.campaigns && filters.campaigns.length) {
            campaignMatch = !!filters.campaigns.find(
                (camp) =>
                    exec.campaignReport &&
                    camp.id === exec.campaignReport.campaignName
            );
        }

        let tagMatch = true;
        if (filters.tags && filters.tags.length) {
            tagMatch = !!filters.tags.find(tag => exec.tags && exec.tags.includes(tag.id));
        }

        return (
            keywordMatch &&
            statusMatch &&
            dateMatch &&
            userMatch &&
            envMatch &&
            campaignMatch &&
            tagMatch
        );
    }

    openCampaignExecution(execution: Execution, event: MouseEvent) {
        if (execution.campaignReport) {
            event.stopPropagation();
            const url = this.router.serializeUrl(
                this.router.createUrlTree(
                    [
                        '/campaign',
                        execution.campaignReport.campaignId,
                        'executions',
                    ],
                    {
                        queryParams: {
                            open: execution.campaignReport.executionId,
                            active: execution.campaignReport.executionId,
                        },
                    }
                )
            );
            window.open('#' + url, "_blank");
        }
    }
}
