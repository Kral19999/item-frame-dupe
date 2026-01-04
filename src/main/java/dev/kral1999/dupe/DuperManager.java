package dev.kral1999.dupe;

import dev.kral1999.dupe.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFrameItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DuperManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final List<FrameSnapshot> targetFrames = new ArrayList<>();
    private static final List<ItemFrameEntity> dontHit = new ArrayList<>();

    private record FrameSnapshot(Vec3d pos, BlockPos supportPos, net.minecraft.util.math.Direction facing) {
    }

    private static int dupeCount = 0;

    private static int placements = 0;
    private static int swaps = 0;
    private static int moves = 0;

    public static void onDupeKeyPressed() {
        Config.INSTANCE.enabled = !Config.INSTANCE.enabled;
        Config.save();
        if (Config.INSTANCE.enabled) {
            reloadFrames();
        } else {
            targetFrames.clear();
        }
    }

    public static void reloadFrames() {
        targetFrames.clear();
        if (mc.player == null || mc.world == null)
            return;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;
        List<ItemFrameEntity> detected = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (entity.squaredDistanceTo(mc.player) <= rangeSq) {
                    detected.add(itemFrame);
                }
            }
        }

        detected.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = Config.INSTANCE.maxFrames;
        if (detected.size() > limit) {
            detected = detected.subList(0, limit);
        }

        for (ItemFrameEntity frame : detected) {
            BlockPos supportPos = Utils.Vec3d2BlockPos(frame.getPos().subtract(frame.getRotationVector().normalize()));
            targetFrames.add(new FrameSnapshot(frame.getPos(), supportPos, frame.getHorizontalFacing()));
        }
    }

    public static void tick() {
        if (!Config.INSTANCE.enabled || mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        placements = 0;
        swaps = 0;
        moves = 0;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;
        List<Item> dupeItemList = Config.getDupeItems();

        List<ItemFrameEntity> allDetectedFrames = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (entity.squaredDistanceTo(mc.player.getEyePos()) <= rangeSq) {
                    allDetectedFrames.add(itemFrame);
                }
            }
        }

        if (Config.INSTANCE.mode == Config.Mode.Normal) {
            handleNormalMode(allDetectedFrames, dupeItemList);
        } else if (Config.INSTANCE.mode == Config.Mode.Speed) {
            handleSpeedMode(allDetectedFrames, dupeItemList);
        }
    }

    private static void handleNormalMode(List<ItemFrameEntity> allDetectedFrames, List<Item> dupeItemList) {
        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;
        int maxInventoryMoves = Config.INSTANCE.maxInventoryMoves;
        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;

        List<ItemFrameEntity> activeFrames = new ArrayList<>(allDetectedFrames);
        activeFrames.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = Config.INSTANCE.maxFrames;
        if (activeFrames.size() > limit) {
            activeFrames = activeFrames.subList(0, limit);
        }

        List<ItemFrameEntity> emptyItemFrames = new ArrayList<>();
        List<ItemFrameEntity> filledItemFrames = new ArrayList<>();

        for (ItemFrameEntity frame : activeFrames) {
            if (frame.squaredDistanceTo(mc.player.getEyePos()) > rangeSq)
                continue;

            if (frame.getHeldItemStack().getItem() == Items.AIR) {
                emptyItemFrames.add(frame);
            } else if (dupeItemList.contains(frame.getHeldItemStack().getItem())) {
                filledItemFrames.add(frame);
            }
        }

        for (ItemFrameEntity emptyFrame : emptyItemFrames) {
            if (placements >= maxPlacements)
                break;

            if (!dupeItemList.contains(inv.getMainHandStack().getItem())) {
                if (swaps < maxSwaps) {
                    attemptSwapToItem(inv, dupeItemList);
                }

                boolean swapped = (swaps > 0);
                if (!dupeItemList.contains(inv.getMainHandStack().getItem()) && moves < maxInventoryMoves && !swapped) {
                    int slot = findItemInInventory(inv, dupeItemList);
                    if (slot != -1) {
                        mc.interactionManager.pickFromInventory(slot);
                        moves++;
                    }
                }
            }

            if (dupeItemList.contains(inv.getMainHandStack().getItem())) {
                interactItemFrame(emptyFrame);
                dontHit.remove(emptyFrame);
                placements++;
            }
        }

        for (ItemFrameEntity filledFrame : filledItemFrames) {
            if (!dontHit.contains(filledFrame)) {
                mc.interactionManager.attackEntity(mc.player, filledFrame);
                dupeCount++;
                dontHit.add(filledFrame);
            }
        }
    }

    private static void handleSpeedMode(List<ItemFrameEntity> allDetectedFrames, List<Item> dupeItemList) {
        List<ItemFrameEntity> presentTargets = new ArrayList<>();
        List<FrameSnapshot> missingTargets = new ArrayList<>();

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;

        for (FrameSnapshot target : targetFrames) {
            if (target.pos.squaredDistanceTo(mc.player.getEyePos()) > rangeSq) {
                continue;
            }

            ItemFrameEntity found = null;
            for (ItemFrameEntity detected : allDetectedFrames) {
                if (detected.getHorizontalFacing() == target.facing &&
                        detected.getPos().squaredDistanceTo(target.pos) < 0.01) {
                    found = detected;
                    break;
                }
            }
            if (found != null) {
                presentTargets.add(found);
            } else {
                missingTargets.add(target);
            }
        }

        if (Config.INSTANCE.replaceItemFrames) {
            replaceMissingFrames(missingTargets);
        }

        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;

        for (ItemFrameEntity frame : presentTargets) {
            if (placements >= maxPlacements)
                break;

            boolean isAir = frame.getHeldItemStack().getItem() == Items.AIR;
            boolean isDupeItem = dupeItemList.contains(frame.getHeldItemStack().getItem());

            if (isAir) {
                checkAndFillFrame(frame, inv, dupeItemList, maxSwaps);
            } else if (!dontHit.contains(frame) && isDupeItem) {
                dontHit.add(frame);
                mc.interactionManager.attackEntity(mc.player, frame);
                dupeCount++;

                checkAndFillFrame(frame, inv, dupeItemList, maxSwaps);
            }
        }
    }

    private static void replaceMissingFrames(List<FrameSnapshot> missingTargets) {
        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;

        for (FrameSnapshot missing : missingTargets) {
            if (placements >= maxPlacements)
                break;

            if (inv.getStack(PlayerInventory.OFF_HAND_SLOT).getItem() instanceof ItemFrameItem) {
                placeFrame(Hand.OFF_HAND, missing);
            } else {
                if (!(inv.getMainHandStack().getItem() instanceof ItemFrameItem) && swaps < maxSwaps) {
                    attemptSwapToItem(inv, ItemFrameItem.class);
                }
                if (inv.getMainHandStack().getItem() instanceof ItemFrameItem) {
                    placeFrame(Hand.MAIN_HAND, missing);
                }
            }
        }
    }

    private static void placeFrame(Hand hand, FrameSnapshot snapshot) {
        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(
                Vec3d.ofCenter(snapshot.supportPos), snapshot.facing, snapshot.supportPos, false));
        placements++;
    }

    private static void checkAndFillFrame(ItemFrameEntity frame, PlayerInventory inv, List<Item> dupeItemList,
            int maxSwapsLimit) {
        if (!dupeItemList.contains(inv.getMainHandStack().getItem())) {
            if (swaps < maxSwapsLimit) {
                attemptSwapToItem(inv, dupeItemList);
            }

            boolean swapped = (swaps > 0);
            if (!dupeItemList.contains(inv.getMainHandStack().getItem()) && moves < Config.INSTANCE.maxInventoryMoves
                    && !swapped) {
                int slot = findItemInInventory(inv, dupeItemList);
                if (slot != -1) {
                    mc.interactionManager.pickFromInventory(slot);
                    moves++;
                }
            }
        }
        if (dupeItemList.contains(inv.getMainHandStack().getItem())) {
            interactItemFrame(frame);
            dontHit.remove(frame);
            placements++;
        }
    }

    private static void interactItemFrame(ItemFrameEntity itemFrame) {
        mc.interactionManager.interactEntity(mc.player, itemFrame, Hand.MAIN_HAND);
    }

    private static void attemptSwapToItem(PlayerInventory inv, List<Item> items) {
        int slot = findItemInHotbar(inv, items);
        if (slot != -1) {
            swap(inv, slot);
            swaps++;
        }
    }

    private static void attemptSwapToItem(PlayerInventory inv, Class<?> clazz) {
        int slot = findItemInHotbar(inv, clazz);
        if (slot != -1) {
            swap(inv, slot);
            swaps++;
        }
    }

    private static int findItemInHotbar(PlayerInventory inv, List<Item> items) {
        for (int i = 0; i < 9; i++) {
            if (items.contains(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInHotbar(PlayerInventory inv, Class<?> clazz) {
        for (int i = 0; i < 9; i++) {
            if (clazz.isInstance(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInInventory(PlayerInventory inv, List<Item> items) {
        for (int i = 9; i < inv.size(); i++) {
            if (items.contains(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static void swap(PlayerInventory inv, int slot) {
        inv.selectedSlot = slot;
    }
}
