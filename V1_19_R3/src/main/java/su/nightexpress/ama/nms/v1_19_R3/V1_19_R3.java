package su.nightexpress.ama.nms.v1_19_R3;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.item.SpawnEggItem;
import org.bukkit.Location;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftMob;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftNamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.utils.Colorizer;
import su.nexmedia.engine.utils.Reflex;
import su.nightexpress.ama.api.arena.IArena;
import su.nightexpress.ama.api.type.MobFaction;
import su.nightexpress.ama.nms.ArenaNMS;
import su.nightexpress.ama.nms.v1_19_R3.brain.MobBrain;
import su.nightexpress.ama.nms.v1_19_R3.brain.goal.FollowOwnerGoal;
import su.nightexpress.ama.nms.v1_19_R3.brain.goal.LastDamagerTargetGoal;
import su.nightexpress.ama.nms.v1_19_R3.brain.goal.MeleeAttackGoal;
import su.nightexpress.ama.nms.v1_19_R3.brain.goal.NearestFactionTargetGoal;

import java.util.HashMap;
import java.util.Map;

public class V1_19_R3 implements ArenaNMS {

    public V1_19_R3() {
        EntityInjector.setup();
    }

    @Override
    @Nullable
    public EntityType getSpawnEggType(@NotNull ItemStack itemStack) {
        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        if (nmsStack.getItem() instanceof SpawnEggItem eggItem) {
            net.minecraft.world.entity.EntityType<?> type = eggItem.getType(nmsStack.getTag());
            ResourceLocation location = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            return Registry.ENTITY_TYPE.get(CraftNamespacedKey.fromMinecraft(location));
        }
        return null;
    }

    @Override
    public LivingEntity spawnMob(@NotNull IArena arena, @NotNull MobFaction faction, @NotNull EntityType type, @NotNull Location location) {
        net.minecraft.world.entity.Mob mob = EntityInjector.spawnEntity(arena, faction, type, location);
        if (mob == null) return null;

        LivingEntity bukkitEntity = (LivingEntity) mob.getBukkitEntity();

        bukkitEntity.teleport(location);
        this.registerAttribute(mob, Attributes.ARMOR);
        this.registerAttribute(mob, Attributes.ARMOR_TOUGHNESS);
        this.registerAttribute(mob, Attributes.ATTACK_DAMAGE);
        this.registerAttribute(mob, Attributes.ATTACK_KNOCKBACK);
        this.registerAttribute(mob, Attributes.ATTACK_SPEED);
        this.setAttribute(mob, Attributes.FOLLOW_RANGE, 256D);
        this.registerAttribute(mob, Attributes.FLYING_SPEED);
        this.registerAttribute(mob, Attributes.JUMP_STRENGTH);
        this.registerAttribute(mob, Attributes.KNOCKBACK_RESISTANCE);
        this.registerAttribute(mob, Attributes.MAX_HEALTH);
        this.registerAttribute(mob, Attributes.MOVEMENT_SPEED);

        if (mob.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 0) {
            this.setAttribute(mob, Attributes.ATTACK_DAMAGE, 1);
        }

        if (mob instanceof PathfinderMob pathfinderMob) {
            if (bukkitEntity instanceof Animals || bukkitEntity instanceof org.bukkit.entity.IronGolem) {
                mob.goalSelector.getAvailableGoals().clear();
                mob.goalSelector.addGoal(0, new FloatGoal(mob));
                mob.goalSelector.addGoal(2, new MeleeAttackGoal(pathfinderMob, arena, faction));
                mob.goalSelector.addGoal(8, new LookAtPlayerGoal(pathfinderMob, net.minecraft.world.entity.player.Player.class, 8.0F));
            }
            else {
                if (mob instanceof net.minecraft.world.entity.monster.Drowned drowned) {
                    drowned.goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof ZombieAttackGoal);
                    drowned.goalSelector.addGoal(3, new ZombieAttackGoal(drowned, 1D, false));
                }
                pathfinderMob.goalSelector.getAvailableGoals().removeIf(wrappedGoal -> {
                    var goal = wrappedGoal.getGoal();
                    return goal instanceof FleeSunGoal || goal instanceof AvoidEntityGoal<?> || goal instanceof RestrictSunGoal;
                });
            }
        }

