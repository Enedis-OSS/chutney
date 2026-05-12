import { Component, inject, Input, OnInit, signal } from "@angular/core";
import {ActivatedRoute, Router} from '@angular/router';
@Component({
    selector: 'chutney-tokens-display',
    templateUrl: './tokens-display.component.html',
    styleUrls: ['./tokens-display.component.scss'],
    standalone: false
})
export class TokenDisplayComponent implements OnInit {

    token:string;
    display: boolean;

    ngOnInit() {
        this.token = history.state?.token;
        history.replaceState({}, '', window.location.href);
        this.display = this.token != undefined
    }

}
