package org.kbeng.data;

/**
 * Marker annotation used to flag domain types that belong to the engine data model.
 * The codebase uses this annotation to make core data carriers easy to discover during maintenance
 * (enums, entities, value objects, and registry-backed definitions) without coupling those classes
 * to infrastructure concerns.
 */
public @interface DataClass {}
