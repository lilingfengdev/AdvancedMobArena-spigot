package su.nightexpress.ama.arena.editor.spot;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.editor.EditorButtonType;
import su.nexmedia.engine.api.editor.EditorInput;
import su.nexmedia.engine.api.menu.MenuClick;
import su.nexmedia.engine.api.menu.MenuItemType;
import su.nexmedia.engine.editor.AbstractEditorMenuAuto;
import su.nexmedia.engine.editor.EditorManager;
import su.nexmedia.engine.utils.ItemUtil;
import su.nightexpress.ama.AMA;
import su.nightexpress.ama.arena.spot.ArenaSpot;
import su.nightexpress.ama.arena.spot.ArenaSpotState;
import su.nightexpress.ama.config.Lang;
import su.nightexpress.ama.editor.ArenaEditorHub;
import su.nightexpress.ama.editor.ArenaEditorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

class SpotStatesEditor extends AbstractEditorMenuAuto<AMA, ArenaSpot, ArenaSpotState> {

    public SpotStatesEditor(@NotNull ArenaSpot spot) {
        super(spot.plugin(), spot, ArenaEditorHub.TITLE_SPOT_EDITOR, 45);

        EditorInput<ArenaSpot, ArenaEditorType> input = (player, spot2, type, e) -> {
            String msg = e.getMessage();
            if (type == ArenaEditorType.SPOT_STATE_CREATE) {
                String id = EditorManager.fineId(msg);
                if (spot2.getState(id) != null) {
                    EditorManager.error(player, plugin.getMessage(Lang.EDITOR_SPOT_STATE_ERROR_EXISTS).getLocalized());
                    return false;
                }

                ArenaSpotState state = new ArenaSpotState(spot, id, new ArrayList<>());
                spot2.getStates().put(state.getId(), state);
            }

            spot2.save();
            return true;
        };

        MenuClick click = (player, type, e) -> {
            if (type instanceof MenuItemType type2) {
                if (type2 == MenuItemType.RETURN) {
                    spot.getEditor().open(player, 1);
                }
                else this.onItemClickDefault(player, type2);
            }
            else if (type instanceof ArenaEditorType type2) {
                if (type2 == ArenaEditorType.SPOT_STATE_CREATE) {
                    EditorManager.startEdit(player, spot, type2, input);
                    EditorManager.prompt(player, plugin.getMessage(Lang.EDITOR_SPOT_STATE_ENTER_ID).getLocalized());
                    player.closeInventory();
                }
            }
        };

        this.loadItems(click);
    }

    @Override
    public void setTypes(@NotNull Map<EditorButtonType, Integer> map) {
        map.put(ArenaEditorType.SPOT_STATE_CREATE, 41);
        map.put(MenuItemType.RETURN, 39);
        map.put(MenuItemType.PAGE_NEXT, 44);
        map.put(MenuItemType.PAGE_PREVIOUS, 36);
    }

    @Override
    public int[] getObjectSlots() {
        return IntStream.range(0, 36).toArray();
    }

    @Override
    @NotNull
    protected List<ArenaSpotState> getObjects(@NotNull Player player) {
        return new ArrayList<>(this.parent.getStates().values());
    }

    @Override
    @NotNull
    protected ItemStack getObjectStack(@NotNull Player player, @NotNull ArenaSpotState state) {
        ItemStack item = ArenaEditorType.SPOT_STATE_OBJECT.getItem();
        ItemUtil.replace(item, state.replacePlaceholders());
        return item;
    }

    @Override
    @NotNull
    protected MenuClick getObjectClick(@NotNull Player player, @NotNull ArenaSpotState state) {
        return (p, type, e) -> {
            if (e.isShiftClick()) {
                if (e.isRightClick()) {
                    this.parent.getStates().remove(state.getId());
                    this.parent.save();
                    this.open(p, this.getPage(p));
                }
                return;
            }

            p.closeInventory();
            plugin.getArenaSetupManager().getSpotStateSetupManager().startSetup(player, state);
        };
    }

    @Override
    public boolean cancelClick(@NotNull InventoryClickEvent e, @NotNull SlotType slotType) {
        return true;
    }
}