        if (!EntityInjector.BRAINED.containsKey(type)) {
            mob.targetSelector.getAvailableGoals().clear();
            mob.targetSelector.addGoal(1, new LastDamagerTargetGoal(mob, arena, faction));
            mob.targetSelector.addGoal(2, new NearestFactionTargetGoal<>(mob, arena, faction));
            mob.setAggressive(true);
        }
        return bukkitEntity;
    }

    private void registerAttribute(@NotNull net.minecraft.world.entity.LivingEntity handle, @NotNull Attribute att) {
        AttributeInstance instance = handle.getAttribute(att);

        if (instance == null) {
            // Hardcode to register missing entity's attributes.
            AttributeSupplier provider = (AttributeSupplier) Reflex.getFieldValue(handle.getAttributes(), "d");
            if (provider == null) return;

            @SuppressWarnings("unchecked")
            Map<Attribute, AttributeInstance> aMap = (Map<Attribute, AttributeInstance>) Reflex.getFieldValue(provider, "a");
            if (aMap == null) return;

            Map<Attribute, AttributeInstance> aMap2 = new HashMap<>(aMap);
            aMap2.put(att, new AttributeInstance(att, var1 -> {

            }));
            Reflex.setFieldValue(provider, "a", aMap2);
            //System.out.println("Injected Attribute: " + att.getDescriptionId());
        }
    }

    private void setAttribute(@NotNull net.minecraft.world.entity.LivingEntity handle, @NotNull Attribute attribute, double value) {
        this.registerAttribute(handle, attribute);

        AttributeInstance instance = handle.getAttribute(attribute);
        if (instance == null) {
            //System.out.println("Could not create attribute instance: " + attribute.getDescriptionId());
            return;
        }
        instance.setBaseValue(value);
    }

    @Override
    public void setFollowGoal(@NotNull LivingEntity bukkitMob, @NotNull Player bukkitPlayer) {
        net.minecraft.world.entity.Mob mob = ((CraftMob)bukkitMob).getHandle();
        net.minecraft.world.entity.player.Player player = ((CraftPlayer)bukkitPlayer).getHandle();

        if (!EntityInjector.BRAINED.containsKey(bukkitMob.getType())) {
            mob.goalSelector.addGoal(6, new FollowOwnerGoal(mob, player));
        }
        else {
            MobBrain.setOwnerMemory(mob, player);
        }
    }

    @Override
    public int visualEntityAdd(@NotNull Player player, @NotNull String name, @NotNull Location loc) {
        org.bukkit.World w = loc.getWorld();
        if (w == null) return -1;

        ServerLevel world = ((CraftWorld) w).getHandle();
        net.minecraft.world.entity.decoration.ArmorStand entity = new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityType.ARMOR_STAND, world);
        ArmorStand armorStand = (ArmorStand) entity.getBukkitEntity();

        entity.moveTo(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
        entity.setYHeadRot(0);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        entity.getBukkitEntity().setCustomName(Colorizer.apply(name));
        armorStand.setSmall(true);
        armorStand.setGravity(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setSilent(true);

        ClientboundAddEntityPacket spawnEntityLiving = new ClientboundAddEntityPacket(entity);
        ((CraftPlayer) player).getHandle().connection.send(spawnEntityLiving);

        ClientboundSetEntityDataPacket entityMetadata = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().packDirty());
        ((CraftPlayer) player).getHandle().connection.send(entityMetadata);

        return entity.getId();
    }

    @Override
    public int visualGlowBlockAdd(@NotNull Player player, @NotNull Location loc) {
        org.bukkit.World w = loc.getWorld();
        if (w == null) return -1;

        ServerLevel world = ((CraftWorld) w).getHandle();
        net.minecraft.world.entity.monster.Shulker entity = new net.minecraft.world.entity.monster.Shulker(net.minecraft.world.entity.EntityType.SHULKER, world);
        Shulker shulker = (Shulker) entity.getBukkitEntity();

        entity.moveTo(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
        entity.setYHeadRot(0);
        shulker.setInvisible(true);
        shulker.setInvulnerable(true);
        shulker.setGravity(false);
        shulker.setCustomNameVisible(true);
        shulker.setSilent(true);
        shulker.setGlowing(true);

        ClientboundAddEntityPacket spawnEntityLiving = new ClientboundAddEntityPacket(entity);
        ((CraftPlayer) player).getHandle().connection.send(spawnEntityLiving);

        ClientboundSetEntityDataPacket entityMetadata = new ClientboundSetEntityDataPacket(entity.getId(), entity.getEntityData().packDirty());
        ((CraftPlayer) player).getHandle().connection.send(entityMetadata);

        return entity.getId();
    }

    @Override
    public void visualEntityRemove(@NotNull Player player, int... ids) {
        ClientboundRemoveEntitiesPacket packetPlayOutEntityDestroy = new ClientboundRemoveEntitiesPacket(ids);
        ((CraftPlayer) player).getHandle().connection.send(packetPlayOutEntityDestroy);
    }
}
