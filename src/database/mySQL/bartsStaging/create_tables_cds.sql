use staging_barts;

drop table if exists cds_inpatient;
drop table if exists cds_inpatient_latest;
drop table if exists cds_outpatient;
drop table if exists cds_outpatient_latest;
drop table if exists cds_emergency;
drop table if exists cds_emergency_latest;
drop table if exists cds_tail;
drop table if exists cds_tail_latest;


-- records from sus inpatient files are written to this table
create table cds_inpatient
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                bigint       NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date              datetime     NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                int          NOT NULL COMMENT 'from CDSUpdateType',
    mrn                            varchar(10)  NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                     varchar(10)  NOT NULL COMMENT 'from NHSNumber',
    withheld                       bool         COMMENT 'True if id is withheld',
    date_of_birth                  date         COMMENT 'from PersonBirthDate',
    consultant_code                varchar(20)  NOT NULL COMMENT 'GMC number of consultant, from ConsultantCode',

    patient_pathway_identifier     varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',
    spell_number                   varchar(12),
    admission_method_code          varchar(12)   COMMENT 'LKP_CDS_ADMISS_METHOD',
    admission_source_code          varchar(12)   COMMENT 'LKP_CDS_ADMISS_SOURCE',
    patient_classification         char(1)      COMMENT 'LKP_CDS_PATIENT_CLASS',
    spell_start_date               datetime     COMMENT 'start date and time of hospital spell',
    episode_number                 varchar(2),
    episode_start_site_code        varchar(12)  COMMENT 'location at start of episode',
    episode_start_ward_code        varchar(12)  COMMENT 'ward at start of episode',
    episode_start_date             datetime     COMMENT 'episode start date and time',
    episode_end_site_code          varchar(12)  COMMENT 'location at end of episode',
    episode_end_ward_code          varchar(12)  COMMENT 'ward at end of episode',
    episode_end_date               datetime     COMMENT 'episode end date and time',
    discharge_date                 datetime     COMMENT 'date and time of discharge',
    discharge_destination_code     varchar(12)   COMMENT 'LKP_CDS_DISCH_DEST',
    discharge_method               char(1)      COMMENT 'LKP_CDS_DISCH_METHOD',

    -- store any diagnosis and procedure data
    primary_diagnosis_ICD          varchar(6),
    secondary_diagnosis_ICD        varchar(6),
    other_diagnosis_ICD            mediumtext,
    primary_procedure_OPCS         varchar(4),
    primary_procedure_date         date,
    secondary_procedure_OPCS       varchar(4),
    secondary_procedure_date       date,
    other_procedures_OPCS          mediumtext,

    lookup_person_id               int          COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id int          COMMENT 'personnel ID looked up using consultant code',
    audit_json                     mediumtext   null COMMENT 'Used for Audit Purposes',
    CONSTRAINT pk_cds_inpatient PRIMARY KEY (exchange_id, cds_unique_identifier)
);
-- index to make it easier to find last checksum for a CDS inpatient record
CREATE INDEX ix_cds_inpatient_checksum_helper on cds_inpatient (cds_unique_identifier, dt_received);

