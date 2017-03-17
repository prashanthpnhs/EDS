import {Component} from "@angular/core";
import {Organisation} from "../organisationManager/models/Organisation";
import {AdminService} from "../administration/admin.service";
import {RegionService} from "./region.service";
import {LoggerService} from "../common/logger.service";
import {Transition, StateService} from "ui-router-ng2";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {Region} from "./models/Region";
import {OrganisationManagerPickerDialog} from "../organisationManager/organisationManagerPicker.dialog";
import {RegionPickerDialog} from "./regionPicker.dialog";
import {Marker} from "./models/Marker";
import {OrganisationManagerService} from "../organisationManager/organisationManager.service";

@Component({
    template: require('./regionEditor.html')
})
export class RegionEditorComponent {

    region : Region = <Region>{};
    organisations : Organisation[];
    parentRegions : Region[];
    childRegions : Region[];
    lat: number = 54.4347266;
    lng: number = -4.7194005;
    zoom: number = 6.01;
    markers: Marker[];


    constructor(private $modal: NgbModal,
                private state : StateService,
                private log:LoggerService,
                private organisationManagerService : OrganisationManagerService,
                private adminService : AdminService,
                private regionService : RegionService,
                private transition : Transition
    ) {
        this.performAction(transition.params()['itemAction'], transition.params()['itemUuid']);
        console.log(transition);
    }

    protected performAction(action:string, itemUuid:string) {
        switch (action) {
            case 'add':
                this.create(itemUuid);
                break;
            case 'edit':
                this.load(itemUuid);
                break;
        }
    }

    create(uuid : string) {
        this.region = {
            name : ''
        } as Region;
    }

    load(uuid : string) {
        var vm = this;
        vm.regionService.getRegion(uuid)
            .subscribe(result =>  {
                    vm.region = result;
                    vm.getRegionOrganisations();
                    vm.getParentRegions();
                    vm.getChildRegions();
                    vm.getOrganisationMarkers();
                },
                error => vm.log.error('Error loading', error, 'Error')
            );
    }

    save(close : boolean) {
        var vm = this;

        // Populate organisations before save
        vm.region.organisations = {};
        for (var idx in this.organisations) {
            var organisation : Organisation = this.organisations[idx];
            this.region.organisations[organisation.uuid] = organisation.name;
        }

        //populate Parent Regions
        vm.region.parentRegions = {};
        for (var idx in this.parentRegions) {
            var region : Region = this.parentRegions[idx];
            this.region.parentRegions[region.uuid] = region.name;
        }

        //populate Parent Regions
        vm.region.childRegions = {};
        for (var idx in this.childRegions) {
            var region : Region = this.childRegions[idx];
            this.region.childRegions[region.uuid] = region.name;
        }

        vm.regionService.saveRegion(vm.region)
            .subscribe(saved => {
                    vm.adminService.clearPendingChanges();
                    vm.log.success('Item saved', vm.region, 'Saved');
                    if (close) { vm.state.go(vm.transition.from()); }
                },
                error => vm.log.error('Error saving', error, 'Error')
            );
    }

    close() {
        console.log(this.transition);
        this.adminService.clearPendingChanges();
        this.state.go(this.transition.from());
    }

    private getRegionOrganisations() {
        var vm = this;
        vm.regionService.getRegionOrganisations(vm.region.uuid)
            .subscribe(
                result => vm.organisations = result,
                error => vm.log.error('Failed to load region organisations', error, 'Load region organisation')
            );
    }

    private getParentRegions() {
        var vm = this;
        vm.regionService.getParentRegions(vm.region.uuid)
            .subscribe(
                result => vm.parentRegions = result,
                error => vm.log.error('Failed to load parent regions', error, 'Load parent regions')
            );
    }

    private getChildRegions() {
        var vm = this;
        vm.regionService.getChildRegions(vm.region.uuid)
            .subscribe(
                result => vm.childRegions = result,
                error => vm.log.error('Failed to load child regions', error, 'Load child regions')
            );
    }

    private getOrganisationMarkers() {
        var vm = this;
        vm.organisationManagerService.getOrganisationMarkers(vm.region.uuid)
            .subscribe(
                result => vm.markers = result,
                error => vm.log.error('Failed to load oranisation markers', error, 'Load organisation Markers')
            );
    }

    private editOrganisations() {
        var vm = this;
        OrganisationManagerPickerDialog.open(vm.$modal, vm.organisations)
            .result.then(function (result : Organisation[]) {
            vm.organisations = result;
        });
    }

    private editParentRegions() {
        var vm = this;
        RegionPickerDialog.open(vm.$modal, vm.parentRegions)
            .result.then(function (result : Region[]) {
            vm.parentRegions = result;
        });
    }

    private editChildRegions() {
        var vm = this;
        RegionPickerDialog.open(vm.$modal, vm.childRegions)
            .result.then(function (result : Region[]) {
            vm.childRegions = result;
        });
    }
}
