package org.endeavourhealth.core.mySQLDatabase.models;

import org.endeavourhealth.core.mySQLDatabase.PersistenceManager;
import org.endeavourhealth.coreui.json.JsonDataSharingSummary;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Entity
@NamedStoredProcedureQueries({
        @NamedStoredProcedureQuery(
                name = "getDataSharingSummaryStatistics",
                procedureName = "getDataSharingSummaryStatistics"
        ),
        @NamedStoredProcedureQuery(
                name = "getDataProcessingAgreementStatistics",
                procedureName = "getDataProcessingAgreementStatistics"
        ),
        @NamedStoredProcedureQuery(
                name = "getDataSharingAgreementStatistics",
                procedureName = "getDataSharingAgreementStatistics"
        ),
        @NamedStoredProcedureQuery(
                name = "getDataFlowStatistics",
                procedureName = "getDataFlowStatistics"
        ),
        @NamedStoredProcedureQuery(
                name = "getCohortStatistics",
                procedureName = "getCohortStatistics"
        )
})
@Table(name = "datasharingsummary", schema = "organisationmanager")
public class DatasharingsummaryEntity {
    private String uuid;
    private String name;
    private String description;
    private String purpose;
    private short natureOfInformationId;
    private String schedule2Condition;
    private String benefitToSharing;
    private String overviewOfDataItems;
    private short formatTypeId;
    private short dataSubjectTypeId;
    private String natureOfPersonsAccessingData;
    private short reviewCycleId;
    private Date reviewDate;
    private Date startDate;
    private String evidenceOfAgreement;

