package su.nightexpress.ama.sign;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.api.manager.AbstractManager;
import su.nexmedia.engine.utils.LocationUtil;
import su.nexmedia.engine.utils.PDCUtil;
import su.nexmedia.engine.utils.StringUtil;
import su.nightexpress.ama.AMA;
import su.nightexpress.ama.Keys;
import su.nightexpress.ama.Placeholders;
import su.nightexpress.ama.api.type.GameState;
import su.nightexpress.ama.arena.impl.Arena;
import su.nightexpress.ama.arena.impl.ArenaPlayer;
import su.nightexpress.ama.config.Config;
import su.nightexpress.ama.kit.impl.Kit;
import su.nightexpress.ama.sign.listener.SignListener;
import su.nightexpress.ama.sign.type.SignType;
import su.nightexpress.ama.stats.object.StatType;
import su.nightexpress.ama.stats.object.StatsScore;

import java.util.*;
import java.util.stream.Stream;

public class SignManager extends AbstractManager<AMA> {

    private static final String HEADER = "AMA";

    private Map<SignType, Set<Sign>> signs;

    public SignManager(@NotNull AMA plugin) {
        super(plugin);
    }

    @Override
    protected void onLoad() {
        this.signs = new HashMap<>();

        // Using 0.5s delay to wait until all arenas, kits and server worlds (for locations) are loaded.
        this.plugin.getScheduler().runTaskLater(this.plugin, () -> {
            this.plugin.getConfig().getStringList("Signs.Locations").forEach(locationRaw -> {
                Location location = LocationUtil.deserialize(locationRaw);
                if (location == null || !(location.getBlock().getState() instanceof Sign sign)) return;

                SignType signType = this.getSignType(sign);
                if (signType == null) return;

                this.getSigns(signType).add(sign);
            });
            this.update();
        }, 10L);

        this.addListener(new SignListener(this));
    }

    @Override
    protected void onShutdown() {
        this.plugin.getConfig().reload();
        this.plugin.getConfig().set("Signs.Locations", this.signs.values().stream().flatMap(Collection::stream).map(BlockState::getLocation).map(LocationUtil::serialize).toList());
        this.plugin.getConfig().saveChanges();
        this.signs.clear();
    }

    @NotNull
    public Set<Sign> getSigns(@NotNull SignType signType) {
        return this.signs.computeIfAbsent(signType, k -> new HashSet<>());
    }

    public void update() {
        Stream.of(SignType.values()).forEach(this::update);
    }

    public void update(@NotNull SignType signType) {
        this.getSigns(signType).stream().filter(sign -> sign.getBlock().getState() instanceof Sign).forEach(sign -> {
            this.setSignText(sign, signType, Config.SIGNS_FORMAT.get().getOrDefault(signType, Collections.emptyList()));
        });
    }

    public void setSignText(@NotNull Sign sign, @NotNull SignType signType, @NotNull List<String> text2) {
        List<String> text = new ArrayList<>(text2);
        switch (signType) {
            case ARENA_JOIN, ARENA_LEAVE, ARENA_READY, ARENA_SHOP -> {
                Arena arena = this.getSignArena(sign);
                if (arena != null) text.replaceAll(arena.replacePlaceholders());
            }
            case KIT -> {
                Kit kit = this.getSignKit(sign);
                if (kit != null) text.replaceAll(kit.replacePlaceholders());
            }
            case STATS -> {
                String typeRaw = PDCUtil.getString(sign, Keys.SIGN_STAT_TYPE).orElse(null);
                StatType statType = typeRaw != null ? StringUtil.getEnum(typeRaw, StatType.class).orElse(null) : null;
                if (statType == null) return;

                int pos = PDCUtil.getInt(sign, Keys.SIGN_STAT_POSITION).orElse(-1);
                if (pos < 1) return;

                String arenaId = PDCUtil.getString(sign, Keys.SIGN_ARENA_ID).orElse(null);
                Arena arena = arenaId != null ? plugin.getArenaManager().getArenaById(arenaId) : null;
                StatsScore score = this.plugin.getStatsManager().getScore(statType, pos, arenaId);

                text.replaceAll(line -> score.replacePlaceholders(pos).apply(line)
                    .replace(Placeholders.ARENA_NAME, arena != null ? arena.getConfig().getName() : "")
                );

                String skullOwner = score.isEmpty() ? "MHF_Question" : score.getName();
                Block blockUponSign = sign.getBlock().getRelative(BlockFace.UP);
                if (blockUponSign.getState() instanceof Skull skull) {
                    skull.setOwner(skullOwner);
                    skull.update();
                }

                if (sign.getBlockData() instanceof Directional directional) {
                    Block blockSign = sign.getBlock().getRelative(directional.getFacing().getOppositeFace());
                    Block blockOnSignBlock = blockSign.getRelative(BlockFace.UP);

                    // Skull skin on the block that holds Sign
                    if (blockOnSignBlock.getState() instanceof Skull skull) {
                        skull.setOwner(skullOwner);
                        skull.update();
                    }
                    // NPC Skin for entity that are on block that holds Sign
                    else {
                        this.plugin.getStatsManager().setLeaderSkin(blockOnSignBlock, score.getName());
                    }
                }
            }
        }
        for (int index = 0; index < 4 && index < text.size(); index++) {
            sign.setLine(index, text.get(index));
        }
        sign.setGlowingText(Config.SIGNS_GLOWING.get());
        sign.update(true);
    }

