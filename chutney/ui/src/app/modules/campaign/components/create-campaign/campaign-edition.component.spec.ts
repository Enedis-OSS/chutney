/*
 * SPDX-FileCopyrightText: 2017-2026 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { FormBuilder } from '@angular/forms';
import { CampaignEditionComponent } from './campaign-edition.component';

describe('CampaignEditionComponent', () => {

    const router = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    const location = jasmine.createSpyObj('Location', ['back', 'getState']);
    const loginService = jasmine.createSpyObj('LoginService', ['hasAuthorization']);
    loginService.hasAuthorization.and.returnValue(true);

    function createComponent(): CampaignEditionComponent {
        return new CampaignEditionComponent(
            null, null, null, null, new FormBuilder(), router, null, null, null, null, null, loginService, location, {}
        );
    }

    beforeEach(() => {
        router.navigate.calls.reset();
        location.back.calls.reset();
    });

    [undefined, 42].forEach(campaignId => {
        it(`should go back to previous screen on cancel (campaign id: ${campaignId})`, () => {
            const component = createComponent();
            component.campaign.id = campaignId;
            component.campaignForm.controls['title'].setValue('modified');

            location.getState.and.returnValue({navigationId: 2});

            component.clear();

            expect(location.back).toHaveBeenCalledTimes(1);
            expect(router.navigate).not.toHaveBeenCalled();
            expect(component.campaignForm.value['title']).toBeNull();
        });
    });

    [{navigationId: 1}, null].forEach(state => {
        it(`should fall back to campaign list on cancel without previous screen (state: ${JSON.stringify(state)})`, () => {
            const component = createComponent();
            location.getState.and.returnValue(state);

            component.clear();

            expect(location.back).not.toHaveBeenCalled();
            expect(router.navigate).toHaveBeenCalledOnceWith(['/campaign']);
        });
    });
});
