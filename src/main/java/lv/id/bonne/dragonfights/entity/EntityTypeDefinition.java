package lv.id.bonne.dragonfights.entity;


/**
 * Contract for entity type definitions that can be registered with the CustomRegistry.
 * The NMS registry uses these keys to register and data-fix the custom entity.
 */
public interface EntityTypeDefinition
{
	/**
	 * @return the unique key for this entity type (e.g. "bentobox_ender_dragon").
	 */
	String getKey();

	/**
	 * @return the base (vanilla) entity key used for data-fixer aliasing (e.g. "ender_dragon").
	 */
	String getBaseKey();
}
