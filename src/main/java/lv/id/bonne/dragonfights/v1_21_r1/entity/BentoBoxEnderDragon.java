package lv.id.bonne.dragonfights.v1_21_r1.entity;

import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import java.util.logging.Logger;
import net.minecraft.core.BlockPos;
import lv.id.bonne.dragonfights.v1_21_r1.battle.BentoBoxDragonBattle;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public class BentoBoxEnderDragon extends EnderDragon
{
    private BentoBoxDragonBattle dragonBattle;
    private BlockPos islandCenter;
    private float speedMultiplier = 1.0f;
    private static final Logger LOG;
    private static final int[] VANILLA_ADJACENCY;
    
    public BentoBoxEnderDragon(final EntityType<? extends EnderDragon> entityType, final Level level) {
        super(EntityType.ENDER_DRAGON, level);
    }
    
    @Override
    public boolean shouldBeSaved() {
        return false;
    }
    
    public void setDragonBattle(final BentoBoxDragonBattle battle) {
        this.dragonBattle = battle;
    }
    
    public BentoBoxDragonBattle getDragonBattle() {
        return this.dragonBattle;
    }
    
    public BlockPos getIslandCenter() {
        return this.islandCenter;
    }
    
    public void initIslandCenter(final BlockPos center) {
        this.setFightOrigin(this.islandCenter = center);
        this.setPodium(center);
        this.fillIslandNodes(center);
        this.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }
    
    public void setSpeedMultiplier(final float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }
    
    public float getSpeedMultiplier() {
        return this.speedMultiplier;
    }
    
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean wasDying = this.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
        boolean result = super.hurtServer(level, source, amount);
        
        if (!wasDying && this.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
            this.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
            this.setHealth(0.0F);
        }
        
        return result;
    }
    
    @Override
    public void aiStep() {
        double prevX = this.getX();
        double prevY = this.getY();
        double prevZ = this.getZ();
        
        super.aiStep();
        
        if (this.dragonDeathTime > 0) {
            this.setPos(prevX, prevY, prevZ);
            this.setDeltaMovement(0, 0, 0);
        }
        else if (this.speedMultiplier != 1.0f) {
            double newX = prevX + (this.getX() - prevX) * this.speedMultiplier;
            double newY = prevY + (this.getY() - prevY) * this.speedMultiplier;
            double newZ = prevZ + (this.getZ() - prevZ) * this.speedMultiplier;
            this.setPos(newX, newY, newZ);
        }
    }
    
    private void fillIslandNodes(final BlockPos center) {
        try {
            final Field nodesField = findFieldByName(EnderDragon.class, "nodes");
            final Field adjField = findFieldByName(EnderDragon.class, "nodeAdjacency");
            if (nodesField == null || adjField == null) {
                BentoBoxEnderDragon.LOG.severe("Could not find nodes/nodeAdjacency fields on EnderDragon!");
                this.dumpFields();
                return;
            }
            nodesField.setAccessible(true);
            adjField.setAccessible(true);
            final Node[] nodes = (Node[])nodesField.get(this);
            final int[] adj = (int[])adjField.get(this);
            if (nodes == null || adj == null) {
                BentoBoxEnderDragon.LOG.severe("nodes or nodeAdjacency array is null! Cannot fill.");
                return;
            }
            final int cx = center.getX();
            final int cz = center.getZ();
            for (int i = 0; i < 12; ++i) {
                final double angle = 2.0 * (-3.141592653589793 + 0.2617994 * i);
                final int x = cx + Mth.floor(60.0f * Mth.cos(angle));
                final int z = cz + Mth.floor(60.0f * Mth.sin(angle));
                final int y = Math.max(73, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).getY() + 5);
                nodes[i] = new Node(x, y, z);
            }
            for (int i = 0; i < 8; ++i) {
                final double angle = 2.0 * (-3.141592653589793 + 0.3926991 * i);
                final int x = cx + Mth.floor(40.0f * Mth.cos(angle));
                final int z = cz + Mth.floor(40.0f * Mth.sin(angle));
                final int y = Math.max(73, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).getY() + 15);
                nodes[12 + i] = new Node(x, y, z);
            }
            for (int i = 0; i < 4; ++i) {
                final double angle = 2.0 * (-3.141592653589793 + 0.7853982 * i);
                final int x = cx + Mth.floor(20.0f * Mth.cos(angle));
                final int z = cz + Mth.floor(20.0f * Mth.sin(angle));
                final int y = Math.max(73, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).getY() + 15);
                nodes[20 + i] = new Node(x, y, z);
            }
            System.arraycopy(BentoBoxEnderDragon.VANILLA_ADJACENCY, 0, adj, 0, BentoBoxEnderDragon.VANILLA_ADJACENCY.length);
        }
        catch (final Exception e) {
            BentoBoxEnderDragon.LOG.severe("Failed to fill island pathfinding nodes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static Field findFieldByName(final Class<?> clazz, final String name) {
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (final Field f : c.getDeclaredFields()) {
                if (name.equals(f.getName())) {
                    return f;
                }
            }
        }
        return null;
    }
    
    private void dumpFields() {
        BentoBoxEnderDragon.LOG.info("EnderDragon declared fields:");
        final Field[] declaredFields = EnderDragon.class.getDeclaredFields();
        for (int length = declaredFields.length, i = 0; i < length; ++i) {
            final Field f = declaredFields[i];
            BentoBoxEnderDragon.LOG.info("  " + f.getName() + " : " + f.getType().getSimpleName() + " final=" + Modifier.isFinal(f.getModifiers()));
        }
    }
    
    static {
        LOG = Logger.getLogger("DragonFights");
        VANILLA_ADJACENCY = new int[] { 6146, 8197, 8202, 16404, 32808, 32848, 65696, 131392, 131712, 263424, 526848, 525313, 1581057, 3166214, 2138120, 6373424, 4358208, 12910976, 9044480, 9706496, 15216640, 13688832, 11763712, 8257536 };
    }
}
