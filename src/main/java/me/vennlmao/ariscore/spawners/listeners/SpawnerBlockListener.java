package me.vennlmao.ariscore.spawners.listeners;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.spawners.managers.SpawnerData;
import me.vennlmao.ariscore.spawners.utils.MessageUtil;
import me.vennlmao.ariscore.spawners.utils.SoundUtil;
import me.vennlmao.ariscore.spawners.utils.SpawnerItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SpawnerBlockListener implements Listener {

    private final SpawnersModule module;

    public SpawnerBlockListener(SpawnersModule module) {
        this.module = module;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!SpawnerItemUtil.isSpawnerItem(item)) return;

        EntityType type = SpawnerItemUtil.getEntityType(item);
        if (type == null || !module.getSpawnerDefinitionManager().has(type)) return;

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.SPAWNER) return;

        long amount = SpawnerItemUtil.getStackAmount(item);

        if (block.getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(type);
            spawner.setSpawnCount(0);
            spawner.setMaxNearbyEntities(0);
            spawner.setRequiredPlayerRange(0);
            spawner.update(true, false);
        }

        module.getSpawnerManager().register(block.getLocation(), type, amount, event.getPlayer());
        MessageUtil.sendChat(event.getPlayer(), "placed",
                s -> s.replace("{mob}", SpawnerItemUtil.mobName(type)).replace("{amount}", String.valueOf(amount)));
        SoundUtil.play(event.getPlayer(), "place");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SPAWNER) return;

        SpawnerData data = module.getSpawnerManager().get(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        ItemStack hand = event.getItem();

        if (SpawnerItemUtil.isSpawnerItem(hand) && SpawnerItemUtil.getEntityType(hand) == data.getEntityType()) {
            event.setCancelled(true);
            long handAmount = SpawnerItemUtil.getStackAmount(hand);
            long addAmount = player.isSneaking() ? handAmount : 1;

            if (module.getSpawnerManager().isMaxStack(data, addAmount)) {
                MessageUtil.sendChat(player, "max_stack_reached");
                return;
            }

            data.addAmount(addAmount);
            module.getSpawnerManager().saveNow(data);

            long remaining = handAmount - addAmount;
            if (remaining <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                ItemMeta meta = hand.getItemMeta();
                meta.getPersistentDataContainer().set(
                        new org.bukkit.NamespacedKey(module.getPlugin(), "spawner_stack_amount"),
                        org.bukkit.persistence.PersistentDataType.LONG, remaining);
                hand.setItemMeta(meta);
            }

            MessageUtil.sendActionbar(player, "stacked",
                    s -> s.replace("{amount}", String.valueOf(data.getAmount())).replace("{mob}", SpawnerItemUtil.mobName(data.getEntityType())));
            SoundUtil.play(player, "stack");
            return;
        }

        event.setCancelled(true);
        module.getGuiListener().openInfo(player, data);
        SoundUtil.play(player, "open");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        SpawnerData data = module.getSpawnerManager().get(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        boolean silkRequired = module.getConfig().getBoolean("silk-touch-required", true);
        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        if (silkRequired && !hasSilkTouch) {
            event.setCancelled(true);
            MessageUtil.sendChat(player, "silk_touch_required");
            return;
        }

        event.setDropItems(false);

        int maxBreakStack = module.getConfig().getInt("max-break-stack", 5000);
        final long dropAmount = player.isSneaking() ? Math.min(data.getAmount(), maxBreakStack) : Math.min(data.getAmount(), 1);
        long remaining = data.getAmount() - dropAmount;

        Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
        ItemStack drop = SpawnerItemUtil.createItem(data.getEntityType(), dropAmount);
        block.getWorld().dropItemNaturally(dropLoc, drop);

        if (remaining > 0) {
            event.setCancelled(true);
            data.setAmount(remaining);
            module.getSpawnerManager().saveNow(data);
            MessageUtil.sendChat(player, "broke_partial",
                    s -> s.replace("{amount}", String.valueOf(dropAmount)).replace("{remaining}", String.valueOf(remaining)));
        } else {
            module.getSpawnerManager().unregister(data);
            MessageUtil.sendChat(player, "broke_full");
        }

        SoundUtil.play(player, "break");
    }
                }
