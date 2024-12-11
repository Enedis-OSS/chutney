/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, HostBinding, OnInit } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { User } from '@model';
import { LoginService } from '@core/services';
import { ThemeService } from '@core/theme/theme.service';
import { LayoutOptions } from '@core/layout/layout-options.service';
import { TypeaheadMatch } from 'ngx-bootstrap/typeahead';
import { SearchService } from '@core/services/search.service';

@Component({
    selector: 'chutney-chutney-main-header',
    templateUrl: './chutney-main-header.component.html',
    styleUrls: ['./chutney-main-header.component.scss']
})
export class ChutneyMainHeaderComponent implements OnInit {

    public user$: Observable<User>;
    keyword: string;

    constructor(private loginService: LoginService,
                private themeService: ThemeService,
                public layoutOptions: LayoutOptions,
                private searchService: SearchService) {
        this.user$ = this.loginService.getUser();
    }

    @HostBinding('class.isActive')
    get isActiveAsGetter() {
        return this.isActive;
    }

    isActive: boolean;


    toggleSidebar() {
        this.layoutOptions.toggleSidebar = !this.layoutOptions.toggleSidebar;
    }

    ngOnInit(): void {
    }

    logout() {
        this.loginService.logout();
    }

    public switchTheme() {
        this.themeService.switchTheme();
    }

    isLight(): boolean {
        return this.themeService.isLight()
    }

    isDark(): boolean {
        return !this.isLight()
    }

    isSearching(event: boolean) {

    }

    search(): Observable<any> {
        if (!this.keyword) {
            return EMPTY;
        }
        return this.searchService.search(this.keyword);
    }

    onSelectSearchHit(hit: TypeaheadMatch<any>) {

    }
}
