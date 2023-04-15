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
import su.nightexpress.ama.arena.spot.ArenaSpotManager;
import su.nightexpress.ama.config.Lang;
import su.nightexpress.ama.editor.ArenaEditorHub;
import su.nightexpress.ama.editor.ArenaEditorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class SpotListEditor extends AbstractEditorMenuAuto<AMA, ArenaSpotManager, ArenaSpot> {

    public SpotListEditor(@NotNull ArenaSpotManager spotManager) {
        super(spotManager.plugin(), spotManager, ArenaEditorHub.TITLE_SPOT_EDITOR, 45);

        EditorInput<ArenaSpotManager, ArenaEditorType> input = (player, spotManager2, type, e) -> {
            String msg = e.getMessage();
            if (type == ArenaEditorType.SPOT_CREATE) {
                if (!spotManager2.createSpot(EditorManager.fineId(msg))) {
                    EditorManager.error(player, plugin.getMessage(Lang.EDITOR_SPOT_ERROR_EXISTS).getLocalized());
                    return false;
                }
            }
            return true;
        };

        MenuClick click = (player, type, e) -> {
            if (type instanceof MenuItemType type2) {
                if (type2 == MenuItemType.RETURN) {
                    spotManager.getArenaConfig().getEditor().open(player, 1);
                }
                else this.onItemClickDefault(player, type2);
            }
            else if (type instanceof ArenaEditorType type2) {
                if (type2 == ArenaEditorType.SPOT_CREATE) {
                    EditorManager.startEdit(player, spotManager, type2, input);
                    EditorManager.prompt(player, plugin.getMessage(Lang.EDITOR_SPOT_ENTER_ID).getLocalized());
                    player.closeInventory();
                }
            }
        };


        this.loadItems(click);
    }

    @Override
    public void setTypes(@NotNull Map<EditorButtonType, Integer> map) {
        map.put(ArenaEditorType.SPOT_CREATE, 41);
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
    protected List<ArenaSpot> getObjects(@NotNull Player player) {
        return new ArrayList<>(this.parent.getSpots());
    }

    @Override
    @NotNull
    protected ItemStack getObjectStack(@NotNull Player player, @NotNull ArenaSpot spot) {
        ItemStack item = ArenaEditorType.SPOT_OBJECT.getItem();
        ItemUtil.replace(item, spot.replacePlaceholders());
        return item;
    }

    @Override
    @NotNull
    protected MenuClick getObjectClick(@NotNull Player player, @NotNull ArenaSpot spot) {
        return (p, type, e) -> {
            if (e.isShiftClick() && e.isRightClick()) {
                this.parent.removeSpot(spot);
                this.open(p, this.getPage(p));
                return;
            }
            spot.getEditor().open(p, 1);
        };
    }

    @Override
    public boolean cancelClick(@NotNull InventoryClickEvent e, @NotNull SlotType slotType) {
        return true;
    }
}
