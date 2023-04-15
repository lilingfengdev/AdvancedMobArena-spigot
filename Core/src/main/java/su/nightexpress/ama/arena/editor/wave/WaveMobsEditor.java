package su.nightexpress.ama.arena.editor.wave;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.editor.EditorButtonType;
import su.nexmedia.engine.api.editor.EditorInput;
import su.nexmedia.engine.api.menu.MenuClick;
import su.nexmedia.engine.api.menu.MenuItemType;
import su.nexmedia.engine.editor.AbstractEditorMenuAuto;
import su.nexmedia.engine.editor.EditorManager;
import su.nexmedia.engine.utils.CollectionsUtil;
import su.nexmedia.engine.utils.Colorizer;
import su.nexmedia.engine.utils.ItemUtil;
import su.nexmedia.engine.utils.StringUtil;
import su.nightexpress.ama.AMA;
import su.nightexpress.ama.arena.wave.ArenaWave;
import su.nightexpress.ama.arena.wave.ArenaWaveMob;
import su.nightexpress.ama.config.Lang;
import su.nightexpress.ama.editor.ArenaEditorHub;
import su.nightexpress.ama.editor.ArenaEditorType;
import su.nightexpress.ama.hook.mob.MobProvider;
import su.nightexpress.ama.hook.mob.PluginMobProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class WaveMobsEditor extends AbstractEditorMenuAuto<AMA, ArenaWave, ArenaWaveMob> {

    public WaveMobsEditor(@NotNull ArenaWave arenaWave) {
        super(arenaWave.getArena().plugin(), arenaWave, ArenaEditorHub.TITLE_WAVE_EDITOR, 45);

        MenuClick click = (player, type, e) -> {
            if (type instanceof MenuItemType type2) {
                if (type2 == MenuItemType.RETURN) {
                    this.parent.getArenaConfig().getWaveManager().getEditor().getWavesListEditor().open(player, 1);
                }
                else super.onItemClickDefault(player, type2);
            }
            else if (type instanceof ArenaEditorType type2) {
                if (type2 == ArenaEditorType.WAVES_WAVE_MOB_CREATE) {
                    MobProvider provider = PluginMobProvider.getProviders().stream().findAny().orElseThrow();
                    ArenaWaveMob mob = new ArenaWaveMob(arenaWave, provider, "null", 1, 1, 100D);
                    arenaWave.getMobs().add(mob);
                    arenaWave.getArenaConfig().getWaveManager().save();
                    this.open(player, this.getPage(player));
                }
            }
        };

        this.loadItems(click);
    }

    @Override
    public void setTypes(@NotNull Map<EditorButtonType, Integer> map) {
        map.put(ArenaEditorType.WAVES_WAVE_MOB_CREATE, 41);
        map.put(MenuItemType.RETURN, 39);
        map.put(MenuItemType.PAGE_NEXT, 44);
        map.put(MenuItemType.PAGE_PREVIOUS, 36);
    }

    @Override
    public boolean cancelClick(@NotNull InventoryClickEvent e, @NotNull SlotType slotType) {
        return true;
    }

    @Override
    public int[] getObjectSlots() {
        return IntStream.range(0, 36).toArray();
    }

    @Override
    @NotNull
    protected List<ArenaWaveMob> getObjects(@NotNull Player player) {
        return new ArrayList<>(this.parent.getMobs());
    }

    @Override
    @NotNull
    protected ItemStack getObjectStack(@NotNull Player player, @NotNull ArenaWaveMob waveMob) {
        ItemStack item = ArenaEditorType.WAVES_WAVE_MOB_OBJECT.getItem();
        ItemUtil.replace(item, waveMob.replacePlaceholders());
        return item;
    }

    @Override
    @NotNull
    protected MenuClick getObjectClick(@NotNull Player player, @NotNull ArenaWaveMob waveMob) {
        EditorInput<ArenaWaveMob, ArenaEditorType> input = (player2, mob, type, e) -> {
            String msg = Colorizer.strip(e.getMessage());
            switch (type) {
                case WAVES_WAVE_MOB_CHANGE_TYPE -> {
                    if (!mob.getProvider().getMobNames().contains(msg)) {
                        EditorManager.error(player, plugin.getMessage(Lang.EDITOR_ARENA_WAVES_ERROR_MOB_INVALID).getLocalized());
                        return false;
                    }
                    mob.setMobId(msg);
                }
                case WAVES_WAVE_MOB_CHANGE_AMOUNT -> mob.setAmount(StringUtil.getInteger(msg, 1));
                case WAVES_WAVE_MOB_CHANGE_LEVEL -> mob.setLevel(StringUtil.getInteger(msg, 1));
                case WAVES_WAVE_MOB_CHANGE_CHANCE -> mob.setChance(StringUtil.getDouble(msg, 0D));
            }

            mob.getArenaWave().getArenaConfig().getWaveManager().save();
            return true;
        };

        return (player2, type, e) -> {
            if (e.isShiftClick() && e.isRightClick()) {
                this.parent.getMobs().remove(waveMob);
                this.parent.getArenaConfig().getWaveManager().save();
                this.open(player2, this.getPage(player2));
                return;
            }

            if (e.isShiftClick()) {
                if (e.isLeftClick()) {
                    EditorManager.startEdit(player2, waveMob, ArenaEditorType.WAVES_WAVE_MOB_CHANGE_LEVEL, input);
                    EditorManager.prompt(player2, plugin.getMessage(Lang.EDITOR_GENERIC_ENTER_NUMBER).getLocalized());
                }
            }
            else {
                if (e.isLeftClick()) {
                    EditorManager.startEdit(player2, waveMob, ArenaEditorType.WAVES_WAVE_MOB_CHANGE_TYPE, input);
                    EditorManager.suggestValues(player2, waveMob.getProvider().getMobNames(), true);
                    EditorManager.prompt(player2, plugin.getMessage(Lang.EDITOR_ARENA_WAVES_ENTER_MOB_ID).getLocalized());
                }
                else if (e.isRightClick()) {
                    EditorManager.startEdit(player2, waveMob, ArenaEditorType.WAVES_WAVE_MOB_CHANGE_AMOUNT, input);
                    EditorManager.prompt(player2, plugin.getMessage(Lang.EDITOR_GENERIC_ENTER_NUMBER).getLocalized());
                }
                else if (e.getClick() == ClickType.DROP) {
                    EditorManager.startEdit(player2, waveMob, ArenaEditorType.WAVES_WAVE_MOB_CHANGE_CHANCE, input);
                    EditorManager.prompt(player2, plugin.getMessage(Lang.EDITOR_GENERIC_ENTER_PERCENT).getLocalized());
                }
                else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                    List<MobProvider> providers = new ArrayList<>(PluginMobProvider.getProviders());
                    waveMob.setProvider(CollectionsUtil.shifted(providers, providers.indexOf(waveMob.getProvider()), 1));
                    this.parent.getArenaConfig().getWaveManager().save();
                    this.open(player2, this.getPage(player2));
                    return;
                }
                else return;
            }
            player2.closeInventory();
        };
    }
}