/*
create table cds_inpatient_latest
(
    exchange_id                    char(36)     NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                    datetime     NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                bigint       NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date              datetime     NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier          varchar(50)  NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                int          NOT NULL COMMENT 'from CDSUpdateType',
    mrn                            varchar(10)  NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                     varchar(10)  NOT NULL COMMENT 'from NHSNumber',
    withheld                       bool         COMMENT 'True if id is withheld',
    date_of_birth                  date COMMENT 'from PersonBirthDate',
    consultant_code                varchar(20)  COMMENT 'GMC number of consultant, from ConsultantCode',

    patient_pathway_identifier     varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',
    spell_number                   varchar(12),
    admission_method_code          varchar(2)   COMMENT 'LKP_CDS_ADMISS_METHOD',
    admission_source_code          varchar(2)   COMMENT 'LKP_CDS_ADMISS_SOURCE',
    patient_classification         char(1)      COMMENT 'LKP_CDS_PATIENT_CLASS',
    spell_start_date               date         COMMENT 'start date of hospital spell: CCYYMMDD',
    spell_start_time               time         COMMENT 'start time of hospital spell: HHSSMM',
    episode_number                 varchar(2),
    episode_start_site_code        varchar(12)  COMMENT 'location at start of episode',
    episode_start_ward_code        varchar(12)  COMMENT 'ward at start of episode',
    episode_start_date             date         COMMENT 'episode start date: CCYYMMDD',
    episode_start_time             time         COMMENT 'episode start time: HHSSMM',
    episode_end_site_code          varchar(12)  COMMENT 'location at end of episode',
    episode_end_ward_code          varchar(12)  COMMENT 'ward at end of episode',
    episode_end_date               date         COMMENT 'episode end date: CCYYMMDD',
    episode_end_time               time         COMMENT 'episode end time: HHSSMM',
    discharge_date                 date         COMMENT 'date of discharge: CCYYMMDD',
    discharge_time                 time         COMMENT 'time of discharge: HHSSMM',
    discharge_destination_code     varchar(2)   COMMENT 'LKP_CDS_DISCH_DEST',
    discharge_method               char(1)      COMMENT 'LKP_CDS_DISCH_METHOD',

    -- store any diagnosis and procedure data
    primary_diagnosis_ICD          varchar(6),
    secondary_diagnosis_ICD        varchar(6),
    other_diagnosis_ICD            mediumtext,
    primary_procedure_OPCS         varchar(4),
    primary_procedure_date         date,
    secondary_procedure_OPCS       varchar(4),
    secondary_procedure_date       date,
    other_procedures_OPCS          mediumtext,

    lookup_person_id               int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id int COMMENT 'personnel ID looked up using consultant code',
    audit_json                     mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_cds_inpatient_latest PRIMARY KEY (cds_unique_identifier)
);
CREATE INDEX ix_cds_inpatient_latest_join_helper on cds_inpatient_latest (exchange_id, cds_unique_identifier);
*/

-- records from sus outpatient files are written to this table
create table cds_outpatient
(
    exchange_id                     char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                     datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                 bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date               datetime    NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier           varchar(50) NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                 int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                             varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                      varchar(10) NOT NULL COMMENT 'from NHSNumber',
    withheld                        bool COMMENT 'True if id is withheld',
    date_of_birth                   date COMMENT 'from PersonBirthDate',
    consultant_code                 varchar(20) NOT NULL COMMENT 'GMC number of consultant, from ConsultantCode',

    patient_pathway_identifier      varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',
    appt_attendance_identifier      varchar(20),
    appt_attended_code              char(1)      COMMENT 'Attended or DNA code: LKP_CDS_ATTENDED',
    appt_outcome_code               char(1)      COMMENT 'LKP_CDS_ATTENDANCE_OUTCOME',
    appt_date                       datetime     COMMENT 'date and time of the outpatient appointment',
    appt_site_code                  varchar(12)  COMMENT 'location of appointment',

    -- store any diagnosis and procedure data
    primary_diagnosis_ICD           varchar(6),
    secondary_diagnosis_ICD         varchar(6),
    other_diagnosis_ICD             mediumtext,
    primary_procedure_OPCS          varchar(4),
    primary_procedure_date          date,
    secondary_procedure_OPCS        varchar(4),
    secondary_procedure_date        date,
    other_procedures_OPCS           mediumtext,

    lookup_person_id                int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id  int COMMENT 'personnel ID looked up using consultant code',
    audit_json                      mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_cds_outpatient  PRIMARY KEY (exchange_id, cds_unique_identifier)
);
-- index to make it easier to find last checksum for a CDS outpatient record
CREATE INDEX ix_cds_outpatient_checksum_helper on cds_outpatient (cds_unique_identifier, dt_received);

