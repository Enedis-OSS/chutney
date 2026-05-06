import { Component } from "@angular/core";

@Component({
    selector: 'chutney-tokens-creation',
    templateUrl: './tokens-creation.component.html',
    styleUrls: ['./tokens-creation.component.scss'],
    standalone: false
})
export class TokenCreationComponent {

    createToken() {
        console.log("create token")
    }
}
