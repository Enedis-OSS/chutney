/*
 * SPDX-FileCopyrightText: 2017-2024 Enedis
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

import { Observable, Observer, Subscription } from 'rxjs';
import { Injectable } from '@angular/core';
import { filter, share, switchMap } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class EventManagerService {

    observable: Observable<any>;
    observer: Observer<any>;

    constructor() {
        this.observable = Observable.create((observer: Observer<any>) => {
            this.observer = observer;
        }).pipe(share());
    }

    /**
     * Method to broadcast the event to observer
     */
    broadcast(event) {
        if (this.observer != null) {
            this.observer.next(event);
        }
    }

    /**
     * Method to unsubscribe the subscription
     */
    destroy(subscriber: Subscription) {
        subscriber && subscriber.unsubscribe();
    }


    /**
     * Method to subscribe to an event with callback
     */
    listen(eventName, callback: (data) => Observable<any>) {
        return this.observable.pipe(
            filter((event) => event.name === eventName),
            switchMap(callback)
        );
    }
}