/*
create table cds_outpatient_latest
(
    exchange_id                     char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                     datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum                 bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date               datetime    NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier           varchar(50) NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type                 int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                             varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                      varchar(10) NOT NULL COMMENT 'from NHSNumber',
    withheld                        bool COMMENT 'True if id is withheld',
    date_of_birth                   date COMMENT 'from PersonBirthDate',
    consultant_code                 varchar(20) NOT NULL COMMENT 'GMC number of consultant, from ConsultantCode',

    patient_pathway_identifier      varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',
    appt_attendance_identifier      varchar(12),
    appt_attended_code              char(1)     COMMENT 'Attended or DNA code: LKP_CDS_ATTENDED',
    appt_outcome_code               char(1)     COMMENT 'LKP_CDS_ATTENDANCE_OUTCOME',
    appt_date                       date        COMMENT 'date of the outpatient appointment: CCYYMMDD',
    appt_time                       time        COMMENT 'time of the outpatient appointment: HHSSMM',
    appt_site_code                  varchar(12) COMMENT 'location of appointment',

    -- store any diagnosis and procedure data
    primary_diagnosis_ICD           varchar(6),
    secondary_diagnosis_ICD         varchar(6),
    other_diagnosis_ICD             mediumtext,
    primary_procedure_OPCS          varchar(4),
    primary_procedure_date          date,
    secondary_procedure_OPCS        varchar(4),
    secondary_procedure_date        date,
    other_procedures_OPCS           mediumtext,

    lookup_person_id                int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id  int COMMENT 'personnel ID looked up using consultant code',
    audit_json                      mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_cds_outpatient_latest PRIMARY KEY (cds_unique_identifier)
);
CREATE INDEX ix_cds_outpatient_latest_join_helper on cds_outpatient_latest (exchange_id, cds_unique_identifier);
*/

-- records from sus emergency care dataset files are written to this table
create table cds_emergency
(
    exchange_id           char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received           datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum       bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date     datetime    NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier varchar(50) NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type       int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                   varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number            varchar(10) NOT NULL COMMENT 'from NHSNumber',
    withheld              bool COMMENT 'True if id has a withheld reason',
    date_of_birth         date COMMENT 'from PersonBirthDate',
    -- consultant_code       varchar(20) NOT NULL COMMENT 'GMC number of consultant, from AEStaffMemberCode',  NOT PRESENT

    patient_pathway_identifier          varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',

    department_type                     varchar(20),
    ambulance_incident_number           varchar(50),
    ambulance_trust_organisation_code   varchar(12),
    attendance_identifier               varchar(20),
    arrival_mode                        varchar(12),
    attendance_category                 varchar(12),
    attendance_source                   varchar(12),
    arrival_date                        datetime,
    initial_assessment_date             datetime,
    chief_complaint                     varchar(12),
    seen_for_treatment_date             datetime,
    decided_to_admit_date               datetime,
    treatment_function_code             varchar(12),
    discharge_status                    varchar(12) COMMENT 'Snomed coded',
    discharge_destination               varchar(12),
    conclusion_date                     datetime,
    departure_date                      datetime,
    mh_classifications                  mediumtext   COMMENT 'code~start datetime~end datetime in upto 10 | delimetered groups',
    diagnosis                           mediumtext   COMMENT 'code in upto 20 | delimetered groups',
    investigations                      mediumtext   COMMENT 'code~datetime in upto 10 | delimetered groups',
    treatments                          mediumtext   COMMENT 'code~datetime in upto 10 | delimetered groups',
    referred_to_services                mediumtext   COMMENT 'Snomed code~request date~assessment date  in upto 10 | delimetered groups',
    safeguarding_concerns               mediumtext   COMMENT 'Snomed code in upto 10 | delimetered groups',

    lookup_person_id               int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    -- lookup_consultant_personnel_id int COMMENT 'personnel ID looked up using consultant code',  NO Staff/Consultant code
    audit_json                     mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_cds_emergency PRIMARY KEY (exchange_id, cds_unique_identifier)
);
-- index to make it easier to find last checksum for a CDS emergency record
CREATE INDEX ix_cds_emergency_checksum_helper on cds_emergency (cds_unique_identifier, dt_received);

