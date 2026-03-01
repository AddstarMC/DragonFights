package lv.id.bonne.dragonfights.config;


import org.bukkit.boss.BarColor;
import org.jetbrains.annotations.Nullable;


/**
 * Represents a set of dragon characteristics: glow colour, health, and speed multiplier.
 * Parsed from config strings in the format {@code COLOUR:HEALTH:SPEED}.
 */
public class DragonCharacteristic
{
	private final BarColor colour;

	private final double health;

	private final float speed;


	public DragonCharacteristic(BarColor colour, double health, float speed)
	{
		this.colour = colour;
		this.health = health;
		this.speed = speed;
	}


	/**
	 * Parses a characteristic from its serialized form {@code COLOUR:HEALTH:SPEED}.
	 *
	 * @param input the string to parse.
	 * @return the parsed characteristic, or {@code null} if the input is invalid.
	 */
	@Nullable
	public static DragonCharacteristic parse(String input)
	{
		if (input == null || input.isBlank())
		{
			return null;
		}

		String[] parts = input.split(":");

		if (parts.length != 3)
		{
			return null;
		}

		try
		{
			BarColor colour = BarColor.valueOf(parts[0].trim().toUpperCase());
			double health = Double.parseDouble(parts[1].trim());
			float speed = Float.parseFloat(parts[2].trim());

			if (health <= 0 || speed <= 0)
			{
				return null;
			}

			return new DragonCharacteristic(colour, Math.max(1, health), Math.max(0.1f, speed));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}


	/**
	 * Serializes this characteristic back to the {@code COLOUR:HEALTH:SPEED} format.
	 *
	 * @return the serialized string.
	 */
	public String serialize()
	{
		return this.colour.name() + ":" + this.health + ":" + this.speed;
	}


	/**
	 * Gets colour.
	 *
	 * @return the colour
	 */
	public BarColor getColour()
	{
		return this.colour;
	}


	/**
	 * Gets health.
	 *
	 * @return the health
	 */
	public double getHealth()
	{
		return this.health;
	}


	/**
	 * Gets speed.
	 *
	 * @return the speed multiplier
	 */
	public float getSpeed()
	{
		return this.speed;
	}
}
