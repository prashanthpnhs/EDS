import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {linq, LoggerService} from "eds-common-js";
import {StateService} from "ui-router-ng2";
import {Component} from "@angular/core";
import {Subscription} from "rxjs/Subscription";
import {ServiceService} from "../services/service.service";
import {SftpReaderService} from "./sftpReader.service";
import {SftpReaderChannelStatus} from "./models/SftpReaderChannelStatus";
import {SftpReaderBatchContents} from "./models/SftpReaderBatchContents";
import {OdsSearchDialog} from "../services/odsSearch.dialog";
import {SftpReaderHistoryDialog} from "./sftpReaderHistory.dialog";
import {SftpReaderConfiguration} from "./models/SftpReaderConfiguration";
import {SftpReaderOrgsDialog} from "./sftpReaderOrgs.dialog";

@Component({
    template : require('./sftpReader.html')
})
export class SftpReaderComponent {

    //resultStr: string;
    configurations: SftpReaderConfiguration[];
    refreshingStatusMap: {};
    statusMap: {};

    filterInstanceName: string;
    statuses: SftpReaderChannelStatus[];
    statusesLastRefreshed: Date;
    resultStr: string;
    showRawJson: boolean;
    refreshingStatus: boolean;
    showWarningsOnly: boolean;

    constructor(private $modal: NgbModal,
                protected sftpReaderService: SftpReaderService,
                protected logger: LoggerService,
                protected $state: StateService) {


    }

    ngOnInit() {
        var vm = this;
        vm.filterInstanceName = 'active';
        vm.showWarningsOnly = true;
        vm.refreshInstances();
    }

    refreshInstances() {
        var vm = this;
        vm.refreshingStatusMap = {};
        vm.statusMap = {};

        vm.sftpReaderService.getSftpReaderInstances().subscribe(
            (result) => {
                vm.configurations = result;
                vm.refreshStatuses();
            },
            (error) => {
                vm.logger.error('Failed get SFTP Reader instances', error, 'SFTP Reader');
            }
        )
    }

    refreshStatuses() {
        var vm = this;
        vm.statusesLastRefreshed = new Date();

        for (var i = 0; i < vm.configurations.length; i++) {
            var configuration = vm.configurations[i];
            vm.refreshStatus(configuration);
        }
    }

    refreshStatus(configuration: SftpReaderConfiguration) {
        var vm = this;

        var configurationId = configuration.configurationId;

        vm.refreshingStatusMap[configurationId] = true;

        vm.sftpReaderService.getSftpReaderStatus(configurationId).subscribe(
            (result) => {

                vm.refreshingStatusMap[configurationId] = false;
                vm.calculateIfWarning(result);
                vm.statusMap[configurationId] = result;
            },
            (error) => {

                vm.refreshingStatusMap[configurationId] = false;
                vm.logger.error('Failed to get status for ' + configurationId, error);
            }
        )
    }

    calculateIfWarning(status: SftpReaderChannelStatus) {
        var vm = this;

        //any of these count as a warning
        if (vm.isLastPollAttemptTooOld(status)
            || !status.latestPollingStart
            || status.latestPollingException
            || vm.isLastExtractTooOld(status)
            || !status.latestBatchId
            || vm.filterOrgs(status.completeBatchContents, false).length > 0) {

            status.warning = true;
        }
    }

    isRefreshing(configuration: SftpReaderConfiguration): boolean {
        var vm = this;

        var configurationId = configuration.configurationId;
        return vm.refreshingStatusMap[configurationId];
    }

    /**
     * returns the status for a configuration. Note this uses an array so that we can use the angular
     * for loop, and avoid having to call this function dozens of times.
     */
    getStatusToDisplay(configuration: SftpReaderConfiguration): SftpReaderChannelStatus[] {
        var vm = this;

        var ret = [];

        var configurationId = configuration.configurationId;
        var status = vm.statusMap[configurationId];
        if (status) {
            ret.push(status);
        }

        return ret;
    }

    /*getStatusesToDisplay() {
        var vm = this;
        if (!vm.showWarningsOnly) {
            return vm.statuses;

        } else {
            var ret = [];
            var i;
            for (i=0; i<vm.statuses.length; i++) {
                var status = vm.statuses[i];

                //any of these count as a warning
                if (vm.isLastPollAttemptTooOld(status)
                    || !status.latestPollingStart
                    || status.latestPollingException
                    || vm.isLastExtractTooOld(status)
                    || !status.latestBatchId
                    || vm.filterOrgs(status.completeBatchContents, false).length > 0) {

                    ret.push(status);
                }
            }
            return ret;
        }

    }*/

    filterOrgs(arr: SftpReaderBatchContents[], wantOk: boolean): SftpReaderBatchContents[] {
        var ret = [];

        var i;
        for (i=0; i<arr.length; i++) {
            var c = arr[i];
            if (c.notified == wantOk) {
                ret.push(c);
            }
        }
        return ret;
    }

    /*getPanelClass(status: SftpReaderChannelStatus): string {
        if (status.instanceName) {
            return "panel panel-primary";
        } else {
            return "panel panel-info";
        }
    }*/

    odsSearch() {
        var vm = this;
        OdsSearchDialog.open(vm.$modal);

    }

