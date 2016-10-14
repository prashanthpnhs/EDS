import {UIPatient} from "./resources/admin/UIPatient";
import {UIProblem} from "./resources/clinical/UIProblem";
import {UIEncounter} from "./resources/clinical/UIEncounter";
import {UICondition} from "./resources/clinical/UICondition";
import {linq} from "../../blocks/linq";
import {UIDiary} from "./resources/clinical/UIDiary";

export class UIPatientRecord {
    patient: UIPatient;
    conditions: UICondition[];
    problems: UIProblem[];
    encounters: UIEncounter[];
    diary: UIDiary[];

    constructor()
    constructor(patient: UIPatient)
    constructor(patient?: UIPatient) {
        this.patient = patient;
    }

    public getActiveProblems(): UIProblem[] {
        return linq(this.problems)
            .Where(t => (!t.hasAbated))
            .ToArray();
    }

    public hasActiveProblems(): boolean {
        return (this.getActiveProblems().length > 0);
    }

    public getPastProblems(): UIProblem[] {
        return linq(this.problems)
            .Where(t => t.hasAbated)
            .ToArray();
    }

    public hasPastProblems(): boolean {
        return (this.getPastProblems().length > 0);
    }

    public hasDiaryEntries(): boolean {
        return (this.diary.length > 0);
    }
}