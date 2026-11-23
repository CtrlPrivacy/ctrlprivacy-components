package org.ctrlprivacy.core.entity;

/**
 * The names of {@link BaseEntity}'s properties, for queries and updates that name them. Spring
 * Data maps a property name to its stored field ({@code key} to {@code _key}) whenever the query
 * or update is run with the entity class, so code uses the property names.
 */
public final class EntityFields {

    /** The application identity's property. */
    public static final String KEY = "key";

    /** The application identity's stored field, named to sit beside {@code _id}. */
    public static final String STORED_KEY = "_key";

    /** When the entity was first saved. */
    public static final String CREATED_AT = "createdAt";

    /** Who first saved it. */
    public static final String CREATED_BY = "createdBy";

    /** When it was last changed. */
    public static final String UPDATED_AT = "updatedAt";

    /** Who last changed it. */
    public static final String UPDATED_BY = "updatedBy";

    /** Not instantiable: a holder of constants. */
    private EntityFields() {}
}