    @Nullable
    public SignType getSignType(@NotNull Sign sign) {
        String typeRaw = PDCUtil.getString(sign, Keys.SIGN_TYPE).orElse(null);
        if (typeRaw == null) return null;

        return StringUtil.getEnum(typeRaw, SignType.class).orElse(null);
    }

    @Nullable
    public Arena getSignArena(@NotNull Sign sign) {
        String arenaId = PDCUtil.getString(sign, Keys.SIGN_ARENA_ID).orElse(null);
        return arenaId == null ? null : this.plugin.getArenaManager().getArenaById(arenaId);
    }

    @Nullable
    public Kit getSignKit(@NotNull Sign sign) {
        String kitId = PDCUtil.getString(sign, Keys.SIGN_KIT_ID).orElse(null);
        return kitId == null ? null : this.plugin.getKitManager().getKitById(kitId);
    }

    public boolean signCreate(@NotNull Sign sign, @NotNull String[] lines) {
        if (lines.length < 2) return false;

        String header = lines[0];
        if (!header.equalsIgnoreCase(HEADER)) return false;

        SignType signType = StringUtil.getEnum(lines[1], SignType.class).orElse(null);
        if (signType == null) return false;

        if (signType == SignType.ARENA_JOIN) {
            if (lines.length < 3) return false;
            String arenaId = lines[2];
            if (!plugin.getArenaManager().isArenaExists(arenaId)) return false;

            PDCUtil.set(sign, Keys.SIGN_ARENA_ID, arenaId);
        }
        else if (signType == SignType.KIT) {
            if (lines.length < 3) return false;
            String kitId = lines[2];
            if (!plugin.getKitManager().isKitExists(kitId)) return false;

            PDCUtil.set(sign, Keys.SIGN_KIT_ID, kitId);
        }
        else if (signType == SignType.STATS) {
            if (lines.length < 4) return false;

            StatType type = StringUtil.getEnum(lines[2], StatType.class).orElse(null);
            if (type == null) return false;

            String[] line3 = lines[3].split(":");
            int pos = StringUtil.getInteger(line3[0], -1);
            if (pos <= 0) return false;

            Arena arena = line3.length >= 2 ? this.plugin.getArenaManager().getArenaById(line3[1]) : null;

            PDCUtil.set(sign, Keys.SIGN_STAT_TYPE, type.name());
            PDCUtil.set(sign, Keys.SIGN_STAT_POSITION, pos);
            if (arena != null) PDCUtil.set(sign, Keys.SIGN_ARENA_ID, arena.getId());
        }

        PDCUtil.set(sign, Keys.SIGN_TYPE, signType.name());
        //sign.update();
        this.getSigns(signType).add(sign);
        this.plugin.getScheduler().runTask(this.plugin, () -> this.update(signType));
        return true;
    }

    public boolean signInteract(@NotNull Player player, @NotNull Sign sign) {
        SignType signType = this.getSignType(sign);
        if (signType == null) return false;

        if (player.isSneaking() && player.getGameMode() == GameMode.CREATIVE) return false;

        switch (signType) {
            case ARENA_JOIN -> {
                Arena arena = this.getSignArena(sign);
                if (arena == null) return false;

                arena.joinLobby(player);
            }
            case ARENA_SHOP -> {
                ArenaPlayer arenaPlayer = ArenaPlayer.getPlayer(player);
                if (arenaPlayer == null) return false;

                arenaPlayer.getArena().getConfig().getShopManager().open(player);
            }
            case ARENA_LEAVE -> {
                ArenaPlayer arenaPlayer = ArenaPlayer.getPlayer(player);
                if (arenaPlayer == null) return false;

                arenaPlayer.leaveArena();
            }
            case ARENA_READY -> {
                ArenaPlayer arenaPlayer = ArenaPlayer.getPlayer(player);
                if (arenaPlayer == null || arenaPlayer.isInGame()) return false;

                arenaPlayer.setState(arenaPlayer.isReady() ? GameState.WAITING : GameState.READY);
            }
            case KIT -> {
                Kit kit = this.getSignKit(sign);
                if (kit == null) return false;

                this.plugin.getKitManager().selectOrPurchase(player, kit);
            }
            case KIT_SHOP -> {
                this.plugin.getKitManager().openShop(player);
            }
            case KIT_SELECTOR -> {
                this.plugin.getKitManager().openSelector(player);
            }
            case STATS_OPEN -> this.plugin.getStatsManager().getStatsMenu().open(player, 1);
        }

        return true;
    }
}
