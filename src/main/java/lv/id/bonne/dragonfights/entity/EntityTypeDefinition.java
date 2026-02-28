package lv.id.bonne.dragonfights.entity;

/**
 * Definition of a custom entity type for registry (1.21 only).
 */
public interface EntityTypeDefinition {

	String getKey();

	String getBaseKey();

	CreatureType getCreatureType();
}
