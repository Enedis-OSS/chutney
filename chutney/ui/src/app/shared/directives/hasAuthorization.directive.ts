/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Directive, Input, TemplateRef, ViewContainerRef } from '@angular/core';

import { LoginService } from '@core/services';

@Directive({
    selector: '[hasAuthorization]',
    standalone: false
})
export class HasAuthorizationDirective {

    private hasApplied = false;

    constructor(
        private templateRef: TemplateRef<any>,
        private viewContainer: ViewContainerRef,
        private loginService: LoginService
    ) {}

    @Input() set hasAuthorization(a: any) {
        const authorizations = a['authorizations'] || (Array.isArray(a) ? a : []);
        const user = a['user'];
        const not: boolean = a['not'] || false;

        const hasAuthorization = this.loginService.hasAuthorization(authorizations, user);
        const condition = (not && !hasAuthorization) || (!not && hasAuthorization)

        if (condition && !this.hasApplied) {
            // Add to DOM
            this.viewContainer.createEmbeddedView(this.templateRef);
            this.hasApplied = true;
        } else if(!condition && this.hasApplied) {
            // Remove from DOM
            this.viewContainer.clear();
            this.hasApplied = false;
        }
    }
}
