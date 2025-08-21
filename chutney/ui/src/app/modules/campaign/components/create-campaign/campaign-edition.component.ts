/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Observable, Subject, takeUntil } from 'rxjs';
import { CdkDragDrop, moveItemInArray, } from '@angular/cdk/drag-drop';
import { Campaign, CampaignScenario, Dataset, JiraScenario, JiraScenarioLinks, ScenarioIndex } from '@model';
import {
    CampaignService,
    DataSetService,
    EnvironmentService,
    JiraPluginConfigurationService,
    JiraPluginService,
    ScenarioService
} from '@core/services';
import { distinct, flatMap, newInstance } from '@shared/tools/array-utils';
import { isNotEmpty } from '@shared';
import { DROPDOWN_SETTINGS } from '@core/model/dropdown-settings';
import { IDropdownSettings } from 'ng-multiselect-dropdown';
import { ListItem } from 'ng-multiselect-dropdown/multiselect.model';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import {
    ScenarioJiraLinksModalComponent
} from '@modules/scenarios/components/scenario-jira-links-modal/scenario-jira-links-modal.component';

@Component({
    selector: 'chutney-campaign-edition',
    templateUrl: './campaign-edition.component.html',
    styleUrls: ['./campaign-edition.component.scss'],
    standalone: false,
})
export class CampaignEditionComponent implements OnInit, OnDestroy {

    campaignForm: FormGroup;

    campaign = new Campaign();
    campaignId: number;
    submitted: boolean;
    scenarios: Array<ScenarioIndex> = [];
    scenariosToAdd: Array<{ 'scenarioId': ScenarioIndex, 'dataset': ListItem }> = [];
    errorMessage: any;
    datasets: ListItem[] = [];
    dropdownDatasetSettings: IDropdownSettings
    error: boolean = false;

    private unsubscribeSub$: Subject<void> = new Subject();

    environments: Array<string>;
    selectedEnvironment: string;

    itemList: ListItem[] = [];
    jiraItemList: ListItem[] = [];
    selectedTags: string[] = [];
    jiraSelectedTags: string[] = [];
    datasetId: string;
    jiraId: string;
    jiraLinks: Map<string, JiraScenarioLinks> = new Map();
    jiraUrl = '';
    jiraScenarios: JiraScenario[] = [];
    jiraScenariosToExclude: Array<ScenarioIndex> = [];

    private jiraEditText: string = '';

    constructor(
        private campaignService: CampaignService,
        private scenarioService: ScenarioService,
        private jiraLinkService: JiraPluginService,
        private jiraPluginConfigurationService: JiraPluginConfigurationService,
        private formBuilder: FormBuilder,
        private router: Router,
        private route: ActivatedRoute,
        private environmentService: EnvironmentService,
        private datasetService: DataSetService,
        private translateService: TranslateService,
        private modalService: NgbModal,
        @Inject(DROPDOWN_SETTINGS) public dropdownSettings: IDropdownSettings
    ) {
        this.campaignForm = this.formBuilder.group({
            title: ['', Validators.required],
            description: '',
            tags: [],
            jiratags: [],
            campaignTags: '',
            scenariosFilter: '',
            parallelRun: false,
            retryAuto: false,
            jiraId: '',
            onlyLinkedScenarios: false
        });
    }

    ngOnInit() {
        this.dropdownDatasetSettings = {...this.dropdownSettings, singleSelection: true};
        this.dropdownDatasetSettings.textField = 'text'; // not mandatory

        this.submitted = false;
        this.loadEnvironment();
        this.initTranslations();
        this.route.params
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe((params) => {
                this.campaignId = params['id'];
                this.initJiraPlugin();
                this.loadAllScenarios();
            });
        this.datasetService.findAll().subscribe((res: Array<Dataset>) => {
            this.datasets = res.map(dataset => {
                return {'id': dataset.id, 'text': dataset.name}
            });
        });
    }

    ngOnDestroy() {
        this.unsubscribeSub$.next();
        this.unsubscribeSub$.complete();
    }

    drop(event: CdkDragDrop<Array<{ 'scenarioId': ScenarioIndex, 'dataset': ListItem }>>) {
        moveItemInArray(this.scenariosToAdd, event.previousIndex, event.currentIndex);
    }

    onItemSelect(item: any) {
        this.selectedTags.push(item.text);
        this.selectedTags = newInstance(this.selectedTags);
    }

