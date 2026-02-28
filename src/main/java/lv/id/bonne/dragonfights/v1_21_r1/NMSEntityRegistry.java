package lv.id.bonne.dragonfights.v1_21_r1;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.bukkit.Bukkit;

import lv.id.bonne.dragonfights.api.CustomRegistry;
import lv.id.bonne.dragonfights.entity.EntityTypeDefinition;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.types.Type;
import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registers the custom bentobox_ender_dragon entity type with the server (Paper 1.21.11).
 * <p>
 * The EntityType constructor calls {@code BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder()},
 * so we must unfreeze that specific registry AND restore its {@code unregisteredIntrusiveHolders}
 * map (set to null by {@code freeze()}) before building the EntityType.
 */
public final class NMSEntityRegistry implements CustomRegistry {

	private static final String ENTITY_KEY = "bentobox_ender_dragon";
	private static boolean registered;

	private static EntityType<BentoBoxEnderDragon> registeredEntityType;
	private static final Logger LOG = Bukkit.getLogger();

	public static EntityType<BentoBoxEnderDragon> getRegisteredEntityType() {
		return registeredEntityType;
	}

	@Override
	public void register(EntityTypeDefinition type) {
		if (!ENTITY_KEY.equals(type.getKey())) {
			return;
		}
		if (registered) {
			return;
		}
		Identifier key = Identifier.parse(ENTITY_KEY);
		try {
			/*
			 * EntityType constructor hardcodes: BuiltInRegistries.ENTITY_TYPE.createIntrusiveHolder(this)
			 * The previous approach unfroze the runtime registry from RegistryAccess, but that is a
			 * different object. We must target BuiltInRegistries.ENTITY_TYPE directly.
			 */
			Registry<EntityType<?>> builtInReg = BuiltInRegistries.ENTITY_TYPE;
			LOG.info("[DragonFights] Entity registry class: " + builtInReg.getClass().getName());

			if (!(builtInReg instanceof MappedRegistry<EntityType<?>> mapped)) {
				throw new IllegalStateException(
					"BuiltInRegistries.ENTITY_TYPE is not MappedRegistry: " + builtInReg.getClass().getName());
			}

			registerDataFixer("minecraft:" + ENTITY_KEY, "minecraft:" + type.getBaseKey());

			unfreezeRegistry(mapped);

			ResourceKey<EntityType<?>> resourceKey = ResourceKey.create(Registries.ENTITY_TYPE, key);
			EntityType<BentoBoxEnderDragon> entityType = EntityType.Builder
				.<BentoBoxEnderDragon>of(BentoBoxEnderDragon::new, MobCategory.MONSTER)
				.sized(16.0F, 8.0F)
				.clientTrackingRange(64)
				.updateInterval(3)
				.noSummon()
				.build(resourceKey);

			mapped.register(resourceKey, entityType, RegistrationInfo.BUILT_IN);

			refreezeRegistry(mapped);

			registeredEntityType = entityType;
			registered = true;
			LOG.info("[DragonFights] Successfully registered custom entity type: " + key);
		} catch (Exception e) {
			LOG.severe("[DragonFights] Failed to register custom entity: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Copies the data fixer type from the vanilla base entity to our custom key.
	 * Prevents the "No data fixer registered" error during EntityType.Builder.build().
	 */
	@SuppressWarnings("unchecked")
	private void registerDataFixer(String customKey, String baseKey) {
		try {
			int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
			Map<String, Type<?>> types = (Map<String, Type<?>>) DataFixers.getDataFixer()
				.getSchema(DataFixUtils.makeKey(dataVersion))
				.findChoiceType(References.ENTITY_TREE)
				.types();

			Type<?> baseType = types.get(baseKey);
			if (baseType != null) {
				types.put(customKey, baseType);
				LOG.info("[DragonFights] Registered data fixer for " + customKey);
			} else {
				LOG.warning("[DragonFights] Base entity '" + baseKey + "' not found in data fixer schema");
			}
		} catch (Exception e) {
			LOG.warning("[DragonFights] Could not register data fixer for " + customKey
				+ " (non-fatal): " + e.getMessage());
		}
	}

	/**
	 * Unfreezes the registry so createIntrusiveHolder() and register() can succeed.
	 * Two things must be restored:
	 * <ol>
	 *   <li>unregisteredIntrusiveHolders map (freeze() sets it to null)</li>
	 *   <li>frozen flag (set to false)</li>
	 * </ol>
	 */
	@SuppressWarnings("deprecation")
	private void unfreezeRegistry(MappedRegistry<EntityType<?>> mapped) throws Exception {
		sun.misc.Unsafe unsafe = getUnsafe();

		Field cacheField = findMapField("unregisteredIntrusiveHolders", "m");
		long cacheOffset = unsafe.objectFieldOffset(cacheField);
		if (unsafe.getObject(mapped, cacheOffset) == null) {
			unsafe.putObject(mapped, cacheOffset, new IdentityHashMap<>());
			LOG.info("[DragonFights] Restored unregisteredIntrusiveHolders on entity registry");
		}

		Field frozenField = findBooleanField("frozen", "l");
		long frozenOffset = unsafe.objectFieldOffset(frozenField);
		unsafe.putBoolean(mapped, frozenOffset, false);
		LOG.info("[DragonFights] Unfroze entity registry");
	}

	@SuppressWarnings("deprecation")
	private void refreezeRegistry(MappedRegistry<EntityType<?>> mapped) throws Exception {
		sun.misc.Unsafe unsafe = getUnsafe();
		Field frozenField = findBooleanField("frozen", "l");
		long frozenOffset = unsafe.objectFieldOffset(frozenField);
		unsafe.putBoolean(mapped, frozenOffset, true);
		LOG.info("[DragonFights] Re-froze entity registry");
	}

	/**
	 * Finds a boolean field by name, with fallback to the first boolean field found.
	 * Supports Mojang-mapped ("frozen") and obfuscated ("l") names.
	 */
	private Field findBooleanField(String... names) throws NoSuchFieldException {
		for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.getType() == boolean.class) {
					for (String name : names) {
						if (name.equals(f.getName())) {
							return f;
						}
					}
				}
			}
		}
		for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (f.getType() == boolean.class) {
					LOG.warning("[DragonFights] Frozen field fallback: using '" + f.getName() + "'");
					return f;
				}
			}
		}
		throw new NoSuchFieldException("MappedRegistry: could not find boolean field (frozen)");
	}

	/**
	 * Finds a Map field by name, with diagnostic dump on failure.
	 * Supports Mojang-mapped ("unregisteredIntrusiveHolders") and obfuscated ("m") names.
	 */
	private Field findMapField(String... names) throws NoSuchFieldException {
		for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (Map.class.isAssignableFrom(f.getType())) {
					for (String name : names) {
						if (name.equals(f.getName())) {
							return f;
						}
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder("MappedRegistry fields: ");
		for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				sb.append(f.getName()).append(':').append(f.getType().getSimpleName()).append(' ');
			}
		}
		LOG.warning("[DragonFights] " + sb);
		throw new NoSuchFieldException(
			"MappedRegistry: could not find Map field for unregisteredIntrusiveHolders");
	}

	private sun.misc.Unsafe getUnsafe() throws Exception {
		Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		return (sun.misc.Unsafe) unsafeField.get(null);
	}
}
