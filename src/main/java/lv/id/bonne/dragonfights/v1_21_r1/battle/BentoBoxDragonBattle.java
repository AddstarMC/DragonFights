package lv.id.bonne.dragonfights.v1_21_r1.battle;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import org.bukkit.entity.EnderCrystal;
import net.minecraft.world.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import lv.id.bonne.dragonfights.v1_21_r1.NMSEntityRegistry;
import java.util.Optional;
import java.util.Iterator;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.Objects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.ListTag;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import net.minecraft.core.Direction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.BossEvent;
import java.util.logging.Logger;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import java.util.List;
import java.util.UUID;
import lv.id.bonne.dragonfights.v1_21_r1.entity.BentoBoxEnderDragon;
import java.util.Random;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import org.bukkit.util.BoundingBox;
import net.minecraft.server.level.ServerLevel;
import lv.id.bonne.custombattle.CustomDragonBattle;

public class BentoBoxDragonBattle implements CustomDragonBattle
{
    private final ServerLevel world;
    private final String battleId;
    private final double range;
    private final BoundingBox boundingBox;
    private final ServerBossEvent bossBattle;
    private final BlockPos originalLocation;
    private BlockPos centerBeamLocation;
    @Nullable
    private BlockPos exitPortalLocation;
    private final boolean searchExitPortal;
    private boolean dragonKilled;
    private boolean previouslyKilled;
    private final String bossBarText;
    private final int numberOfTowers;
    private final int numberOfProtectedTowers;
    private final int distanceTillTowers;
    private final int numberPathPoints;
    private final int minTowerHeight;
    private final int maxTowerHeight;
    private final long battleSeed;
    private final Random random;
    @Nullable
    private BattleStage battleStage;
    @Nullable
    private BentoBoxEnderDragon enderDragon;
    @Nullable
    private UUID dragonUUID;
    @Nullable
    private BlockPos lastLocation;
    @Nullable
    private List<EndCrystal> respawnCrystals;
    private int tickCounter;
    private int currentTowerIndex;
    private int nextTowerSpawnTick;
    @Nullable
    private List<int[]> towerSpawnData;
    private final List<EndCrystal> towerCrystals;
    private final double dragonMaxHealth;
    private final float dragonSpeedMultiplier;
    @Nullable
    private final BarColor dragonGlowColor;
    private static final Logger LOG;
    
    public BentoBoxDragonBattle(final ServerLevel world, final BlockPos portalLocation, final BoundingBox boundingBox, final double range, final boolean dragonKilled, final boolean previouslyKilled, final String bossBarText, final BossEvent.BossBarColor bossBarColor, final BossEvent.BossBarOverlay bossBarStyle, final boolean playMusic, final boolean enableFog, final int distanceTillTowers, final int numberOfTowers, final int numberOfProtectedTowers, final int numberPathPoints, final int minTowerHeight, final int maxTowerHeight, final long battleSeed, final String battleId, final boolean searchExitPortal, final double dragonMaxHealth, final float dragonSpeedMultiplier, @Nullable final BarColor dragonGlowColor) {
        this.tickCounter = 0;
        this.currentTowerIndex = 0;
        this.nextTowerSpawnTick = 1;
        this.towerCrystals = new ArrayList<EndCrystal>();
        this.world = world;
        this.battleId = battleId;
        this.range = range;
        this.boundingBox = boundingBox;
        this.dragonKilled = dragonKilled;
        this.previouslyKilled = previouslyKilled;
        this.bossBarText = bossBarText;
        this.searchExitPortal = searchExitPortal;
        this.exitPortalLocation = (searchExitPortal ? null : portalLocation);
        this.originalLocation = portalLocation;
        this.centerBeamLocation = this.originalLocation.relative(Direction.UP, 40);
        this.numberOfTowers = numberOfTowers;
        this.numberOfProtectedTowers = numberOfProtectedTowers;
        this.distanceTillTowers = distanceTillTowers;
        this.numberPathPoints = numberPathPoints;
        this.minTowerHeight = minTowerHeight;
        this.maxTowerHeight = maxTowerHeight;
        this.battleSeed = battleSeed;
        this.random = new Random(battleSeed);
        this.dragonMaxHealth = dragonMaxHealth;
        this.dragonSpeedMultiplier = dragonSpeedMultiplier;
        this.dragonGlowColor = dragonGlowColor;
        this.battleStage = BattleStage.START;
        (this.bossBattle = new ServerBossEvent(CraftChatMessage.fromStringOrNull(bossBarText, true), bossBarColor, bossBarStyle)).setPlayBossMusic(playMusic);
        this.bossBattle.setCreateWorldFog(enableFog);
    }
    
