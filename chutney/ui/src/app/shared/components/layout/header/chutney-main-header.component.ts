/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Component, HostBinding, ChangeDetectionStrategy } from '@angular/core';
import { Observable } from 'rxjs';
import { Authorization, User } from '@model';
import { LoginService } from '@core/services';
import { ThemeService } from '@core/theme/theme.service';
import { LayoutOptions } from '@core/layout/layout-options.service';
import { HasAuthorizationDirective } from '../../../directives/hasAuthorization.directive';
import { SearchBarComponent } from '../../search-bar/search-bar.component';
import { AsyncPipe } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
    selector: 'chutney-chutney-main-header',
    templateUrl: './chutney-main-header.component.html',
    styleUrls: ['./chutney-main-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HasAuthorizationDirective,
        SearchBarComponent,
        AsyncPipe,
        TranslatePipe
    ]
})
export class ChutneyMainHeaderComponent{

    public user$: Observable<User>;

    Authorization = Authorization;

    constructor(private loginService: LoginService,
                private themeService: ThemeService,
                public layoutOptions: LayoutOptions) {
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
}
