package com.JOMPILS.milkyourfriends;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class EntityHandler {
    private static final boolean DEBUG = false; // Disable debug messages in production

    // Map using a composite key ("playerUUID:HAND") to track per-hand processing per tick.
    private static final Map<String, Long> lastMilkTick = new HashMap<>();
    // Map to record whether the player’s main hand had an empty bucket during the current tick.
    private static final Map<UUID, Long> mainHandHadBucketTick = new HashMap<>();

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient && entity instanceof PlayerEntity target) {
                UUID playerUUID = player.getUuid();
                long currentTick = world.getTime();
                String key = playerUUID.toString() + ":" + hand.toString();

                // Process only one event per hand per tick.
                if (lastMilkTick.containsKey(key) && lastMilkTick.get(key) == currentTick) {
                    if (DEBUG) {
                        System.out.println("[MilkableEntityHandler] Already processed " + hand + " for " + player.getName().getString() + " this tick.");
                    }
                    return ActionResult.PASS;
                }
                lastMilkTick.put(key, currentTick);

                // For main hand events, record that the main hand had an empty bucket this tick.
                if (hand == Hand.MAIN_HAND) {
                    if (player.getStackInHand(Hand.MAIN_HAND).getItem() == Items.BUCKET) {
                        mainHandHadBucketTick.put(playerUUID, currentTick);
                    } else {
                        mainHandHadBucketTick.remove(playerUUID);
                    }
                }
                // For offhand events, if the main hand had a bucket this tick, skip processing.
                if (hand == Hand.OFF_HAND) {
                    if (mainHandHadBucketTick.containsKey(playerUUID) && mainHandHadBucketTick.get(playerUUID) == currentTick) {
                        if (DEBUG) {
                            System.out.println("[MilkableEntityHandler] Main hand had an empty bucket this tick; skipping offhand event for " + player.getName().getString());
                        }
                        return ActionResult.PASS;
                    }
                }

                if (isPlayerMilkable(target)) { // target defined as pattern variable line 28
                    // Process only if the triggered hand holds a bucket.
                    if (player.getStackInHand(hand).getItem() == Items.BUCKET) {
                        if (DEBUG) {
                            System.out.println("[MilkableEntityHandler] Processing milking for " + player.getName().getString() + " using hand: " + hand);
                        }
                        if (tryMilk(player, hand)) {
                            return ActionResult.SUCCESS;
                        }
                    }
                } else if (DEBUG) {
                    System.out.println("[MilkableEntityHandler] Target is not milkable.");
                }
            }
            return ActionResult.PASS;
        });
    }

    private static boolean tryMilk(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() == Items.BUCKET) {
            if (DEBUG) {
                System.out.println("[MilkableEntityHandler] tryMilk called for "
                        + player.getName().getString() + " with hand: " + hand + ", bucket count: " + stack.getCount());
            }
            // Process inventory changes based on game mode.
            if (player.isCreative()) {
                boolean hasMilkBucket = false;
                for (int i = 0; i < player.getInventory().size(); i++) {
                    if (player.getInventory().getStack(i).getItem() == Items.MILK_BUCKET) {
                        hasMilkBucket = true;
                        break;
                    }
                }
                if (!hasMilkBucket) {
                    ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
                    if (!player.getInventory().insertStack(milkBucket)) {
                        player.dropItem(milkBucket, false);
                    }
                    if (DEBUG) {
                        System.out.println("[MilkableEntityHandler] Creative mode: Added milk bucket to inventory.");
                    }
                } else if (DEBUG) {
                    System.out.println("[MilkableEntityHandler] Creative mode: Milk bucket already in inventory.");
                }
            } else {
                // Survival mode: update the inventory.
                if (stack.getCount() > 1) {
                    stack.decrement(1);
                    if (DEBUG) {
                        System.out.println("[MilkableEntityHandler] Survival mode: Decremented bucket stack. New count: " + stack.getCount());
                    }
                    ItemStack milkBucket = new ItemStack(Items.MILK_BUCKET);
                    if (!player.getInventory().insertStack(milkBucket)) {
                        player.dropItem(milkBucket, false);
                    }
                } else {
                    if (DEBUG) {
                        System.out.println("[MilkableEntityHandler] Survival mode: Single bucket; replacing it with a milk bucket.");
                    }
                    player.setStackInHand(hand, new ItemStack(Items.MILK_BUCKET));
                }
            }
            // Trigger the full swing-hand animation.
            player.swingHand(hand, true);
            // Play the cow milking sound.
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_COW_MILK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return true;
        }
        if (DEBUG) {
            System.out.println("[MilkableEntityHandler] tryMilk did not find a bucket in hand: " + hand);
        }
        return false;
    }

    private static boolean isPlayerMilkable(PlayerEntity target) {
        UUID targetUuid = target.getUuid();
        Set<UUID> configList = Config.getMilkPlayers();
        String listType = Config.getMilkListType();
        // check if list type is currently whitelist. If it is, players on the list are milkable. if it is not, players NOT on the list are milkable.
        boolean milkable = "whitelist".equalsIgnoreCase(listType) == configList.contains(targetUuid);
        if (DEBUG) {
            System.out.println("[MilkableEntityHandler] isPlayerMilkable: Target "
                    + target.getName().getString() + " (UUID: " + targetUuid + ") milkable? " + milkable);
        }
        return milkable;
    }
}
