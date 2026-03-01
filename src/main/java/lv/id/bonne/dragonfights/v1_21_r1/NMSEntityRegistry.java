package lv.id.bonne.dragonfights.v1_21_r1;

import org.bukkit.Bukkit;
import java.lang.reflect.Field;
import sun.misc.Unsafe;
import java.util.IdentityHashMap;
import java.util.Map;
import com.mojang.datafixers.types.Type;
import net.minecraft.util.datafix.fixes.References;
import com.mojang.datafixers.DataFixUtils;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import lv.id.bonne.dragonfights.entity.EntityTypeDefinition;
import java.util.logging.Logger;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import net.minecraft.world.entity.EntityType;
import lv.id.bonne.dragonfights.api.CustomRegistry;

public final class NMSEntityRegistry implements CustomRegistry
{
    private static final String ENTITY_KEY = "bentobox_ender_dragon";
    private static boolean registered;
    private static EntityType<BentoBoxEnderDragon> registeredEntityType;
    private static final Logger LOG;
    
    public static EntityType<BentoBoxEnderDragon> getRegisteredEntityType() {
        return NMSEntityRegistry.registeredEntityType;
    }
    
    @Override
    public void register(final EntityTypeDefinition type) {
        if (!"bentobox_ender_dragon".equals(type.getKey())) {
            return;
        }
        if (NMSEntityRegistry.registered) {
            return;
        }
        final Identifier key = Identifier.parse("bentobox_ender_dragon");
        try {
            final Registry<EntityType<?>> builtInReg = (Registry<EntityType<?>>)BuiltInRegistries.ENTITY_TYPE;
            NMSEntityRegistry.LOG.info("[DragonFights] Entity registry class: " + builtInReg.getClass().getName());
            if (!(builtInReg instanceof MappedRegistry)) {
                throw new IllegalStateException("BuiltInRegistries.ENTITY_TYPE is not MappedRegistry: " + builtInReg.getClass().getName());
            }
            final MappedRegistry<EntityType<?>> mapped = (MappedRegistry<EntityType<?>>)builtInReg;
            this.registerDataFixer("minecraft:bentobox_ender_dragon", "minecraft:" + type.getBaseKey());
            this.unfreezeRegistry(mapped);
            final ResourceKey<EntityType<?>> resourceKey = (ResourceKey<EntityType<?>>)ResourceKey.create(Registries.ENTITY_TYPE, key);
            final EntityType<BentoBoxEnderDragon> entityType = (EntityType<BentoBoxEnderDragon>)EntityType.Builder.of(BentoBoxEnderDragon::new, MobCategory.MONSTER).sized(16.0f, 8.0f).clientTrackingRange(64).updateInterval(3).noSummon().build((ResourceKey)resourceKey);
            mapped.register(resourceKey, entityType, RegistrationInfo.BUILT_IN);
            this.refreezeRegistry(mapped);
            NMSEntityRegistry.registeredEntityType = entityType;
            NMSEntityRegistry.registered = true;
            NMSEntityRegistry.LOG.info("[DragonFights] Successfully registered custom entity type: " + String.valueOf(key));
        }
        catch (final Exception e) {
            NMSEntityRegistry.LOG.severe("[DragonFights] Failed to register custom entity: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerDataFixer(final String customKey, final String baseKey) {
        try {
            final int dataVersion = SharedConstants.getCurrentVersion().dataVersion().version();
            @SuppressWarnings("unchecked")
            final Map<String, Type<?>> types = (Map<String, Type<?>>) (Map<?, ?>) DataFixers.getDataFixer().getSchema(DataFixUtils.makeKey(dataVersion)).findChoiceType(References.ENTITY_TREE).types();
            final Type<?> baseType = types.get(baseKey);
            if (baseType != null) {
                types.put(customKey, baseType);
                NMSEntityRegistry.LOG.info("[DragonFights] Registered data fixer for " + customKey);
            }
            else {
                NMSEntityRegistry.LOG.warning("[DragonFights] Base entity '" + baseKey + "' not found in data fixer schema");
            }
        }
        catch (final Exception e) {
            NMSEntityRegistry.LOG.warning("[DragonFights] Could not register data fixer for " + customKey + " (non-fatal): " + e.getMessage());
        }
    }
    
    private void unfreezeRegistry(final MappedRegistry<EntityType<?>> mapped) throws Exception {
        final Unsafe unsafe = this.getUnsafe();
        final Field cacheField = this.findMapField("unregisteredIntrusiveHolders", "m");
        final long cacheOffset = unsafe.objectFieldOffset(cacheField);
        if (unsafe.getObject(mapped, cacheOffset) == null) {
            unsafe.putObject(mapped, cacheOffset, new IdentityHashMap());
            NMSEntityRegistry.LOG.info("[DragonFights] Restored unregisteredIntrusiveHolders on entity registry");
        }
        final Field frozenField = this.findBooleanField("frozen", "l");
        final long frozenOffset = unsafe.objectFieldOffset(frozenField);
        unsafe.putBoolean(mapped, frozenOffset, false);
        NMSEntityRegistry.LOG.info("[DragonFights] Unfroze entity registry");
    }
    
    private void refreezeRegistry(final MappedRegistry<EntityType<?>> mapped) throws Exception {
        final Unsafe unsafe = this.getUnsafe();
        final Field frozenField = this.findBooleanField("frozen", "l");
        final long frozenOffset = unsafe.objectFieldOffset(frozenField);
        unsafe.putBoolean(mapped, frozenOffset, true);
        NMSEntityRegistry.LOG.info("[DragonFights] Re-froze entity registry");
    }
    
    private Field findBooleanField(final String... names) throws NoSuchFieldException {
        for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field f : c.getDeclaredFields()) {
                if (f.getType() == Boolean.TYPE) {
                    for (final String name : names) {
                        if (name.equals(f.getName())) {
                            return f;
                        }
                    }
                }
            }
        }
        for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == Boolean.TYPE) {
                    NMSEntityRegistry.LOG.warning("[DragonFights] Frozen field fallback: using '" + f.getName());
                    return f;
                }
            }
        }
        throw new NoSuchFieldException("MappedRegistry: could not find boolean field (frozen)");
    }
    
    private Field findMapField(final String... names) throws NoSuchFieldException {
        for (Class<?> c = MappedRegistry.class; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field f : c.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    for (final String name : names) {
                        if (name.equals(f.getName())) {
                            return f;
                        }
                    }
                }
            }
        }
        final StringBuilder sb = new StringBuilder("MappedRegistry fields: ");
        for (Class<?> c2 = MappedRegistry.class; c2 != null && c2 != Object.class; c2 = c2.getSuperclass()) {
            for (final Field f2 : c2.getDeclaredFields()) {
                sb.append(f2.getName()).append(':').append(f2.getType().getSimpleName()).append(' ');
            }
        }
        NMSEntityRegistry.LOG.warning("[DragonFights] " + String.valueOf(sb));
        throw new NoSuchFieldException("MappedRegistry: could not find Map field for unregisteredIntrusiveHolders");
    }
    
    private Unsafe getUnsafe() throws Exception {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe)unsafeField.get(null);
    }
    
    static {
        LOG = Bukkit.getLogger();
    }
}