    isLastPollAttemptTooOld(status: SftpReaderChannelStatus): boolean {
        var vm = this;

        var lastPolled = status.latestPollingStart;
        if (!lastPolled) {
            return true;
        }
        var pollingFrequencySec = status.pollFrequencySeconds;
        var pollingFrequencyMs = pollingFrequencySec * 1000;
        pollingFrequencyMs = (pollingFrequencyMs * 1.1); //add 10% to the poling frequency so it's less touchy

        //var now = new Date();
        var now = vm.statusesLastRefreshed; //use date of refresh rather than current date so it doesn't change if you don't refresh the page
        var lastPolledDiffMs = now.getTime() - lastPolled;

        return lastPolledDiffMs > pollingFrequencyMs;
    }

    isLastExtractTooOld(status: SftpReaderChannelStatus): boolean {
        var vm = this;

        var lastExtract = status.latestBatchReceived;
        if (!lastExtract) {
            return true;
        }

        var dataFrequencyDays = status.dataFrequencyDays;
        var dataFrequencyMs = dataFrequencyDays * 24 * 60 * 60 * 1000;
        dataFrequencyMs = (dataFrequencyMs * 1.5); //add 10% to the poling frequency so it's less touchy

        //var now = new Date();
        var now = vm.statusesLastRefreshed; //use date of refresh rather than current date so it doesn't change if you don't refresh the page
        var lastExtractDiffMs = now.getTime() - lastExtract;

        return lastExtractDiffMs > dataFrequencyMs;
    }

    viewHistory(configuration: SftpReaderConfiguration) {
        var vm = this;
        SftpReaderHistoryDialog.open(vm.$modal, configuration);
    }


    /*ignoreBatchSplit(content: SftpReaderBatchContents, status: SftpReaderChannelStatus) {
        var vm = this;

        var reason = prompt('Enter reason');
        if (reason == null) {
            return;
        }

        vm.sftpReaderService.ignoreBatchSplit(status.latestBatchId, content.batchSplitId, status.id, reason).subscribe(
            (result) => {
                content.error = null;
                content.result = reason;
                content.notified = true;
            },
            (error) => {
                vm.logger.error('Failed to ignore batch split', error, 'SFTP Reader Error');
            }
        )
    }*/

    /*selectAllBatchSpitErrors(status: SftpReaderChannelStatus) {
        var vm = this;

        for (var i=0; i<status.completeBatchContents.length; i++) {
            var batchSplit = status.completeBatchContents[i] as SftpReaderBatchContents;
            if (!batchSplit.notified) {
                batchSplit.selected = true;
            }
        }
    }

    ignoreBatchSplits(status: SftpReaderChannelStatus) {
        var vm = this;

        var selected = [];
        for (var i=0; i<status.completeBatchContents.length; i++) {
            var batchSplit = status.completeBatchContents[i];
            if (batchSplit.selected) {
                selected.push(batchSplit);
            }
        }

        if (selected.length == 0) {
            vm.logger.warning("No errors selected");
            return;
        }

        var reason = prompt('Enter reason');
        if (reason == null) {
            return;
        }

        for (var j=0; j<selected.length; j++) {
            var batchSplit = selected[j] as SftpReaderBatchContents;
            vm.ignoreBatchSplit(status, batchSplit, reason);
        }
    }

    ignoreBatchSplit(status: SftpReaderChannelStatus, batchSplit: SftpReaderBatchContents, reason: string) {
        var vm = this;

        vm.sftpReaderService.ignoreBatchSplit(status.latestBatchId, batchSplit.batchSplitId, status.id, reason).subscribe(
            (result) => {
                batchSplit.error = null;
                batchSplit.result = reason;
                batchSplit.notified = true;
            },
            (error) => {
                vm.logger.error('Failed to ignore batch split', error, 'SFTP Reader Error');
            }
        )
    }*/

    togglePauseAll() {
        var vm = this;
        vm.sftpReaderService.togglePauseAll().subscribe(
            (result) => {
                vm.refreshInstances();
            },
            (error) => {
                vm.logger.error('Failed to toggle pause', error);
            }
        );
    }

    togglePause(configuration: SftpReaderConfiguration) {
        var vm = this;
        var configurationId = configuration.configurationId;
        vm.sftpReaderService.togglePause(configurationId).subscribe(
            (result) => {
                vm.refreshInstances();
            },
            (error) => {
                vm.logger.error('Failed to toggle pause', error);
            }
        );
    }

    viewOrgs(status: SftpReaderChannelStatus) {
        var vm = this;
        vm.viewOrgsImpl(status, status.completeBatchContents);
    }

    viewOrgsOk(status: SftpReaderChannelStatus) {
        var vm = this;
        var orgsOk = vm.filterOrgs(status.completeBatchContents, true);
        vm.viewOrgsImpl(status, orgsOk);
    }

    viewOrgsError(status: SftpReaderChannelStatus) {
        var vm = this;
        var orgsError = vm.filterOrgs(status.completeBatchContents, false);
        vm.viewOrgsImpl(status, orgsError);
    }

    private viewOrgsImpl(status: SftpReaderChannelStatus, orgs: SftpReaderBatchContents[]) {
        var vm = this;
        var configurationId = status.id;
        var batchId = status.completeBatchId;
        SftpReaderOrgsDialog.open(vm.$modal, configurationId, batchId, orgs);
    }
}