    public BentoBoxDragonBattle(final ServerLevel world, final CompoundTag storedData) {
        this.tickCounter = 0;
        this.currentTowerIndex = 0;
        this.nextTowerSpawnTick = 1;
        this.towerCrystals = new ArrayList<EndCrystal>();
        this.world = world;
        this.range = storedData.getDoubleOr("Range", 196.0);
        this.originalLocation = readBlockPos(storedData, "OriginalLocation");
        final BlockPos minCorner = readBlockPos(storedData, "BoundingBoxMin");
        final BlockPos maxCorner = readBlockPos(storedData, "BoundingBoxMax");
        this.boundingBox = new BoundingBox((double)minCorner.getX(), (double)minCorner.getY(), (double)minCorner.getZ(), (double)maxCorner.getX(), (double)maxCorner.getY(), (double)maxCorner.getZ());
        this.exitPortalLocation = (storedData.contains("PortalLocation") ? readBlockPos(storedData, "PortalLocation") : null);
        this.bossBarText = storedData.getStringOr("BarTitle", "entity.minecraft.ender_dragon");
        final BossEvent.BossBarColor barColor = BossEvent.BossBarColor.valueOf(storedData.getStringOr("BarColor", "pink").toUpperCase(Locale.ROOT));
        final BossEvent.BossBarOverlay barStyle = BossEvent.BossBarOverlay.valueOf(storedData.getStringOr("BarStyle", "progress").toUpperCase(Locale.ROOT));
        (this.bossBattle = new ServerBossEvent(CraftChatMessage.fromStringOrNull(this.bossBarText, true), barColor, barStyle)).setPlayBossMusic(storedData.getBooleanOr("BarMusic", true));
        this.searchExitPortal = (this.exitPortalLocation == null);
        this.dragonKilled = storedData.getBooleanOr("DragonKilled", false);
        this.previouslyKilled = storedData.getBooleanOr("PreviouslyKilled", false);
        if (storedData.contains("DragonId")) {
            this.dragonUUID = UUID.fromString(storedData.getStringOr("DragonId", ""));
            this.lastLocation = (storedData.contains("LastLocation") ? readBlockPos(storedData, "LastLocation") : null);
        }
        this.battleId = storedData.getStringOr("BattleId", "");
        final String stageStr = storedData.getStringOr("BattleStage", "null");
        this.battleStage = ((stageStr == null || "null".equals(stageStr)) ? null : BattleStage.valueOf(stageStr));
        this.centerBeamLocation = ((this.exitPortalLocation != null) ? this.exitPortalLocation.relative(Direction.UP, 40) : this.originalLocation.relative(Direction.UP, 40));
        this.numberOfTowers = storedData.getIntOr("TowerCount", 10);
        this.numberOfProtectedTowers = storedData.getIntOr("ProtectedTowerCount", 2);
        this.distanceTillTowers = storedData.getIntOr("TowerDistance", 43);
        this.numberPathPoints = storedData.getIntOr("PathCount", 8);
        this.minTowerHeight = storedData.getIntOr("MinHeight", 73);
        this.maxTowerHeight = storedData.getIntOr("MaxHeight", 107);
        this.battleSeed = storedData.getLongOr("BattleSeed", 0L);
        this.random = new Random(this.battleSeed);
        this.currentTowerIndex = storedData.getIntOr("CurrentTowerIndex", 0);
        this.dragonMaxHealth = storedData.getDoubleOr("DragonMaxHealth", 200.0);
        this.dragonSpeedMultiplier = (float) storedData.getDoubleOr("DragonSpeedMultiplier", 1.0);
        final String glowColorStr = storedData.getStringOr("DragonGlowColor", "");
        this.dragonGlowColor = glowColorStr.isEmpty() ? null : BarColor.valueOf(glowColorStr);
        BentoBoxDragonBattle.LOG.info("Restoring battle from NBT: id='" + this.battleId +
            "' stage=" + this.battleStage +
            " dragonUUID=" + this.dragonUUID +
            " lastLoc=" + this.lastLocation +
            " exitPortal=" + this.exitPortalLocation +
            " originalLoc=" + this.originalLocation +
            " dragonKilled=" + this.dragonKilled +
            " previouslyKilled=" + this.previouslyKilled +
            " range=" + this.range +
            " world=" + (this.world != null ? this.world.dimension() : "null"));
        if (this.battleStage == BattleStage.SPAWNING_TOWERS && this.exitPortalLocation != null) {
            this.precomputeTowers();
        }
        if (storedData.contains("RespawnCrystals")) {
            final ListTag list = storedData.getListOrEmpty("RespawnCrystals");
            this.respawnCrystals = new ArrayList<EndCrystal>(list.size());
            for (int i = 0; i < list.size(); ++i) {
                final CompoundTag tag = list.getCompoundOrEmpty(i);
                final String id = tag.getStringOr("CrystalID", "");
                if (!id.isEmpty()) {
                    try {
                        final UUID uuid = UUID.fromString(id);
                        final Entity e = world.getEntity(uuid);
                        if (e instanceof final EndCrystal ec) {
                            this.respawnCrystals.add(ec);
                        }
                    }
                    catch (final IllegalArgumentException ex) {}
                }
            }
        }
    }
    