    @Id
    @Column(name = "Uuid", nullable = false, length = 36)
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Basic
    @Column(name = "Name", nullable = false, length = 100)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Basic
    @Column(name = "Description", nullable = true, length = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Basic
    @Column(name = "Purpose", nullable = true, length = 100)
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @Basic
    @Column(name = "NatureOfInformationId", nullable = false)
    public short getNatureOfInformationId() {
        return natureOfInformationId;
    }

    public void setNatureOfInformationId(short natureOfInformationId) {
        this.natureOfInformationId = natureOfInformationId;
    }

    @Basic
    @Column(name = "Schedule2Condition", nullable = true, length = 100)
    public String getSchedule2Condition() {
        return schedule2Condition;
    }

    public void setSchedule2Condition(String schedule2Condition) {
        this.schedule2Condition = schedule2Condition;
    }

    @Basic
    @Column(name = "BenefitToSharing", nullable = true, length = 200)
    public String getBenefitToSharing() {
        return benefitToSharing;
    }

    public void setBenefitToSharing(String benefitToSharing) {
        this.benefitToSharing = benefitToSharing;
    }

    @Basic
    @Column(name = "OverviewOfDataItems", nullable = true, length = 100)
    public String getOverviewOfDataItems() {
        return overviewOfDataItems;
    }

    public void setOverviewOfDataItems(String overviewOfDataItems) {
        this.overviewOfDataItems = overviewOfDataItems;
    }

    @Basic
    @Column(name = "FormatTypeId", nullable = false)
    public short getFormatTypeId() {
        return formatTypeId;
    }

    public void setFormatTypeId(short formatTypeId) {
        this.formatTypeId = formatTypeId;
    }

    @Basic
    @Column(name = "DataSubjectTypeId", nullable = false)
    public short getDataSubjectTypeId() {
        return dataSubjectTypeId;
    }

    public void setDataSubjectTypeId(short dataSubjectTypeId) {
        this.dataSubjectTypeId = dataSubjectTypeId;
    }

    @Basic
    @Column(name = "NatureOfPersonsAccessingData", nullable = true, length = 100)
    public String getNatureOfPersonsAccessingData() {
        return natureOfPersonsAccessingData;
    }

    public void setNatureOfPersonsAccessingData(String natureOfPersonsAccessingData) {
        this.natureOfPersonsAccessingData = natureOfPersonsAccessingData;
    }

    @Basic
    @Column(name = "ReviewCycleId", nullable = false)
    public short getReviewCycleId() {
        return reviewCycleId;
    }

    public void setReviewCycleId(short reviewCycleId) {
        this.reviewCycleId = reviewCycleId;
    }

    @Basic
    @Column(name = "ReviewDate", nullable = true)
    public Date getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(Date reviewDate) {
        this.reviewDate = reviewDate;
    }

    @Basic
    @Column(name = "StartDate", nullable = true)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Basic
    @Column(name = "EvidenceOfAgreement", nullable = true, length = 200)
    public String getEvidenceOfAgreement() {
        return evidenceOfAgreement;
    }

    public void setEvidenceOfAgreement(String evidenceOfAgreement) {
        this.evidenceOfAgreement = evidenceOfAgreement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DatasharingsummaryEntity that = (DatasharingsummaryEntity) o;

        if (natureOfInformationId != that.natureOfInformationId) return false;
        if (formatTypeId != that.formatTypeId) return false;
        if (dataSubjectTypeId != that.dataSubjectTypeId) return false;
        if (reviewCycleId != that.reviewCycleId) return false;
        if (uuid != null ? !uuid.equals(that.uuid) : that.uuid != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (purpose != null ? !purpose.equals(that.purpose) : that.purpose != null) return false;
        if (schedule2Condition != null ? !schedule2Condition.equals(that.schedule2Condition) : that.schedule2Condition != null)
            return false;
        if (benefitToSharing != null ? !benefitToSharing.equals(that.benefitToSharing) : that.benefitToSharing != null)
            return false;
        if (overviewOfDataItems != null ? !overviewOfDataItems.equals(that.overviewOfDataItems) : that.overviewOfDataItems != null)
            return false;
        if (natureOfPersonsAccessingData != null ? !natureOfPersonsAccessingData.equals(that.natureOfPersonsAccessingData) : that.natureOfPersonsAccessingData != null)
            return false;
        if (reviewDate != null ? !reviewDate.equals(that.reviewDate) : that.reviewDate != null) return false;
        if (startDate != null ? !startDate.equals(that.startDate) : that.startDate != null) return false;
        if (evidenceOfAgreement != null ? !evidenceOfAgreement.equals(that.evidenceOfAgreement) : that.evidenceOfAgreement != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = uuid != null ? uuid.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (purpose != null ? purpose.hashCode() : 0);
        result = 31 * result + (int) natureOfInformationId;
        result = 31 * result + (schedule2Condition != null ? schedule2Condition.hashCode() : 0);
        result = 31 * result + (benefitToSharing != null ? benefitToSharing.hashCode() : 0);
        result = 31 * result + (overviewOfDataItems != null ? overviewOfDataItems.hashCode() : 0);
        result = 31 * result + (int) formatTypeId;
        result = 31 * result + (int) dataSubjectTypeId;
        result = 31 * result + (natureOfPersonsAccessingData != null ? natureOfPersonsAccessingData.hashCode() : 0);
        result = 31 * result + (int) reviewCycleId;
        result = 31 * result + (reviewDate != null ? reviewDate.hashCode() : 0);
        result = 31 * result + (startDate != null ? startDate.hashCode() : 0);
        result = 31 * result + (evidenceOfAgreement != null ? evidenceOfAgreement.hashCode() : 0);
        return result;
    }

    public static List<Object[]> getStatistics(String procName) throws Exception {

        EntityManager entityManager = PersistenceManager.getEntityManager();

        StoredProcedureQuery spq = entityManager.createNamedStoredProcedureQuery(procName);
        spq.execute();
        List<Object[]> ent = spq.getResultList();
        entityManager.close();

        return ent;
    }

    public static List<DatasharingsummaryEntity> getAllDataSharingSummaries() throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DatasharingsummaryEntity> cq = cb.createQuery(DatasharingsummaryEntity.class);
        Root<DatasharingsummaryEntity> rootEntry = cq.from(DatasharingsummaryEntity.class);
        CriteriaQuery<DatasharingsummaryEntity> all = cq.select(rootEntry);
        TypedQuery<DatasharingsummaryEntity> allQuery = entityManager.createQuery(all);
        List<DatasharingsummaryEntity> ret = allQuery.getResultList();

        entityManager.close();

        return ret;
    }

    public static DatasharingsummaryEntity getDataSharingSummary(String uuid) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        DatasharingsummaryEntity ret = entityManager.find(DatasharingsummaryEntity.class, uuid);

        entityManager.close();

        return ret;
    }

    public static void updateDataSharingSummary(JsonDataSharingSummary dataSharingSummary) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        DatasharingsummaryEntity datasharingsummaryEntity = entityManager.find(DatasharingsummaryEntity.class, dataSharingSummary.getUuid());
        entityManager.getTransaction().begin();
        datasharingsummaryEntity.setName(dataSharingSummary.getName());
        datasharingsummaryEntity.setDescription(dataSharingSummary.getDescription());
        datasharingsummaryEntity.setPurpose(dataSharingSummary.getPurpose());
        datasharingsummaryEntity.setNatureOfInformationId(dataSharingSummary.getNatureOfInformationId());
        datasharingsummaryEntity.setSchedule2Condition(dataSharingSummary.getSchedule2Condition());
        datasharingsummaryEntity.setBenefitToSharing(dataSharingSummary.getBenefitToSharing());
        datasharingsummaryEntity.setOverviewOfDataItems(dataSharingSummary.getOverviewOfDataItems());
        datasharingsummaryEntity.setFormatTypeId(dataSharingSummary.getFormatTypeId());
        datasharingsummaryEntity.setDataSubjectTypeId(dataSharingSummary.getDataSubjectTypeId());
        datasharingsummaryEntity.setNatureOfPersonsAccessingData(dataSharingSummary.getNatureOfPersonsAccessingData());
        datasharingsummaryEntity.setReviewCycleId(dataSharingSummary.getReviewCycleId());
        datasharingsummaryEntity.setReviewDate(dataSharingSummary.getReviewDate());
        datasharingsummaryEntity.setStartDate(dataSharingSummary.getStartDate());
        datasharingsummaryEntity.setEvidenceOfAgreement(dataSharingSummary.getEvidenceOfAgreement());
        entityManager.getTransaction().commit();

        entityManager.close();
    }

