/*
 * This file is generated by jOOQ.
 */
package eu.fasten.core.data.metadatadb.codegen.tables;


import eu.fasten.core.data.metadatadb.codegen.Indexes;
import eu.fasten.core.data.metadatadb.codegen.Keys;
import eu.fasten.core.data.metadatadb.codegen.Public;
import eu.fasten.core.data.metadatadb.codegen.tables.records.FilesRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row6;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "https://www.jooq.org",
        "jOOQ version:3.14.8"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Files extends TableImpl<FilesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>public.files</code>
     */
    public static final Files FILES = new Files();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FilesRecord> getRecordType() {
        return FilesRecord.class;
    }

    /**
     * The column <code>public.files.id</code>.
     */
    public final TableField<FilesRecord, Long> ID = createField(DSL.name("id"), SQLDataType.BIGINT.nullable(false).identity(true), this, "");

    /**
     * The column <code>public.files.package_version_id</code>.
     */
    public final TableField<FilesRecord, Long> PACKAGE_VERSION_ID = createField(DSL.name("package_version_id"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>public.files.path</code>.
     */
    public final TableField<FilesRecord, String> PATH = createField(DSL.name("path"), SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.files.checksum</code>.
     */
    public final TableField<FilesRecord, byte[]> CHECKSUM = createField(DSL.name("checksum"), SQLDataType.BLOB, this, "");

    /**
     * The column <code>public.files.created_at</code>.
     */
    public final TableField<FilesRecord, Timestamp> CREATED_AT = createField(DSL.name("created_at"), SQLDataType.TIMESTAMP(6), this, "");

    /**
     * The column <code>public.files.metadata</code>.
     */
    public final TableField<FilesRecord, JSONB> METADATA = createField(DSL.name("metadata"), SQLDataType.JSONB, this, "");

    private Files(Name alias, Table<FilesRecord> aliased) {
        this(alias, aliased, null);
    }

    private Files(Name alias, Table<FilesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
    }

    /**
     * Create an aliased <code>public.files</code> table reference
     */
    public Files(String alias) {
        this(DSL.name(alias), FILES);
    }

    /**
     * Create an aliased <code>public.files</code> table reference
     */
    public Files(Name alias) {
        this(alias, FILES);
    }

    /**
     * Create a <code>public.files</code> table reference
     */
    public Files() {
        this(DSL.name("files"), null);
    }

    public <O extends Record> Files(Table<O> child, ForeignKey<O, FilesRecord> key) {
        super(child, key, FILES);
    }

    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.FILES_PACKAGE_VERSION_ID);
    }

    @Override
    public Identity<FilesRecord, Long> getIdentity() {
        return (Identity<FilesRecord, Long>) super.getIdentity();
    }

    @Override
    public UniqueKey<FilesRecord> getPrimaryKey() {
        return Keys.FILES_PKEY;
    }

    @Override
    public List<UniqueKey<FilesRecord>> getKeys() {
        return Arrays.<UniqueKey<FilesRecord>>asList(Keys.FILES_PKEY, Keys.UNIQUE_VERSION_PATH);
    }

    @Override
    public List<ForeignKey<FilesRecord, ?>> getReferences() {
        return Arrays.<ForeignKey<FilesRecord, ?>>asList(Keys.FILES__FILES_PACKAGE_VERSION_ID_FKEY);
    }

    private transient PackageVersions _packageVersions;

    public PackageVersions packageVersions() {
        if (_packageVersions == null)
            _packageVersions = new PackageVersions(this, Keys.FILES__FILES_PACKAGE_VERSION_ID_FKEY);

        return _packageVersions;
    }

    @Override
    public Files as(String alias) {
        return new Files(DSL.name(alias), this);
    }

    @Override
    public Files as(Name alias) {
        return new Files(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Files rename(String name) {
        return new Files(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Files rename(Name name) {
        return new Files(name, null);
    }

    // -------------------------------------------------------------------------
    // Row6 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row6<Long, Long, String, byte[], Timestamp, JSONB> fieldsRow() {
        return (Row6) super.fieldsRow();
    }
}