    private static BlockPos readBlockPos(final CompoundTag tag, final String key) {
        final CompoundTag c = tag.getCompoundOrEmpty(key);
        if (c.isEmpty()) {
            return BlockPos.ZERO;
        }
        return new BlockPos(c.getIntOr("X", 0), c.getIntOr("Y", 0), c.getIntOr("Z", 0));
    }
    
    private static void writeBlockPos(final CompoundTag tag, final String key, final BlockPos pos) {
        final CompoundTag c = new CompoundTag();
        c.putInt("X", pos.getX());
        c.putInt("Y", pos.getY());
        c.putInt("Z", pos.getZ());
        tag.put(key, (Tag)c);
    }
    
    @Override
    public void tickBattle() {
        if (this.battleStage == null) {
            return;
        }
        ++this.tickCounter;
        switch (this.battleStage.ordinal()) {
            case 0: {
                BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' stage=START tick=" + this.tickCounter);
                if (this.exitPortalLocation == null) {
                    this.exitPortalLocation = this.originalLocation;
                    BentoBoxDragonBattle.LOG.info("exitPortalLocation defaulted to originalLocation: " + this.exitPortalLocation.getX() + "," + this.exitPortalLocation.getY() + "," + this.exitPortalLocation.getZ());
                }
                BentoBoxDragonBattle.LOG.info("Generating exit portal at " + this.exitPortalLocation.getX() + "," + this.exitPortalLocation.getY() + "," + this.exitPortalLocation.getZ());
                this.generateExitPortal();
                this.centerBeamLocation = this.exitPortalLocation.relative(Direction.UP, 40);
                this.precomputeTowers();
                this.currentTowerIndex = 0;
                this.towerCrystals.clear();
                this.battleStage = BattleStage.SPAWNING_TOWERS;
                this.tickCounter = 0;
                BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' -> SPAWNING_TOWERS (" + this.numberOfTowers + " towers)");
                break;
            }
            case 1: {
                if (this.towerSpawnData == null) {
                    this.precomputeTowers();
                }
                if (this.currentTowerIndex >= this.numberOfTowers || this.tickCounter < this.nextTowerSpawnTick) {
                    break;
                }
                this.spawnSingleTower(this.currentTowerIndex);
                ++this.currentTowerIndex;
                if (this.currentTowerIndex >= this.numberOfTowers) {
                    this.battleStage = BattleStage.PREPARING_TO_SUMMON;
                    this.tickCounter = 0;
                    BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' all towers placed -> PREPARING_TO_SUMMON");
                    break;
                }
                this.nextTowerSpawnTick = this.tickCounter + 30 + this.world.random.nextInt(11);
                break;
            }
            case 2: {
                if (this.tickCounter == 1) {
                    BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' PREPARING_TO_SUMMON, waiting 20 ticks...");
                }
                if (this.tickCounter >= 20) {
                    this.battleStage = BattleStage.SUMMONING;
                    this.tickCounter = 0;
                    BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' -> SUMMONING");
                    break;
                }
                break;
            }
            case 3: {
                BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' SUMMONING at " + this.centerBeamLocation.getX() + "," + this.centerBeamLocation.getY() + "," + this.centerBeamLocation.getZ());
                this.clearCrystalBeamTargets();
                this.findOrCreateDragon();
                if (this.enderDragon != null) {
                    this.dragonKilled = false;
                    this.battleStage = BattleStage.BATTLE;
                    this.tickCounter = 0;
                    this.world.playSound((Entity)null, this.exitPortalLocation.getX() + 0.5, (double)this.exitPortalLocation.getY(), this.exitPortalLocation.getZ() + 0.5, SoundEvents.ENDER_DRAGON_AMBIENT, SoundSource.HOSTILE, 5.0f, 1.0f);
                    BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' dragon spawned UUID=" + String.valueOf(this.dragonUUID) + " -> BATTLE");
                    break;
                }
                BentoBoxDragonBattle.LOG.warning("Battle '" + this.battleId + "' failed to spawn dragon (attempt " + this.tickCounter);
                if (this.tickCounter >= 100) {
                    BentoBoxDragonBattle.LOG.severe("Battle '" + this.battleId + "' giving up dragon spawn after " + this.tickCounter + " attempts");
                    this.battleStage = BattleStage.END;
                    break;
                }
                break;
            }
            case 4: {
                if (this.enderDragon != null && !this.enderDragon.isAlive()) {
                    if (this.enderDragon.isRemoved()) {
                        if (this.enderDragon.getRemovalReason() == Entity.RemovalReason.KILLED) {
                            BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' dragon died! -> END");
                            this.dragonKilled = true;
                            if (this.bossBattle != null) {
                                this.bossBattle.removeAllPlayers();
                                this.bossBattle.setVisible(false);
                            }
                            this.battleStage = BattleStage.END;
                            this.tickCounter = 0;
                            break;
                        }
                        BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' dragon entity stale (removal=" + this.enderDragon.getRemovalReason() + "), clearing reference to re-find");
                        this.enderDragon = null;
                    } else {
                        if (this.bossBattle != null) {
                            this.bossBattle.setProgress(0);
                        }
                        break;
                    }
                }
                if (this.enderDragon == null) {
                    this.findOrCreateDragon();
                    if (this.enderDragon == null && this.tickCounter % 200 == 0) {
                        BentoBoxDragonBattle.LOG.warning("Battle '" + this.battleId + "' no dragon found (BATTLE tick=" + this.tickCounter);
                    }
                }
                if (this.enderDragon != null && this.bossBattle != null) {
                    this.bossBattle.setProgress(this.enderDragon.getHealth() / this.enderDragon.getMaxHealth());

                    double rangeSq = this.range * this.range;
                    Set<ServerPlayer> inRange = new HashSet<>();
                    for (ServerPlayer p : this.world.players()) {
                        if (p.distanceToSqr((double)this.centerBeamLocation.getX(), (double)this.centerBeamLocation.getY(), (double)this.centerBeamLocation.getZ()) < rangeSq) {
                            inRange.add(p);
                        }
                    }

                    inRange.forEach(this.bossBattle::addPlayer);

                    for (ServerPlayer tracked : new ArrayList<>(this.bossBattle.getPlayers())) {
                        if (!inRange.contains(tracked)) {
                            this.bossBattle.removePlayer(tracked);
                        }
                    }
                    break;
                }
                break;
            }
            case 5: {
                BentoBoxDragonBattle.LOG.info("Battle '" + this.battleId + "' END - cleaning up boss bar");
                if (this.bossBattle != null) {
                    this.bossBattle.removeAllPlayers();
                    this.bossBattle.setVisible(false);
                }
                this.cleanupGlowTeam();
                break;
            }
        }
    }
    
