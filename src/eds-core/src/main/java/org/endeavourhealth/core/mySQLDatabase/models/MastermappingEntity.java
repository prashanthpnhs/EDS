package org.endeavourhealth.core.mySQLDatabase.models;

import org.endeavourhealth.core.mySQLDatabase.MapType;
import org.endeavourhealth.core.mySQLDatabase.PersistenceManager;
import org.endeavourhealth.coreui.json.*;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NamedStoredProcedureQueries({
    @NamedStoredProcedureQuery(
            name = "deleteAllMappings",
            procedureName = "deleteAllMappings",
            parameters = {
                    @StoredProcedureParameter(mode = ParameterMode.IN, type = String.class, name = "UUID")
            }
    )
})
@Entity
@Table(name = "mastermapping", schema = "organisationmanager", catalog = "")
@IdClass(MastermappingEntityPK.class)
public class MastermappingEntity {
    private String childUuid;
    private String parentUUid;
    private byte isDefault;
    private short childMapTypeId;
    private short parentMapTypeId;

    public static void deleteAllMappings(String uuid) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        StoredProcedureQuery spq = entityManager.createNamedStoredProcedureQuery("deleteAllMappings");
        spq.setParameter("UUID", uuid);
        spq.execute();
        entityManager.close();
    }

    public static void saveParentMappings(Map<UUID, String> parents, Short parentMapTypeId, String childUuid, Short childMapTypeId) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        parents.forEach((k, v) -> {
            MastermappingEntity mme = new MastermappingEntity();
            entityManager.getTransaction().begin();
            mme.setChildUuid(childUuid);
            mme.setChildMapTypeId(childMapTypeId);
            mme.setParentUUid(k.toString());
            mme.setParentMapTypeId(parentMapTypeId);
            entityManager.persist(mme);
            entityManager.getTransaction().commit();
        });

        entityManager.close();

    }

    public static void saveChildMappings(Map<UUID, String> children, Short childMapTypeId, String parentUuid, Short parentMapTypeId) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        children.forEach((k, v) -> {
            MastermappingEntity mme = new MastermappingEntity();
            entityManager.getTransaction().begin();
            mme.setChildUuid(k.toString());
            mme.setChildMapTypeId(childMapTypeId);
            mme.setParentUUid(parentUuid);
            mme.setParentMapTypeId(parentMapTypeId);
            entityManager.persist(mme);
            entityManager.getTransaction().commit();
        });

        entityManager.close();
    }

    public static List<String> getParentMappings(String childUuid, Short childMapTypeId, Short parentMapTypeId) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MastermappingEntity> cq = cb.createQuery(MastermappingEntity.class);
        Root<MastermappingEntity> rootEntry = cq.from(MastermappingEntity.class);

        Predicate predicate = cb.and(cb.equal(rootEntry.get("childUuid"), childUuid ),
                cb.equal(rootEntry.get("childMapTypeId"), childMapTypeId),
                cb.equal(rootEntry.get("parentMapTypeId"), parentMapTypeId));

        cq.where(predicate);
        TypedQuery<MastermappingEntity> query = entityManager.createQuery(cq);
        List<MastermappingEntity> maps =  query.getResultList();

        List<String> parents = new ArrayList<>();
        for(MastermappingEntity mme : maps){
            parents.add(mme.getParentUUid());
        }

        entityManager.close();

        return parents;
    }

    public static List<String> getChildMappings(String parentUuid, Short parentMapTypeId, Short childMapTypeId) throws Exception {
        EntityManager entityManager = PersistenceManager.getEntityManager();

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<MastermappingEntity> cq = cb.createQuery(MastermappingEntity.class);
        Root<MastermappingEntity> rootEntry = cq.from(MastermappingEntity.class);

        Predicate predicate = cb.and(cb.equal(rootEntry.get("parentUUid"), parentUuid ),
                cb.equal(rootEntry.get("parentMapTypeId"), parentMapTypeId),
                cb.equal(rootEntry.get("childMapTypeId"), childMapTypeId));

        cq.where(predicate);
        TypedQuery<MastermappingEntity> query = entityManager.createQuery(cq);
        List<MastermappingEntity> maps =  query.getResultList();

        List<String> children = new ArrayList<>();
        for(MastermappingEntity mme : maps){
            children.add(mme.getChildUuid());
        }

        entityManager.close();

        return children;
    }

    public static void saveOrganisationMappings(JsonOrganisationManager organisation) throws Exception {
        //Default to organisation
        Short organisationType = MapType.ORGANISATION.getMapType();

        if (organisation.getIsService().equals("0")) {
            if (organisation.getRegions() != null) {
                Map<UUID, String> regions = organisation.getRegions();
                saveParentMappings(regions, MapType.REGION.getMapType(), organisation.getUuid(), MapType.ORGANISATION.getMapType());
            }

            if (organisation.getChildOrganisations() != null) {
                Map<UUID, String> childOrganisations = organisation.getChildOrganisations();
                saveChildMappings(childOrganisations, MapType.ORGANISATION.getMapType(), organisation.getUuid(), MapType.ORGANISATION.getMapType());
            }

            if (organisation.getServices() != null) {
                Map<UUID, String> services = organisation.getServices();
                saveChildMappings(services, MapType.SERVICE.getMapType(), organisation.getUuid(), MapType.ORGANISATION.getMapType());
            }
        } else {
            organisationType = MapType.SERVICE.getMapType();
        }

        if (organisation.getParentOrganisations() != null) {
            Map<UUID, String> parentOrganisations = organisation.getParentOrganisations();
            saveParentMappings(parentOrganisations, MapType.ORGANISATION.getMapType(), organisation.getUuid(), organisationType);
        }
    }

    public static void saveRegionMappings(JsonRegion region) throws Exception {

        if (region.getParentRegions() != null) {
            Map<UUID, String> parentRegions = region.getParentRegions();
            saveParentMappings(parentRegions, MapType.REGION.getMapType(), region.getUuid(), MapType.REGION.getMapType());
        }

        if (region.getChildRegions() != null) {
            Map<UUID, String> childRegions = region.getChildRegions();
            saveChildMappings(childRegions, MapType.REGION.getMapType(), region.getUuid(), MapType.REGION.getMapType());
        }

        if (region.getOrganisations() != null) {
            Map<UUID, String> organisations = region.getOrganisations();
            saveChildMappings(organisations, MapType.ORGANISATION.getMapType(), region.getUuid(), MapType.REGION.getMapType());
        }
    }

    public static void saveDataSharingAgreementMappings(JsonDSA dsa) throws Exception {

        if (dsa.getDataFlows() != null) {
            Map<UUID, String> dataFlows = dsa.getDataFlows();
            saveChildMappings(dataFlows, MapType.DATAFLOW.getMapType(), dsa.getUuid(), MapType.DATASHARINGAGREEMENT.getMapType());
        }
    }

    public static void saveDataFlowMappings(JsonDataFlow dataFlow) throws Exception {

        if (dataFlow.getDsas() != null) {
            Map<UUID, String> dsas = dataFlow.getDsas();
            saveParentMappings(dsas, MapType.DATASHARINGAGREEMENT.getMapType(),  dataFlow.getUuid(), MapType.DATAFLOW.getMapType());
        }

        if (dataFlow.getDpas() != null) {
            Map<UUID, String> dpas = dataFlow.getDpas();
            saveChildMappings(dpas, MapType.DATAPROCESSINGAGREEMENT.getMapType(), dataFlow.getUuid(), MapType.DATAFLOW.getMapType());
        }
    }

    public static void saveDataProcessingAgreementMappings(JsonDPA dpa) throws Exception {

        if (dpa.getDataFlows() != null) {
            Map<UUID, String> dataFlows = dpa.getDataFlows();
            saveParentMappings(dataFlows, MapType.DATAFLOW.getMapType(), dpa.getUuid(), MapType.DATAPROCESSINGAGREEMENT.getMapType());
        }

        if (dpa.getCohorts() != null) {
            Map<UUID, String> cohorts = dpa.getCohorts();
            saveChildMappings(cohorts, MapType.COHORT.getMapType(), dpa.getUuid(), MapType.DATAPROCESSINGAGREEMENT.getMapType());
        }
    }

    public static void saveCohortMappings(JsonCohort cohort) throws Exception {

        if (cohort.getDpas() != null) {
            Map<UUID, String> dataFlows = cohort.getDpas();
            saveParentMappings(dataFlows, MapType.DATAFLOW.getMapType(), cohort.getUuid(), MapType.COHORT.getMapType());
        }
    }

    @Id
    @Column(name = "ChildUuid", nullable = false, length = 36)
    public String getChildUuid() {
        return childUuid;
    }

    public void setChildUuid(String childUuid) {
        this.childUuid = childUuid;
    }

    @Id
    @Column(name = "ParentUUid", nullable = false, length = 36)
    public String getParentUUid() {
        return parentUUid;
    }

    public void setParentUUid(String parentUUid) {
        this.parentUUid = parentUUid;
    }

    @Basic
    @Column(name = "IsDefault", nullable = false)
    public byte getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(byte isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MastermappingEntity that = (MastermappingEntity) o;

        if (childMapTypeId != that.childMapTypeId) return false;
        if (parentMapTypeId != that.parentMapTypeId) return false;
        if (isDefault != that.isDefault) return false;
        if (childUuid != null ? !childUuid.equals(that.childUuid) : that.childUuid != null) return false;
        if (parentUUid != null ? !parentUUid.equals(that.parentUUid) : that.parentUUid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = childUuid != null ? childUuid.hashCode() : 0;
        result = 31 * result + (parentUUid != null ? parentUUid.hashCode() : 0);
        result = 31 * result + (int) childMapTypeId;
        result = 31 * result + (int) parentMapTypeId;
        result = 31 * result + (int) isDefault;
        return result;
    }

    @Id
    @Column(name = "ChildMapTypeId", nullable = false)
    public short getChildMapTypeId() {
        return childMapTypeId;
    }

    public void setChildMapTypeId(short childMapTypeId) {
        this.childMapTypeId = childMapTypeId;
    }

    @Id
    @Column(name = "ParentMapTypeId", nullable = false)
    public short getParentMapTypeId() {
        return parentMapTypeId;
    }

    public void setParentMapTypeId(short parentMapTypeId) {
        this.parentMapTypeId = parentMapTypeId;
    }
}
