package lv.id.bonne.dragonfights.entity;


import lv.id.bonne.dragonfights.api.NMSHandler;
import lv.id.bonne.dragonfights.v1_21_r1.NMSHandlerImpl;


/**
 * Provides access to the NMS handler for the current server version (1.21 only).
 */
public class CustomEntityAPI
{
	/**
	 * Get the {@link NMSHandler} instance.
	 *
	 * @return the {@link NMSHandler} singleton
	 */
	public static NMSHandler getAPI()
	{
		if (api == null)
		{
			api = new NMSHandlerImpl();
		}

		return api;
	}


	private static NMSHandler api;
}