/*
create table cds_emergency_latest
(
    exchange_id           char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received           datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum       bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    cds_activity_date     datetime    NOT NULL COMMENT 'Date common to all sus files',
    cds_unique_identifier varchar(50) NOT NULL COMMENT 'from CDSUniqueIdentifier',
    cds_update_type       int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                   varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number            varchar(10) NOT NULL COMMENT 'from NHSNumber',
    withheld              bool COMMENT 'True if id is withheld',
    date_of_birth         date COMMENT 'from PersonBirthDate',

    patient_pathway_identifier          varchar(20)  COMMENT 'links to the EpisodeId from the tail file if present',

    department_type                     varchar(20),
    ambulance_incident_number           varchar(50),
    ambulance_trust_organisation_code   varchar(12),
    attendance_identifier               varchar(20),
    arrival_mode                        varchar(12),
    attendance_category                 varchar(12),
    attendance_source                   varchar(12),
    Arrival_date                        datetime,
    initial_assessment_date             datetime,
    chief_complaint                     varchar(12),
    seen_for_treatment_date             datetime,
    decided_to_admit_date               datetime,
    treatment_function_code             varchar(12),
    discharge_status                    varchar(12) COMMENT 'Snomed coded',
    discharge_destination               varchar(12),
    conclusion_date                     datetime,
    departure_date                      datetime,
    mh_classifications                  mediumtext   COMMENT 'code~start datetime~end datetime in upto 10 | delimetered groups',
    diagnosis                           mediumtext   COMMENT 'code in upto 20 | delimetered groups',
    investigations                      mediumtext   COMMENT 'code~datetime in upto 10 | delimetered groups',
    treatments                          mediumtext   COMMENT 'code~datetime in upto 10 | delimetered groups',
    referred_to_services                mediumtext   COMMENT 'Snomed code~request date~assessment date  in upto 10 | delimetered groups',
    safeguarding_concerns               mediumtext   COMMENT 'Snomed code in upto 10 | delimetered groups',

    lookup_person_id               int COMMENT 'person ID looked up using NHS number, DoB and MRN',
    lookup_consultant_personnel_id int COMMENT 'personnel ID looked up using consultant code',
    audit_json                     mediumtext   null comment 'Used for Audit Purposes',
    CONSTRAINT pk_cds_emergency_latest PRIMARY KEY (cds_unique_identifier)
);
CREATE INDEX ix_cds_emergency_latest_join_helper on cds_emergency_latest (exchange_id, cds_unique_identifier);
*/

-- records from sus inpatient, outpatient and emergency tail files are all written to this table with sus_record_type
-- telling us which is which and there there is an encounter_id for every entry
create table cds_tail
(
    exchange_id                  char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                  datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum              bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type              varchar(10) NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier        varchar(50) NOT NULL,
    cds_update_type              int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                          varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                   varchar(10) NOT NULL COMMENT 'from NHSNumber',
    person_id                    int         NOT NULL,
    encounter_id                 int         NOT NULL COMMENT 'encounterId always present',
    episode_id                   int         COMMENT 'episodeId not always present',
    responsible_hcp_personnel_id int         NOT NULL COMMENT 'from Responsible_HCP_Personal_ID',
    treatment_function_code      varchar(12) COMMENT 'the treatment function code of the responsible hcp',
    audit_json                   mediumtext  NULL COMMENT 'Used for Audit Purposes',
    CONSTRAINT pk_cds_tail PRIMARY KEY (exchange_id, cds_unique_identifier, sus_record_type)
);
CREATE INDEX ix_cds_tail_checksum_helper on cds_tail (cds_unique_identifier, sus_record_type, dt_received);


create table cds_tail_latest
(
    exchange_id                  char(36)    NOT NULL COMMENT 'links to audit.exchange table (but on a different server)',
    dt_received                  datetime    NOT NULL COMMENT 'date time this record was received into Discovery',
    record_checksum              bigint      NOT NULL COMMENT 'checksum of the columns below to easily spot duplicates',
    sus_record_type              varchar(10) NOT NULL COMMENT 'one of inpatient, outpatient, emergency',
    cds_unique_identifier        varchar(50) NOT NULL,
    cds_update_type              int         NOT NULL COMMENT 'from CDSUpdateType',
    mrn                          varchar(10) NOT NULL COMMENT 'patient MRN from LocalPatientID field',
    nhs_number                   varchar(10) NOT NULL COMMENT 'from NHSNumber',
    person_id                    int         NOT NULL,
    encounter_id                 int         NOT NULL COMMENT 'encounterId always present',
    episode_id                   int         COMMENT 'episodeId not always present',
    responsible_hcp_personnel_id int         NOT NULL COMMENT 'from Responsible_HCP_Personal_ID',
    treatment_function_code      varchar(10) COMMENT 'the treatment function code of the responsible hcp',
    audit_json                   mediumtext  NULL COMMENT 'Used for Audit Purposes',
    CONSTRAINT pk_cds_tail_latest PRIMARY KEY (cds_unique_identifier, sus_record_type)
);