    public static void saveDataSharingSummary(JsonDataSharingSummary dataSharingSummary) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        DatasharingsummaryEntity datasharingsummaryEntity = new DatasharingsummaryEntity();
        entityManager.getTransaction().begin();
        datasharingsummaryEntity.setName(dataSharingSummary.getName());
        datasharingsummaryEntity.setDescription(dataSharingSummary.getDescription());
        datasharingsummaryEntity.setPurpose(dataSharingSummary.getPurpose());
        datasharingsummaryEntity.setNatureOfInformationId(dataSharingSummary.getNatureOfInformationId());
        datasharingsummaryEntity.setSchedule2Condition(dataSharingSummary.getSchedule2Condition());
        datasharingsummaryEntity.setBenefitToSharing(dataSharingSummary.getBenefitToSharing());
        datasharingsummaryEntity.setOverviewOfDataItems(dataSharingSummary.getOverviewOfDataItems());
        datasharingsummaryEntity.setFormatTypeId(dataSharingSummary.getFormatTypeId());
        datasharingsummaryEntity.setDataSubjectTypeId(dataSharingSummary.getDataSubjectTypeId());
        datasharingsummaryEntity.setNatureOfPersonsAccessingData(dataSharingSummary.getNatureOfPersonsAccessingData());
        datasharingsummaryEntity.setReviewCycleId(dataSharingSummary.getReviewCycleId());
        datasharingsummaryEntity.setReviewDate(dataSharingSummary.getReviewDate());
        datasharingsummaryEntity.setStartDate(dataSharingSummary.getStartDate());
        datasharingsummaryEntity.setEvidenceOfAgreement(dataSharingSummary.getEvidenceOfAgreement());
        datasharingsummaryEntity.setUuid(dataSharingSummary.getUuid());
        entityManager.persist(datasharingsummaryEntity);
        entityManager.getTransaction().commit();

        entityManager.close();
    }

    public static void deleteDataSharingSummary(String uuid) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        DatasharingsummaryEntity datasharingsummaryEntity = entityManager.find(DatasharingsummaryEntity.class, uuid);
        entityManager.getTransaction().begin();
        entityManager.remove(datasharingsummaryEntity);
        entityManager.getTransaction().commit();

        entityManager.close();
    }

    public static List<DatasharingsummaryEntity> search(String expression) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<DatasharingsummaryEntity> cq = cb.createQuery(DatasharingsummaryEntity.class);
        Root<DatasharingsummaryEntity> rootEntry = cq.from(DatasharingsummaryEntity.class);

        Predicate predicate = cb.or(cb.like(cb.upper(rootEntry.get("name")), "%" + expression.toUpperCase() + "%"),
                cb.like(cb.upper(rootEntry.get("description")), "%" + expression.toUpperCase() + "%"));

        cq.where(predicate);
        TypedQuery<DatasharingsummaryEntity> query = entityManager.createQuery(cq);
        List<DatasharingsummaryEntity> ret = query.getResultList();

        entityManager.close();

        return ret;
    }
}
