import {NgModule} from "@angular/core";
import {BrowserModule} from "@angular/platform-browser";
import {FormsModule} from "@angular/forms";
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";
import {DsaComponent} from "./dsa.component";
import {DsaEditorComponent} from "./dsaEditor.component";
import {DsaService} from "./dsa.service";

@NgModule({
    imports : [
        BrowserModule,
        FormsModule,
        NgbModule,
    ],
    declarations : [
        DsaComponent,
        DsaEditorComponent
    ],
    entryComponents : [
    ],
    providers : [
        DsaService
    ]
})
export class DsaModule {}