package lv.id.bonne.dragonfights.v1_21_r1.battle;

import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BarColor;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.World;
import net.minecraft.nbt.CompoundTag;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.TagParser;
import org.jetbrains.annotations.Nullable;
import org.bukkit.util.BoundingBox;
import lv.id.bonne.custombattle.CustomDragonBattle;
import net.minecraft.world.BossEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import lv.id.bonne.custombattle.DragonBattleBuilder;

public class NMSDragonBattleBuilder extends DragonBattleBuilder
{
    private BentoBoxDragonBattle dragonBattle;
    private ServerLevel world;
    private BlockPos portalLocation;
    private BossEvent.BossBarColor bossBarColor;
    private BossEvent.BossBarOverlay bossBarStyle;
    
    public NMSDragonBattleBuilder(final String battleId) {
        super(battleId);
        this.bossBarColor = BossEvent.BossBarColor.PINK;
        this.bossBarStyle = BossEvent.BossBarOverlay.PROGRESS;
    }
    
    @Nullable
    @Override
    public CustomDragonBattle build() {
        if (this.dragonBattle != null) {
            return this.dragonBattle;
        }
        if (this.world == null || this.battleId == null || this.portalLocation == null) {
            return null;
        }
        if (this.boundingBox == null) {
            this.boundingBox = new BoundingBox(this.portalLocation.getX() - this.range, 0.0, this.portalLocation.getZ() - this.range, this.portalLocation.getX() + this.range, 255.0, this.portalLocation.getZ() + this.range);
        }
        return this.dragonBattle = new BentoBoxDragonBattle(this.world, this.portalLocation, this.boundingBox, this.range, this.dragonKilled, this.previouslyKilled, this.bossBarText, this.bossBarColor, this.bossBarStyle, this.playMusic, this.enableFog, this.distanceTillTowers, this.numberOfTowers, this.numberOfProtectedTowers, this.numberOfPathPoints, this.minTowerHeight, this.maxTowerHeight, this.battleSeed, this.battleId, this.searchExitPortal, this.dragonMaxHealth, this.dragonSpeedMultiplier, this.dragonGlowColor);
    }
    
    @Nullable
    @Override
    public CustomDragonBattle buildFromNBT(final String nbt) {
        try {
            final CompoundTag tag = TagParser.parseCompoundFully(nbt);
            if (tag.contains("BattleData")) {
                this.dragonBattle = new BentoBoxDragonBattle(this.world, tag.getCompoundOrEmpty("BattleData"));
            }
            else {
                org.bukkit.Bukkit.getLogger().warning("[DragonFights] buildFromNBT: no 'BattleData' key in NBT. tag=" + tag);
            }
        }
        catch (final CommandSyntaxException e) {
            org.bukkit.Bukkit.getLogger().severe("[DragonFights] buildFromNBT: failed to parse NBT: " + e.getMessage());
            this.dragonBattle = null;
        }
        catch (final Exception e) {
            org.bukkit.Bukkit.getLogger().severe("[DragonFights] buildFromNBT: unexpected error: " + e.getMessage());
            e.printStackTrace();
            this.dragonBattle = null;
        }
        return this.dragonBattle;
    }
    
    @Override
    public DragonBattleBuilder setWorld(final World world) {
        this.world = ((CraftWorld)world).getHandle();
        return super.setWorld(world);
    }
    
    @Override
    public DragonBattleBuilder setPortalLocation(final Vector portalLocation) {
        this.portalLocation = new BlockPos(portalLocation.getBlockX(), portalLocation.getBlockY(), portalLocation.getBlockZ());
        return super.setPortalLocation(portalLocation);
    }
    
    @Override
    public DragonBattleBuilder setBossBarColor(final BarColor bossBarColor) {
        this.bossBarColor = convertColor(bossBarColor);
        return super.setBossBarColor(bossBarColor);
    }
    
    @Override
    public DragonBattleBuilder setBossBarStyle(final BarStyle bossBarStyle) {
        this.bossBarStyle = convertStyle(bossBarStyle);
        return super.setBossBarStyle(bossBarStyle);
    }
    
    private static BossEvent.BossBarColor convertColor(final BarColor color) {
        return switch ((color != null) ? color : BarColor.PINK) {
            case BLUE -> BossEvent.BossBarColor.BLUE;
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case PINK -> BossEvent.BossBarColor.PINK;
            case PURPLE -> BossEvent.BossBarColor.PURPLE;
            case RED -> BossEvent.BossBarColor.RED;
            case WHITE -> BossEvent.BossBarColor.WHITE;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.PINK;
        };
    }
    
    private static BossEvent.BossBarOverlay convertStyle(final BarStyle style) {
        return switch ((style != null) ? style : BarStyle.SOLID) {
            case SEGMENTED_6 -> BossEvent.BossBarOverlay.NOTCHED_6;
            case SEGMENTED_10 -> BossEvent.BossBarOverlay.NOTCHED_10;
            case SEGMENTED_12 -> BossEvent.BossBarOverlay.NOTCHED_12;
            case SEGMENTED_20 -> BossEvent.BossBarOverlay.NOTCHED_20;
            default -> BossEvent.BossBarOverlay.PROGRESS;
        };
    }
}