    private void findOrCreateDragon() {
        final List<? extends EnderDragon> allDragons = this.world.getDragons();
        BentoBoxDragonBattle.LOG.info("findOrCreateDragon: total dragons in world: " + allDragons.size());

        // 1) Try to find an existing BentoBoxEnderDragon already linked to this battle.
        final Optional<BentoBoxEnderDragon> byBattleRef = allDragons.stream()
            .filter(e -> e instanceof BentoBoxEnderDragon && Objects.equals(((BentoBoxEnderDragon)e).getDragonBattle(), this))
            .map(e -> (BentoBoxEnderDragon) e)
            .findFirst();
        if (byBattleRef.isPresent()) {
            this.enderDragon = byBattleRef.get();
            this.dragonUUID = this.enderDragon.getUUID();
            this.enderDragon.setDragonBattle(this);
            BentoBoxDragonBattle.LOG.info("Found existing dragon by battle ref UUID=" + String.valueOf(this.dragonUUID));
            this.applyDragonCharacteristics(false);
            this.ensureBossBarVisible();
            return;
        }

        // 2) Try to find a BentoBoxEnderDragon by UUID (e.g. after chunk reload cleared the battle ref).
        if (this.dragonUUID != null) {
            final Optional<BentoBoxEnderDragon> byUUID = allDragons.stream()
                .filter(e -> e instanceof BentoBoxEnderDragon && this.dragonUUID.equals(e.getUUID()))
                .map(e -> (BentoBoxEnderDragon) e)
                .findFirst();
            if (byUUID.isPresent()) {
                this.enderDragon = byUUID.get();
                this.enderDragon.setDragonBattle(this);
                this.enderDragon.initIslandCenter((this.exitPortalLocation != null) ? this.exitPortalLocation : this.originalLocation);
                BentoBoxDragonBattle.LOG.info("Re-linked existing dragon by UUID=" + String.valueOf(this.dragonUUID));
                this.applyDragonCharacteristics(false);
                this.ensureBossBarVisible();
                return;
            }
        }

        // 3) Remove any stray vanilla ender dragons inside this island's bounds.
        //    These are artifacts from chunk serialization (BentoBoxEnderDragon saves as
        //    minecraft:ender_dragon and reloads as a vanilla dragon heading to 0,0).
        allDragons.stream()
            .filter(e -> !(e instanceof BentoBoxEnderDragon) && e.isAlive())
            .filter(e -> this.boundingBox.contains(e.getX(), e.getY(), e.getZ()))
            .forEach(e -> {
                BentoBoxDragonBattle.LOG.info("Removing stray vanilla dragon UUID=" + e.getUUID() + " at " + e.blockPosition());
                e.discard();
            });

        // 4) Create a fresh custom dragon.
        BentoBoxDragonBattle.LOG.info("No matching dragon found, creating new one...");
        this.createNewDragon();
        if (this.enderDragon != null) {
            this.ensureBossBarVisible();
        }
    }
    
