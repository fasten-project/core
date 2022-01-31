/*
 * This file is generated by jOOQ.
 */
package eu.fasten.core.data.metadatadb.codegen.tables.records;


import eu.fasten.core.data.metadatadb.codegen.tables.Vulnerabilities;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "https://www.jooq.org",
        "jOOQ version:3.16.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class VulnerabilitiesRecord extends UpdatableRecordImpl<VulnerabilitiesRecord> implements Record3<Long, String, JSONB> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>public.vulnerabilities.id</code>.
     */
    public void setId(Long value) {
        set(0, value);
    }

    /**
     * Getter for <code>public.vulnerabilities.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>public.vulnerabilities.external_id</code>.
     */
    public void setExternalId(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>public.vulnerabilities.external_id</code>.
     */
    public String getExternalId() {
        return (String) get(1);
    }

    /**
     * Setter for <code>public.vulnerabilities.statement</code>.
     */
    public void setStatement(JSONB value) {
        set(2, value);
    }

    /**
     * Getter for <code>public.vulnerabilities.statement</code>.
     */
    public JSONB getStatement() {
        return (JSONB) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row3<Long, String, JSONB> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    @Override
    public Row3<Long, String, JSONB> valuesRow() {
        return (Row3) super.valuesRow();
    }

    @Override
    public Field<Long> field1() {
        return Vulnerabilities.VULNERABILITIES.ID;
    }

    @Override
    public Field<String> field2() {
        return Vulnerabilities.VULNERABILITIES.EXTERNAL_ID;
    }

    @Override
    public Field<JSONB> field3() {
        return Vulnerabilities.VULNERABILITIES.STATEMENT;
    }

    @Override
    public Long component1() {
        return getId();
    }

    @Override
    public String component2() {
        return getExternalId();
    }

    @Override
    public JSONB component3() {
        return getStatement();
    }

    @Override
    public Long value1() {
        return getId();
    }

    @Override
    public String value2() {
        return getExternalId();
    }

    @Override
    public JSONB value3() {
        return getStatement();
    }

    @Override
    public VulnerabilitiesRecord value1(Long value) {
        setId(value);
        return this;
    }

    @Override
    public VulnerabilitiesRecord value2(String value) {
        setExternalId(value);
        return this;
    }

    @Override
    public VulnerabilitiesRecord value3(JSONB value) {
        setStatement(value);
        return this;
    }

    @Override
    public VulnerabilitiesRecord values(Long value1, String value2, JSONB value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached VulnerabilitiesRecord
     */
    public VulnerabilitiesRecord() {
        super(Vulnerabilities.VULNERABILITIES);
    }

    /**
     * Create a detached, initialised VulnerabilitiesRecord
     */
    public VulnerabilitiesRecord(Long id, String externalId, JSONB statement) {
        super(Vulnerabilities.VULNERABILITIES);

        setId(id);
        setExternalId(externalId);
        setStatement(statement);
    }
}
