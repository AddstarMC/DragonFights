package lv.id.bonne.dragonfights.entity;


/**
 * Entity type definition for the custom BentoBox ender dragon (1.21 only).
 */
public class BentoBoxEnderDragonRoot implements EntityTypeDefinition
{
	@Override
	public String getKey()
	{
		return "bentobox_ender_dragon";
	}


	@Override
	public String getBaseKey()
	{
		return "ender_dragon";
	}
}
