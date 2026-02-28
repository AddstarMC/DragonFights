package lv.id.bonne.dragonfights.v1_21_r1.battle;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lv.id.bonne.custombattle.CustomDragonBattle;
import lv.id.bonne.custombattle.DragonBattleBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class NMSDragonBattleBuilder extends DragonBattleBuilder {

	private BentoBoxDragonBattle dragonBattle;
	private ServerLevel world;
	private BlockPos portalLocation;
	private BossEvent.BossBarColor bossBarColor = BossEvent.BossBarColor.PINK;
	private BossEvent.BossBarOverlay bossBarStyle = BossEvent.BossBarOverlay.PROGRESS;

	public NMSDragonBattleBuilder(String battleId) {
		super(battleId);
	}

	@Nullable
	@Override
	public CustomDragonBattle build() {
		if (dragonBattle != null) {
			return dragonBattle;
		}
		if (world == null || battleId == null || portalLocation == null) {
			return null;
		}
		if (boundingBox == null) {
			boundingBox = new BoundingBox(
				portalLocation.getX() - range, 0, portalLocation.getZ() - range,
				portalLocation.getX() + range, 255, portalLocation.getZ() + range);
		}
		dragonBattle = new BentoBoxDragonBattle(world, portalLocation, boundingBox, range,
			dragonKilled, previouslyKilled, bossBarText, bossBarColor, bossBarStyle,
			playMusic, enableFog, distanceTillTowers, numberOfTowers, numberOfProtectedTowers,
			numberOfPathPoints, minTowerHeight, maxTowerHeight, battleSeed, battleId, searchExitPortal);
		return dragonBattle;
	}

	@Nullable
	@Override
	public CustomDragonBattle buildFromNBT(String nbt) {
		try {
			CompoundTag tag = TagParser.parseCompoundFully(nbt);
			if (tag.contains("BattleData")) {
				dragonBattle = new BentoBoxDragonBattle(world, tag.getCompoundOrEmpty("BattleData"));
			}
		} catch (CommandSyntaxException e) {
			dragonBattle = null;
		}
		return dragonBattle;
	}

	@Override
	public DragonBattleBuilder setWorld(World world) {
		this.world = ((CraftWorld) world).getHandle();
		return super.setWorld(world);
	}

	@Override
	public DragonBattleBuilder setPortalLocation(Vector portalLocation) {
		this.portalLocation = new BlockPos(portalLocation.getBlockX(), portalLocation.getBlockY(), portalLocation.getBlockZ());
		return super.setPortalLocation(portalLocation);
	}

	@Override
	public DragonBattleBuilder setBossBarColor(BarColor bossBarColor) {
		this.bossBarColor = convertColor(bossBarColor);
		return super.setBossBarColor(bossBarColor);
	}

	@Override
	public DragonBattleBuilder setBossBarStyle(BarStyle bossBarStyle) {
		this.bossBarStyle = convertStyle(bossBarStyle);
		return super.setBossBarStyle(bossBarStyle);
	}

	private static BossEvent.BossBarColor convertColor(BarColor color) {
		return switch (color != null ? color : BarColor.PINK) {
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

	private static BossEvent.BossBarOverlay convertStyle(BarStyle style) {
		return switch (style != null ? style : BarStyle.SOLID) {
			case SEGMENTED_6 -> BossEvent.BossBarOverlay.NOTCHED_6;
			case SEGMENTED_10 -> BossEvent.BossBarOverlay.NOTCHED_10;
			case SEGMENTED_12 -> BossEvent.BossBarOverlay.NOTCHED_12;
			case SEGMENTED_20 -> BossEvent.BossBarOverlay.NOTCHED_20;
			default -> BossEvent.BossBarOverlay.PROGRESS;
		};
	}
}
