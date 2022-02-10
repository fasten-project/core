/*
 * This file is generated by jOOQ.
 */
package eu.fasten.core.data.metadatadb.codegen.routines;


import eu.fasten.core.data.metadatadb.codegen.Public;

import javax.annotation.processing.Generated;

import org.jooq.Field;
import org.jooq.Parameter;
import org.jooq.impl.AbstractRoutine;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;


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
public class EncryptIv extends AbstractRoutine<byte[]> {

    private static final long serialVersionUID = 1L;

    /**
     * The parameter <code>public.encrypt_iv.RETURN_VALUE</code>.
     */
    public static final Parameter<byte[]> RETURN_VALUE = Internal.createParameter("RETURN_VALUE", SQLDataType.BLOB, false, false);

    /**
     * The parameter <code>public.encrypt_iv._1</code>.
     */
    public static final Parameter<byte[]> _1 = Internal.createParameter("_1", SQLDataType.BLOB, false, true);

    /**
     * The parameter <code>public.encrypt_iv._2</code>.
     */
    public static final Parameter<byte[]> _2 = Internal.createParameter("_2", SQLDataType.BLOB, false, true);

    /**
     * The parameter <code>public.encrypt_iv._3</code>.
     */
    public static final Parameter<byte[]> _3 = Internal.createParameter("_3", SQLDataType.BLOB, false, true);

    /**
     * The parameter <code>public.encrypt_iv._4</code>.
     */
    public static final Parameter<String> _4 = Internal.createParameter("_4", SQLDataType.CLOB, false, true);

    /**
     * Create a new routine call instance
     */
    public EncryptIv() {
        super("encrypt_iv", Public.PUBLIC, SQLDataType.BLOB);

        setReturnParameter(RETURN_VALUE);
        addInParameter(_1);
        addInParameter(_2);
        addInParameter(_3);
        addInParameter(_4);
    }

    /**
     * Set the <code>_1</code> parameter IN value to the routine
     */
    public void set__1(byte[] value) {
        setValue(_1, value);
    }

    /**
     * Set the <code>_1</code> parameter to the function to be used with a
     * {@link org.jooq.Select} statement
     */
    public void set__1(Field<byte[]> field) {
        setField(_1, field);
    }

    /**
     * Set the <code>_2</code> parameter IN value to the routine
     */
    public void set__2(byte[] value) {
        setValue(_2, value);
    }

    /**
     * Set the <code>_2</code> parameter to the function to be used with a
     * {@link org.jooq.Select} statement
     */
    public void set__2(Field<byte[]> field) {
        setField(_2, field);
    }

    /**
     * Set the <code>_3</code> parameter IN value to the routine
     */
    public void set__3(byte[] value) {
        setValue(_3, value);
    }

    /**
     * Set the <code>_3</code> parameter to the function to be used with a
     * {@link org.jooq.Select} statement
     */
    public void set__3(Field<byte[]> field) {
        setField(_3, field);
    }

    /**
     * Set the <code>_4</code> parameter IN value to the routine
     */
    public void set__4(String value) {
        setValue(_4, value);
    }

    /**
     * Set the <code>_4</code> parameter to the function to be used with a
     * {@link org.jooq.Select} statement
     */
    public void set__4(Field<String> field) {
        setField(_4, field);
    }
}
