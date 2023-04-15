package su.nightexpress.ama.api.arena.type;

public enum ArenaGameEventType {
    GAME_START, GAME_END_WIN, GAME_END_LOSE, GAME_END_TIME,
    WAVE_START, WAVE_END,
    SHOP_LOCKED, SHOP_UNLOCKED,
    SHOP_CATEGORY_LOCKED, SHOP_CATEGORY_UNLOCKED,
    SHOP_ITEM_UNLOCKED, SHOP_ITEM_LOCKED,
    PLAYER_JOIN, PLAYER_LEAVE, PLAYER_DEATH,
    MOB_KILLED,
    REGION_LOCKED, REGION_UNLOCKED,
    SPOT_CHANGED,
}