    OnItemDeSelect(item: any) {
        this.selectedTags.splice(this.selectedTags.indexOf(item.text), 1);
        this.selectedTags = newInstance(this.selectedTags);
    }

    OnItemDeSelectAll() {
        this.selectedTags = newInstance([]);
    }

    onJiraItemSelect(item: any) {
        this.jiraSelectedTags.push(item.text);
        this.jiraSelectedTags = newInstance(this.jiraSelectedTags);
        this.jiraFilter();
    }

    OnJiraItemDeSelect(item: any) {
        this.jiraSelectedTags.splice(this.jiraSelectedTags.indexOf(item.text), 1);
        this.jiraSelectedTags = newInstance(this.jiraSelectedTags);
        this.jiraFilter();
    }

    OnJiraItemDeSelectAll() {
        this.jiraSelectedTags = newInstance([]);
        this.jiraFilter();
    }

    load(id: number) {
        if (id != null) {
            this.campaignService.find(id)
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    next: (campaignFound) => {
                        this.campaign = campaignFound;
                        this.campaignForm.controls['title'].setValue(this.campaign.title);
                        this.campaignForm.controls['description'].setValue(this.campaign.description);
                        this.campaignForm.controls['parallelRun'].setValue(this.campaign.parallelRun);
                        this.campaignForm.controls['retryAuto'].setValue(this.campaign.retryAuto);
                        this.campaignForm.controls['campaignTags'].setValue(this.campaign.tags);
                        this.selectedEnvironment = this.campaign.environment;
                        this.setCampaignScenarios();
                        this.datasetId = this.campaign.datasetId;
                    },
                    error: (error) => {
                        this.errorMessage = error._body;
                    }
                });
        }
    }

    loadAllScenarios() {
        this.scenarioService.findScenarios()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.scenarios = res;
                    this.load(this.campaignId);
                    this.initTags();
                },
                error: (error) => {
                    this.errorMessage = error.error;
                }
            });
    }

    private initTags() {
        const allTagsInScenario: string[] = distinct(flatMap(this.scenarios, (sc) => sc.tags)).sort();

        allTagsInScenario.forEach((currentValue, index) => {
            this.itemList.push({id: index, text: currentValue});
        });
    }

    loadEnvironment() {
        this.environmentService.names()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res) => {
                    this.environments = res.sort((t1, t2) => t1.toUpperCase() > t2.toUpperCase() ? 1 : 0);
                },
                error: (error) => {
                    this.errorMessage = error.error;
                }
            });
    }

    private loadJiraLink() {
        this.jiraLinkService.findByCampaignId(this.campaignId)
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (jiraId) => {
                    this.campaignForm.controls['jiraId'].setValue(jiraId);
                    this.refreshJiraScenarios();
                },
                error: (error) => {
                    this.errorMessage = error.error;
                }
            });
    }

    private initJiraPlugin() {
        this.jiraPluginConfigurationService.getUrl()
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe((r) => {
                if (r !== '') {
                    this.jiraUrl = r;
                    this.loadJiraLink();
                    this.jiraLinkService.findScenarios()
                        .pipe(takeUntil(this.unsubscribeSub$))
                        .subscribe(
                            (result) => {
                                this.jiraLinks = result;
                            }
                        );
                }
            });
    }

    private initTranslations() {
        this.translateService.get('scenarios.jira.edit').subscribe(s => {
            this.jiraEditText = s;
        });
    }

    showScenarioJiraLinks(scenario: ScenarioIndex) {
        const modalRef = this.modalService.open(ScenarioJiraLinksModalComponent, {size: 'lg'});
        modalRef.componentInstance.scenario = scenario;
        modalRef.componentInstance.jiraUrl = this.jiraUrl;
        modalRef.closed.subscribe(
            res => this.jiraLinks.set(scenario.id, res)
        );
    }

    hasJiraLinks(scenario: ScenarioIndex): boolean {
        let jiraLinks = this.jiraLinks.get(scenario.id);
        return jiraLinks != null && (
            (jiraLinks.id?.length > 0) || (jiraLinks.datasetLinks != null && Object.keys(jiraLinks.datasetLinks).length > 0)
        );
    }

    jiraLinksTitleContent(scenario: ScenarioIndex): string {
        var titleContent = this.jiraEditText;
        const jiraLinks = this.jiraLinks.get(scenario.id);
        if (jiraLinks != null) {
            const jiraId = jiraLinks.id;
            if (jiraId?.length > 0) {
                titleContent += `\n\n${jiraId}`;
            }
            if (jiraLinks.datasetLinks != null && Object.keys(jiraLinks.datasetLinks).length > 0) {
                titleContent += `\n\n${Object.keys(jiraLinks.datasetLinks).map(key => `${key}     ${jiraLinks.datasetLinks[key]}`).join('\n')}`;
            }
        }
        return titleContent;
    }

    trackViewedScenarios(index: number, scenario: ScenarioIndex) {
        return parseInt(scenario.id) * (this.hasJiraLinks(scenario) ? -1 : 1);
    }

    getJiraLastExecutionStatus(id: string) {
        const jiraScenario = this.jiraScenarios.filter(s => s.chutneyId === id);
        if (jiraScenario.length > 0) {
            return jiraScenario[0].executionStatus;
        } else {
            return '';
        }
    }

    getJiraLastExecutionStatusClass(id: string) {
        const status = this.getJiraLastExecutionStatus(id);
        switch (status) {
            case 'PASS' :
                return 'bg-success';
            case 'FAIL' :
                return 'bg-danger';
            default :
                return 'bg-secondary';
        }
    }

    hasJiraId() {
        return this.campaignForm.value['jiraId'] != null && this.campaignForm.value['jiraId'] !== '';
    }

    refreshJiraScenarios() {
        if (this.campaignForm.value['jiraId'] !== '') {
            this.jiraLinkService.findTestExecScenarios(this.campaignForm.value['jiraId'])
                .pipe(takeUntil(this.unsubscribeSub$))
                .subscribe({
                    next: (result) => {
                        this.jiraScenarios = result;
                        let index = 0;
                        this.jiraScenarios.forEach((currentValue) => {
                            if (isNotEmpty(currentValue.executionStatus)) {
                                this.jiraItemList.push({'id': index, 'text': currentValue.executionStatus});
                                index++;
                            }
                        });
                        this.jiraFilter();
                    },
                    error: (error) => {
                        this.errorMessage = error.error;
                        this.clearJiraScenarios();
                    }
                });
        } else {
            this.clearJiraScenarios();
        }
    }

    clearJiraScenarios() {
        this.jiraScenarios = [];
        this.jiraScenariosToExclude = [];
        this.campaignForm.controls['onlyLinkedScenarios'].setValue(false);
    }

    jiraFilter() {
        if (this.campaignForm.controls['onlyLinkedScenarios'].value === true) {
            this.jiraScenariosToExclude = this.scenarios.filter((item) => {
                let jiraTagFilter = false;
                if (this.jiraSelectedTags.length > 0) {
                    jiraTagFilter = (this.jiraScenarios.find(s => item.id === s.chutneyId &&
                        this.jiraSelectedTags.includes(s.executionStatus))) === undefined;
                }
                return (!this.jiraScenarios.map(j => j.chutneyId).includes(item.id)) || jiraTagFilter;
            });
        } else {
            this.jiraScenariosToExclude = [];
        }
    }

    clear() {
        this.campaignForm.reset();
        let url: string;
        if (this.campaign.id) {
            url = '/campaign/' + this.campaign.id + '/executions';
        } else {
            url = '/campaign';
        }
        this.router.navigateByUrl(url);
    }

    saveCampaign() {
        this.submitted = true;
        const formValue = this.campaignForm.value;

        if (this.campaignForm.invalid) {
            return;
        }

        this.campaign.title = formValue['title'];
        this.campaign.description = formValue['description'];
        this.campaign.environment = this.selectedEnvironment;
        this.campaign.parallelRun = formValue['parallelRun'];
        this.campaign.retryAuto = formValue['retryAuto'];
        this.campaign.datasetId = (!this.datasetId || this.datasetId.trim() === '') ? null : this.datasetId
        const tags = formValue['campaignTags'] + '';
        this.campaign.tags = tags.length !== 0 ? tags.split(',') : [];

        this.setCampaignScenariosIdsToAdd(this.scenariosToAdd);
        if (!this.error) {
            if (this.campaign.id != null) {
                this.subscribeToSaveResponse(
                    this.campaignService.update(this.campaign));
            } else {
                this.subscribeToSaveResponse(
                    this.campaignService.create(this.campaign));
            }
        }
    }

    setCampaignScenarios() {
        this.scenariosToAdd = [];
        if (this.campaign.scenarios) {
            for (const campaignScenario of this.campaign.scenarios) {
                const scenarioFound = this.scenarios.find((x) => x.id === campaignScenario.scenarioId);
                this.scenariosToAdd.push({
                    scenarioId: scenarioFound,
                    dataset: (campaignScenario.datasetId ? {
                        id: campaignScenario.datasetId,
                        text: campaignScenario.datasetId
                    } : null)
                });
            }
        }
    }

    setCampaignScenariosIdsToAdd(scenariosToAdd: Array<{ scenarioId: ScenarioIndex, dataset: ListItem }>) {
        this.error = false;
        this.campaign.scenarios = [];
        for (const scenario of scenariosToAdd) {
            if (!this.campaign.scenarios.some((s) => s.scenarioId === scenario.scenarioId.id && ((scenario.dataset === null && s.datasetId == null) || (scenario.dataset !== null && s.datasetId === scenario.dataset.id)))) {
                this.campaign.scenarios.push(new CampaignScenario(scenario.scenarioId.id, scenario.dataset ? scenario.dataset.id as string : null));
            } else {
                this.error = true;
                const messageKey = scenario.dataset ? 'campaigns.edition.errors.scenarioDatasetDuplicate' : 'campaigns.edition.errors.scenarioDatasetNullDuplicate'
                let messageParams = {
                    scenarioId: scenario.scenarioId.id,
                    datasetId: scenario.dataset ? scenario.dataset.id : 'NULL'
                }
                this.translateService.get(messageKey, messageParams).subscribe((msg: string) => {
                    this.errorMessage = msg
                });
                break;
            }
        }
        if (this.error) {
            // Clear campaign.scenarios
            this.campaign.scenarios.length = 0
        }
    }

    addScenario(scenario: ScenarioIndex) {
        this.scenariosToAdd.push({scenarioId: scenario, dataset: null});
        this.refreshForPipe();
    }

    removeScenario(scenario: { 'scenarioId': ScenarioIndex, 'dataset': ListItem }) {
        const index = this.scenariosToAdd.findIndex(scenarioElement => scenarioElement === scenario)
        this.scenariosToAdd.splice(index, 1);
        this.refreshForPipe();
    }

    private subscribeToSaveResponse(result: Observable<Campaign>) {
        result
            .pipe(takeUntil(this.unsubscribeSub$))
            .subscribe({
                next: (res: Campaign) => this.onSaveSuccess(res),
                error: (error) => this.onSaveError(error)
            });
    }

    private onSaveSuccess(result: Campaign) {
        this.submitted = false;
        const url = '/campaign/' + result.id + '/executions';
        this.updateJiraLink(result.id).subscribe({
            next: () => {
                this.router.navigateByUrl(url);
            },
            error: (error) => {this.errorMessage = error.error}
        });

    }

    private onSaveError(error) {
        console.log(error);
        try {
            error.json();
        } catch (exception) {
            error.message = error.error;
        }
        this.submitted = false;
        this.errorMessage = error.message;
    }

    private refreshForPipe() {
        // force instance to change for pipe refresh
        this.scenariosToAdd = Object.assign([], this.scenariosToAdd);
    }

    setSelectedEnvironment(event: string) {
        this.selectedEnvironment = event;
    }

    selectDataset(datasetId: string) {
        this.datasetId = datasetId;
    }

    selectDatasetScenario(dataset: ListItem, scenario: { 'scenarioId': ScenarioIndex, 'dataset': ListItem }) {
        const scenarioSelected = this.scenariosToAdd.find(scenarioElement => scenarioElement === scenario)
        scenarioSelected.dataset = dataset;
        this.refreshForPipe();
    }

    deselectDatasetScenario(scenario: { 'scenarioId': ScenarioIndex, 'dataset': ListItem }) {
        const scenarioSelected = this.scenariosToAdd.find(scenarioElement => scenarioElement === scenario)
        scenarioSelected.dataset = null;
        this.refreshForPipe();
    }

    private updateJiraLink(campaignId: number): Observable<JiraScenario> {
        this.jiraId = this.campaignForm.value['jiraId'];
        return this.jiraLinkService.saveForCampaign(campaignId, this.jiraId)
            .pipe(takeUntil(this.unsubscribeSub$));
    }
}
