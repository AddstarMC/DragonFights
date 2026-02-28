package lv.id.bonne.dragonfights.api;

import lv.id.bonne.dragonfights.entity.EntityTypeDefinition;

/**
 * Registry for custom entity types (1.21 only).
 */
public interface CustomRegistry {

	void register(EntityTypeDefinition type);
}
