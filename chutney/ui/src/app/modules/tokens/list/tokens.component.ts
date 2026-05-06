import { Component } from "@angular/core";
import { Router } from "@angular/router";

@Component({
    selector: 'chutney-tokens',
    templateUrl: './tokens.component.html',
    styleUrls: ['./tokens.component.scss'],
    standalone: false
})
export class TokenListComponent {

    constructor(private router: Router
        ) {
    }

    createToken() {
        this.router.navigate(['/tokens', 'creation']);
    }
}