    private void applyDragonCharacteristics(boolean setHealth) {
        if (this.enderDragon == null) {
            return;
        }

        if (setHealth && this.dragonMaxHealth > 0) {
            var attr = this.enderDragon.getAttribute(Attributes.MAX_HEALTH);

            if (attr != null) {
                attr.setBaseValue(this.dragonMaxHealth);
                this.enderDragon.setHealth((float) this.dragonMaxHealth);
            }
        }

        this.enderDragon.setSpeedMultiplier(this.dragonSpeedMultiplier);
        this.applyDragonGlow();
    }
    
    private void applyDragonGlow() {
        if (this.enderDragon == null || this.dragonGlowColor == null) {
            return;
        }

        org.bukkit.entity.EnderDragon bukkitDragon =
            (org.bukkit.entity.EnderDragon) this.enderDragon.getBukkitEntity();
        bukkitDragon.setGlowing(true);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = this.getGlowTeamName();
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        team.setColor(barColorToChatColor(this.dragonGlowColor));
        team.addEntity(bukkitDragon);
    }
    
    private void cleanupGlowTeam() {
        if (this.dragonGlowColor == null) {
            return;
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(this.getGlowTeamName());

        if (team != null) {
            team.unregister();
        }
    }
    
    private String getGlowTeamName() {
        return "df" + Integer.toHexString(this.battleId.hashCode());
    }
    
    private static ChatColor barColorToChatColor(BarColor color) {
        return switch (color) {
            case PINK -> ChatColor.LIGHT_PURPLE;
            case BLUE -> ChatColor.BLUE;
            case RED -> ChatColor.RED;
            case GREEN -> ChatColor.GREEN;
            case YELLOW -> ChatColor.YELLOW;
            case PURPLE -> ChatColor.DARK_PURPLE;
            case WHITE -> ChatColor.WHITE;
        };
    }
    
    private void ensureBossBarVisible() {
        if (this.bossBattle != null && !this.bossBattle.isVisible()) {
            this.bossBattle.setVisible(true);
        }
    }
    
    @Override
    public void removeBossBarPlayer(org.bukkit.entity.Player player) {
        if (this.bossBattle != null && player instanceof CraftPlayer craftPlayer) {
            this.bossBattle.removePlayer(craftPlayer.getHandle());
        }
    }
    
    @Nullable
    private BentoBoxEnderDragon createNewDragon() {
        final EntityType<BentoBoxEnderDragon> type = NMSEntityRegistry.getRegisteredEntityType();
        BentoBoxDragonBattle.LOG.info("createNewDragon: registeredEntityType=" + String.valueOf((type != null) ? type : "NULL"));
        if (type == null) {
            BentoBoxDragonBattle.LOG.severe("Cannot create dragon: registered entity type is NULL! Entity was not registered.");
            return null;
        }
        final BentoBoxEnderDragon dragon = (BentoBoxEnderDragon)type.create((Level)this.world, EntitySpawnReason.COMMAND);
        if (dragon == null) {
            BentoBoxDragonBattle.LOG.severe("type.create() returned null! Dragon entity could not be instantiated.");
            return null;
        }
        dragon.setPos((double)this.centerBeamLocation.getX(), (double)this.centerBeamLocation.getY(), (double)this.centerBeamLocation.getZ());
        dragon.setDragonBattle(this);
        BentoBoxDragonBattle.LOG.info("Spawning dragon at " + this.centerBeamLocation.getX() + "," + this.centerBeamLocation.getY() + "," + this.centerBeamLocation.getZ() + " UUID=" + String.valueOf(dragon.getUUID()));
        final boolean added = this.world.addFreshEntity((Entity)dragon, CreatureSpawnEvent.SpawnReason.CUSTOM);
        if (added) {
            this.enderDragon = dragon;
            this.dragonUUID = dragon.getUUID();
            dragon.initIslandCenter((this.exitPortalLocation != null) ? this.exitPortalLocation : this.originalLocation);
            this.applyDragonCharacteristics(true);
            BentoBoxDragonBattle.LOG.info("Dragon island center initialized at portal location");
        }
        else {
            BentoBoxDragonBattle.LOG.severe("addFreshEntity FAILED - dragon was not added to world!");
        }
        return added ? dragon : null;
    }
    
    @Override
    public void onCrystalDamage(final EnderCrystal crystal) {
        BentoBoxDragonBattle.LOG.info("onCrystalDamage: stage=" + String.valueOf(this.battleStage) + " crystalAt=" + crystal.getLocation().getBlockX() + "," + crystal.getLocation().getBlockY() + "," + crystal.getLocation().getBlockZ());
        if (this.battleStage != null && this.boundingBox.contains(crystal.getLocation().toVector()) && this.respawnCrystals != null) {
            this.respawnCrystals.removeIf(ec -> ec.getUUID().equals(crystal.getUniqueId()));
            BentoBoxDragonBattle.LOG.info("Crystal removed from tracking, remaining=" + this.respawnCrystals.size());
        }
    }
    
    private void generateExitPortal() {
        final BlockPos center = this.exitPortalLocation;
        BentoBoxDragonBattle.LOG.info("generateExitPortal at " + center.getX() + "," + center.getY() + "," + center.getZ() + " world=" + String.valueOf(this.world.dimension()));
        for (int x = -3; x <= 3; ++x) {
            for (int z = -3; z <= 3; ++z) {
                final int distSq = x * x + z * z;
                if (distSq < 13) {
                    this.world.setBlock(center.offset(x, -2, z), Blocks.END_STONE.defaultBlockState(), 3);
                    final BlockPos surfacePos = center.offset(x, -1, z);
                    if (x == 0 && z == 0) {
                        this.world.setBlock(surfacePos, Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                    else if (distSq < 7) {
                        this.world.setBlock(surfacePos, Blocks.AIR.defaultBlockState(), 3);
                    }
                    else {
                        this.world.setBlock(surfacePos, Blocks.BEDROCK.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int y = 0; y <= 2; ++y) {
            this.world.setBlock(center.offset(0, y, 0), Blocks.BEDROCK.defaultBlockState(), 3);
        }
        this.world.setBlock(center.offset(1, 1, 0),
            Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 3);
        this.world.setBlock(center.offset(-1, 1, 0),
            Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 3);
        this.world.setBlock(center.offset(0, 1, 1),
            Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 3);
        this.world.setBlock(center.offset(0, 1, -1),
            Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 3);
        BentoBoxDragonBattle.LOG.info("Exit portal generated (unlit): bedrock frame + pillar at Y=" + (center.getY() - 1));
    }
    
    private void precomputeTowers() {
        this.towerSpawnData = new ArrayList<int[]>(this.numberOfTowers);
        final int centerX = this.exitPortalLocation.getX();
        final int centerZ = this.exitPortalLocation.getZ();
        final Random towerRandom = new Random(this.battleSeed);
        for (int i = 0; i < this.numberOfTowers; ++i) {
            final double angle = 6.283185307179586 * i / this.numberOfTowers;
            final int towerX = centerX + (int)(this.distanceTillTowers * Math.cos(angle));
            final int towerZ = centerZ + (int)(this.distanceTillTowers * Math.sin(angle));
            final int towerHeight = this.minTowerHeight + towerRandom.nextInt(Math.max(1, this.maxTowerHeight - this.minTowerHeight + 1));
            final boolean isProtected = i < this.numberOfProtectedTowers;
            this.towerSpawnData.add(new int[] { towerX, towerZ, towerHeight, isProtected ? 1 : 0 });
        }
    }
    
    private void spawnSingleTower(final int index) {
        final int[] data = this.towerSpawnData.get(index);
        final int towerX = data[0];
        final int towerZ = data[1];
        final int towerHeight = data[2];
        final boolean isProtected = data[3] == 1;
        final int baseY = this.exitPortalLocation.getY();
        final int radius = 2;
        BentoBoxDragonBattle.LOG.info("Tower " + index + ": pos=" + towerX + "," + towerZ + " height=" + towerHeight + " protected=" + isProtected);
        for (int y = baseY; y <= towerHeight; ++y) {
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (dx * dx + dz * dz <= radius * radius) {
                        this.world.setBlock(new BlockPos(towerX + dx, y, towerZ + dz), Blocks.OBSIDIAN.defaultBlockState(), 3);
                    }
                }
            }
        }
        this.world.setBlock(new BlockPos(towerX, towerHeight + 1, towerZ), Blocks.BEDROCK.defaultBlockState(), 3);
        if (isProtected) {
            for (int dy = -1; dy <= 3; ++dy) {
                for (int dx = -2; dx <= 2; ++dx) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        final boolean isEdge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        final boolean isCorner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                        if (isEdge && !isCorner) {
                            this.world.setBlock(new BlockPos(towerX + dx, towerHeight + 2 + dy, towerZ + dz), Blocks.IRON_BARS.defaultBlockState(), 3);
                        }
                    }
                }
            }
            for (int dx2 = -2; dx2 <= 2; ++dx2) {
                for (int dz2 = -2; dz2 <= 2; ++dz2) {
                    final boolean isCorner2 = Math.abs(dx2) == 2 && Math.abs(dz2) == 2;
                    if (!isCorner2) {
                        this.world.setBlock(new BlockPos(towerX + dx2, towerHeight + 5, towerZ + dz2), Blocks.IRON_BARS.defaultBlockState(), 3);
                    }
                }
            }
        }
        final EndCrystal crystal = new EndCrystal((Level)this.world, towerX + 0.5, (double)(towerHeight + 2), towerZ + 0.5);
        crystal.setShowBottom(false);
        crystal.setInvulnerable(false);
        crystal.setBeamTarget(this.exitPortalLocation);
        this.world.addFreshEntity((Entity)crystal);
        this.towerCrystals.add(crystal);
        this.world.playSound((Entity)null, this.exitPortalLocation.getX() + 0.5, (double)this.exitPortalLocation.getY(), this.exitPortalLocation.getZ() + 0.5, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 3.2f, 1.0f);
        BentoBoxDragonBattle.LOG.info("Tower " + index + " spawned with beam target and explosion sound");
    }
    
    private void clearCrystalBeamTargets() {
        final AABB searchBox = new AABB(this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMinZ(), this.boundingBox.getMaxX(), this.boundingBox.getMaxY(), this.boundingBox.getMaxZ());
        final List<EndCrystal> crystals = this.world.getEntitiesOfClass((Class)EndCrystal.class, searchBox);
        for (final EndCrystal crystal : crystals) {
            crystal.setBeamTarget((BlockPos)null);
        }
        this.towerCrystals.clear();
    }
    
    @Override
    public boolean onCrystalPlacement(final EnderCrystal crystal) {
        final boolean inBounds = this.boundingBox.contains(crystal.getLocation().toVector());
        BentoBoxDragonBattle.LOG.info("onCrystalPlacement: stage=" + String.valueOf(this.battleStage) + " crystalAt=" + crystal.getLocation().getBlockX() + "," + crystal.getLocation().getBlockY() + "," + crystal.getLocation().getBlockZ() + " inBounds=" + inBounds);
        if (!inBounds) {
            BentoBoxDragonBattle.LOG.info("Crystal outside bounding box " + String.valueOf(this.boundingBox) + ", ignoring");
            return false;
        }
        if (this.respawnCrystals == null) {
            this.respawnCrystals = new ArrayList<EndCrystal>();
        }
        final Entity nmsEntity = ((CraftEntity)crystal).getHandle();
        if (nmsEntity instanceof final EndCrystal ec) {
            this.respawnCrystals.add(ec);
            BentoBoxDragonBattle.LOG.info("Tracked crystal, total count=" + this.respawnCrystals.size());
        }
        if (this.battleStage == null || this.battleStage == BattleStage.START || this.battleStage == BattleStage.END) {
            this.battleStage = BattleStage.START;
            this.tickCounter = 0;
            BentoBoxDragonBattle.LOG.info("Crystal placement triggered battle -> START");
            return true;
        }
        BentoBoxDragonBattle.LOG.info("Crystal placed but battle already in stage=" + String.valueOf(this.battleStage) + ", no state change");
        return false;
    }
    
    @Nullable
    @Override
    public Vector getGeneratedPortalLocation() {
        return (this.exitPortalLocation != null) ? new Vector(this.exitPortalLocation.getX(), this.exitPortalLocation.getY(), this.exitPortalLocation.getZ()) : null;
    }
    
    @Override
    public boolean isGenerated() {
        return this.exitPortalLocation != null;
    }
    
    @Override
    public boolean isFinished() {
        return this.battleStage == BattleStage.END;
    }
    
    @Override
    public String saveData() {
        final CompoundTag compound = new CompoundTag();
        this.saveData(compound);
        String result = compound.toString();
        BentoBoxDragonBattle.LOG.info("saveData(): battle='" + this.battleId +
            "' stage=" + this.battleStage +
            "' dragonUUID=" + this.dragonUUID +
            " dragonAlive=" + (this.enderDragon != null && this.enderDragon.isAlive()) +
            " exitPortal=" + this.exitPortalLocation +
            " originalLoc=" + this.originalLocation +
            " resultLength=" + result.length());
        return result;
    }
    
    private void saveData(final CompoundTag nbt) {
        final CompoundTag battleData = new CompoundTag();
        battleData.putString("BattleId", this.battleId);
        battleData.putString("BattleStage", (this.battleStage != null) ? this.battleStage.name() : "null");
        battleData.putLong("BattleSeed", this.battleSeed);
        battleData.putString("BarTitle", this.bossBarText);
        battleData.putString("BarColor", this.bossBattle.getColor().name().toLowerCase(Locale.ROOT));
        battleData.putString("BarStyle", this.bossBattle.getOverlay().name().toLowerCase(Locale.ROOT));
        battleData.putBoolean("BarMusic", this.bossBattle.shouldPlayBossMusic());
        battleData.putInt("TowerCount", this.numberOfTowers);
        battleData.putInt("ProtectedTowerCount", this.numberOfProtectedTowers);
        battleData.putInt("PathCount", this.numberPathPoints);
        battleData.putInt("TowerDistance", this.distanceTillTowers);
        battleData.putInt("MinHeight", this.minTowerHeight);
        battleData.putInt("MaxHeight", this.maxTowerHeight);
        battleData.putInt("CurrentTowerIndex", this.currentTowerIndex);
        battleData.putDouble("DragonMaxHealth", this.dragonMaxHealth);
        battleData.putDouble("DragonSpeedMultiplier", this.dragonSpeedMultiplier);
        if (this.dragonGlowColor != null) {
            battleData.putString("DragonGlowColor", this.dragonGlowColor.name());
        }
        if (this.exitPortalLocation != null) {
            writeBlockPos(battleData, "PortalLocation", this.exitPortalLocation);
        }
        writeBlockPos(battleData, "OriginalLocation", this.originalLocation);
        battleData.putDouble("Range", this.range);
        battleData.putBoolean("PreviouslyKilled", this.previouslyKilled);
        battleData.putBoolean("DragonKilled", this.dragonKilled);
        if (this.dragonUUID != null) {
            battleData.putString("DragonId", this.dragonUUID.toString());
            if (this.enderDragon != null && this.enderDragon.isAlive()) {
                writeBlockPos(battleData, "LastLocation", this.enderDragon.blockPosition());
            } else if (this.exitPortalLocation != null) {
                writeBlockPos(battleData, "LastLocation", this.exitPortalLocation);
            } else {
                writeBlockPos(battleData, "LastLocation", this.originalLocation);
            }
        }
        writeBlockPos(battleData, "BoundingBoxMin", new BlockPos((int)Math.floor(this.boundingBox.getMinX()), (int)Math.floor(this.boundingBox.getMinY()), (int)Math.floor(this.boundingBox.getMinZ())));
        writeBlockPos(battleData, "BoundingBoxMax", new BlockPos((int)Math.floor(this.boundingBox.getMaxX()), (int)Math.floor(this.boundingBox.getMaxY()), (int)Math.floor(this.boundingBox.getMaxZ())));
        if (this.respawnCrystals != null) {
            final ListTag listTag = new ListTag();
            for (final EndCrystal crystal : this.respawnCrystals) {
                if (crystal.isAlive()) {
                    final CompoundTag tag = new CompoundTag();
                    tag.putString("CrystalID", crystal.getUUID().toString());
                    listTag.add(tag);
                }
            }
            battleData.put("RespawnCrystals", (Tag)listTag);
        }
        nbt.put("BattleData", (Tag)battleData);
    }
    
    @Nullable
    @Override
    public UUID getLastDragonUUID() {
        return this.dragonUUID;
    }
    
    @Nullable
    @Override
    public Vector getLastDragonLocation() {
        if (this.lastLocation != null) {
            return new Vector(this.lastLocation.getX(), this.lastLocation.getY(), this.lastLocation.getZ());
        }
        return (this.enderDragon != null) ? new Vector(this.enderDragon.getX(), this.enderDragon.getY(), this.enderDragon.getZ()) : null;
    }
    
    static {
        LOG = Logger.getLogger("DragonFights");
    }
    
    public enum BattleStage
    {
        START, 
        SPAWNING_TOWERS, 
        PREPARING_TO_SUMMON, 
        SUMMONING, 
        BATTLE, 
        END;
    }
}
