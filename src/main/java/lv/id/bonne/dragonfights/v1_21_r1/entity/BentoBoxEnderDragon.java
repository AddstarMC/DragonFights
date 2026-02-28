package lv.id.bonne.dragonfights.v1_21_r1.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;

import lv.id.bonne.dragonfights.v1_21_r1.battle.BentoBoxDragonBattle;

import java.lang.reflect.Field;
import java.util.logging.Logger;

/**
 * Custom Ender Dragon for BentoBox dragon fights (Paper 1.21.11).
 * Vanilla AI is used, but pathfinding nodes are repositioned to center
 * on the player's island portal instead of world origin.
 */
public class BentoBoxEnderDragon extends EnderDragon {

	private BentoBoxDragonBattle dragonBattle;
	private BlockPos islandCenter;
	private static final Logger LOG = Logger.getLogger("DragonFights");

	/**
	 * Vanilla hardcoded adjacency values from EnderDragon.findClosestNode() bytecode.
	 * These define which path nodes can reach each other.
	 */
	private static final int[] VANILLA_ADJACENCY = {
		6146, 8197, 8202, 16404, 32808, 32848, 65696, 131392,
		131712, 263424, 526848, 525313, 1581057, 3166214, 2138120,
		6373424, 4358208, 12910976, 9044480, 9706496, 15216640,
		13688832, 11763712, 8257536
	};

	public BentoBoxEnderDragon(EntityType<? extends EnderDragon> entityType, Level level) {
		super(EntityType.ENDER_DRAGON, level);
	}

	public void setDragonBattle(BentoBoxDragonBattle battle) {
		this.dragonBattle = battle;
	}

	public BentoBoxDragonBattle getDragonBattle() {
		return dragonBattle;
	}

	public BlockPos getIslandCenter() {
		return islandCenter;
	}

	/**
	 * Fills the vanilla pathfinding node arrays (which are final but mutable)
	 * with positions centered on the island portal instead of world origin.
	 * Must be called after entity creation but before first server tick.
	 */
	public void initIslandCenter(BlockPos center) {
		this.islandCenter = center;
		this.setFightOrigin(center);
		this.setPodium(center);
		fillIslandNodes(center);
		this.getPhaseManager().setPhase(
			net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
	}

	/**
	 * Accesses the existing final Node[] and int[] arrays via reflection
	 * and fills their CONTENTS with island-centered values.
	 * We cannot replace the array references (they're final), but we can
	 * modify the elements, which prevents vanilla from overwriting with 0,0 nodes.
	 */
	private void fillIslandNodes(BlockPos center) {
		try {
			Field nodesField = findFieldByName(EnderDragon.class, "nodes");
			Field adjField = findFieldByName(EnderDragon.class, "nodeAdjacency");

			if (nodesField == null || adjField == null) {
				LOG.severe("Could not find nodes/nodeAdjacency fields on EnderDragon!");
				dumpFields();
				return;
			}

			nodesField.setAccessible(true);
			adjField.setAccessible(true);

			Node[] nodes = (Node[]) nodesField.get(this);
			int[] adj = (int[]) adjField.get(this);

			if (nodes == null || adj == null) {
				LOG.severe("nodes or nodeAdjacency array is null! Cannot fill.");
				return;
			}

			LOG.info("Filling path nodes: array length=" + nodes.length +
				" adj length=" + adj.length + " center=" + center);

			int cx = center.getX();
			int cz = center.getZ();

			// Group 1 (indices 0-11): outer ring, radius 60, 12 nodes
			for (int i = 0; i < 12; i++) {
				double angle = 2.0 * (-Math.PI + 0.2617994 * i);
				int x = cx + Mth.floor(60.0f * Mth.cos(angle));
				int z = cz + Mth.floor(60.0f * Mth.sin(angle));
				int y = Math.max(73,
					level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						new BlockPos(x, 0, z)).getY() + 5);
				nodes[i] = new Node(x, y, z);
			}

			// Group 2 (indices 12-19): middle ring, radius 40, 8 nodes
			for (int i = 0; i < 8; i++) {
				double angle = 2.0 * (-Math.PI + 0.3926991 * i);
				int x = cx + Mth.floor(40.0f * Mth.cos(angle));
				int z = cz + Mth.floor(40.0f * Mth.sin(angle));
				int y = Math.max(73,
					level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						new BlockPos(x, 0, z)).getY() + 15);
				nodes[12 + i] = new Node(x, y, z);
			}

			// Group 3 (indices 20-23): inner ring, radius 20, 4 nodes
			for (int i = 0; i < 4; i++) {
				double angle = 2.0 * (-Math.PI + 0.7853982 * i);
				int x = cx + Mth.floor(20.0f * Mth.cos(angle));
				int z = cz + Mth.floor(20.0f * Mth.sin(angle));
				int y = Math.max(73,
					level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						new BlockPos(x, 0, z)).getY() + 15);
				nodes[20 + i] = new Node(x, y, z);
			}

			// Fill adjacency with vanilla hardcoded values
			System.arraycopy(VANILLA_ADJACENCY, 0, adj, 0, VANILLA_ADJACENCY.length);

			LOG.info("Path nodes filled successfully. Node[0]=" +
				nodes[0].x + "," + nodes[0].y + "," + nodes[0].z +
				" Node[12]=" + nodes[12].x + "," + nodes[12].y + "," + nodes[12].z);

		} catch (Exception e) {
			LOG.severe("Failed to fill island pathfinding nodes: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static Field findFieldByName(Class<?> clazz, String name) {
		for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (name.equals(f.getName())) {
					return f;
				}
			}
		}
		return null;
	}

	private void dumpFields() {
		LOG.info("EnderDragon declared fields:");
		for (Field f : EnderDragon.class.getDeclaredFields()) {
			LOG.info("  " + f.getName() + " : " + f.getType().getSimpleName() +
				" final=" + java.lang.reflect.Modifier.isFinal(f.getModifiers()));
		}
	}
}
