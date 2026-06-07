package dev.kral1999.dupe;

import dev.kral1999.dupe.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFrameItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DuperManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final List<FrameSnapshot> targetFrames = new ArrayList<>();
    private static final List<ItemFrameEntity> dontHit = new ArrayList<>();

    private static final long DPS_UPDATE_INTERVAL_MS = 1000;
    private static final long CLEANUP_INTERVAL_MS = 5000;
    private static final long COOLDOWN_EXPIRY_MS = 10000;

    private static final Map<Integer, Long> interactionCooldowns = new ConcurrentHashMap<>();

    private static int currentDps = 0;
    private static long lastDpsUpdate = 0;
    private static int accumulatedDupes = 0;

    public static int getDps() {
        return currentDps;
    }

    private record FrameSnapshot(Vec3d pos, BlockPos supportPos, net.minecraft.util.math.Direction facing) {
    }

    private static int placements = 0;
    private static int swaps = 0;
    private static int moves = 0;
    private static long lastCleanup = 0;
    private static int tickCounter = 0;

    public static void onDupeKeyPressed() {
        Config.INSTANCE.enabled = !Config.INSTANCE.enabled;
        Config.save();
        if (Config.INSTANCE.enabled) {
            reloadFrames();
        } else {
            targetFrames.clear();
            dontHit.clear();
            interactionCooldowns.clear();
        }

        if (mc.player != null) {
            boolean enabled = Config.INSTANCE.enabled;
            mc.player.sendMessage(Text.translatable(enabled ? "dupe.command.toggle.on" : "dupe.command.toggle.off")
                    .formatted(enabled ? Formatting.GREEN : Formatting.RED));
        }
    }

    public static void reloadFrames() {
        targetFrames.clear();
        dontHit.clear();
        if (mc.player == null || mc.world == null)
            return;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;
        List<ItemFrameEntity> detected = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (itemFrame.isRemoved() || !itemFrame.isAlive())
                    continue;
                if (entity.squaredDistanceTo(mc.player) <= rangeSq) {
                    detected.add(itemFrame);
                }
            }
        }

        detected.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = Config.INSTANCE.mode == Config.Mode.Normal ? 3 : Config.INSTANCE.maxFrames;
        if (detected.size() > limit) {
            detected = detected.subList(0, limit);
        }

        for (ItemFrameEntity frame : detected) {
            Vec3d rotationVec = frame.getRotationVector();
            if (rotationVec == null)
                continue;
            BlockPos supportPos = Utils.Vec3d2BlockPos(frame.getPos().subtract(rotationVec.normalize()));
            targetFrames.add(new FrameSnapshot(frame.getPos(), supportPos, frame.getHorizontalFacing()));
        }
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (now - lastDpsUpdate >= DPS_UPDATE_INTERVAL_MS) {
            currentDps = accumulatedDupes / 2;
            accumulatedDupes = 0;
            lastDpsUpdate = now;
        }

        if (!Config.INSTANCE.enabled || mc.player == null || mc.world == null || mc.interactionManager == null)
            return;

        if (Config.INSTANCE.tickDelay > 0) {
            tickCounter++;
            if (tickCounter <= Config.INSTANCE.tickDelay) {
                return;
            }
            tickCounter = 0;
        }

        if (!Config.INSTANCE.multitask) {
            if (mc.player.isUsingItem() || mc.options.attackKey.isPressed() || mc.options.useKey.isPressed()
                    || mc.currentScreen != null) {
                return;
            }
        }

        if (now - lastCleanup >= CLEANUP_INTERVAL_MS) {
            cleanupDeadEntities();
            lastCleanup = now;
        }

        placements = 0;
        swaps = 0;
        moves = 0;

        double rangeSq = Config.INSTANCE.range * Config.INSTANCE.range;

        List<ItemFrameEntity> allDetectedFrames = new ArrayList<>();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ItemFrameEntity itemFrame) {
                if (entity.squaredDistanceTo(mc.player.getEyePos()) <= rangeSq) {
                    allDetectedFrames.add(itemFrame);
                }
            }
        }

        if (Config.INSTANCE.mode == Config.Mode.Normal) {
            handleNormalMode(allDetectedFrames, rangeSq);
        } else if (Config.INSTANCE.mode == Config.Mode.Speed) {
            handleSpeedMode(allDetectedFrames, rangeSq);
        } else if (Config.INSTANCE.mode == Config.Mode.Multi) {
            handleMultiMode(allDetectedFrames, rangeSq);
        }
    }

    private static void handleNormalMode(List<ItemFrameEntity> allDetectedFrames,
            double rangeSq) {
        PlayerInventory inv = mc.player.getInventory();
        int maxPlacements = Config.INSTANCE.maxPlacements;
        int maxSwaps = Config.INSTANCE.maxSwaps;

        List<ItemFrameEntity> activeFrames = new ArrayList<>(allDetectedFrames);
        activeFrames.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));

        int limit = 3;
        if (activeFrames.size() > limit) {
            activeFrames = activeFrames.subList(0, limit);
        }

        List<ItemFrameEntity> emptyItemFrames = new ArrayList<>();
        List<ItemFrameEntity> filledItemFrames = new ArrayList<>();

        for (ItemFrameEntity frame : activeFrames) {
            if (frame.isRemoved() || !frame.isAlive())
                continue;
            if (frame.squaredDistanceTo(mc.player.getEyePos()) > rangeSq)
                continue;

            if (frame.getHeldItemStack().isEmpty()) {
                emptyItemFrames.add(frame);
            } else if (Config.isItemAllowed(frame.getHeldItemStack().getItem())) {
                filledItemFrames.add(frame);
            }
        }

        for (ItemFrameEntity emptyFrame : emptyItemFrames) {
            if (placements >= maxPlacements)
                break;
            checkAndFillFrame(emptyFrame, inv, maxSwaps);
        }

        for (ItemFrameEntity filledFrame : filledItemFrames) {
            if (!dontHit.contains(filledFrame)) {
                if (Config.INSTANCE.checkStatus && filledFrame.getHeldItemStack().isEmpty())
                    continue;

                long currentTime = System.currentTimeMillis();
                if (shouldSkipDoubleClick(filledFrame, currentTime))
                    continue;

                attackItemFrame(filledFrame, currentTime);
            }
        }
    }

    private static void handleMultiMode(List<ItemFrameEntity> allDetectedFrames, double rangeSq) {
        PlayerInventory inv = mc.player.getInventory();
        int maxFrameLimit = Config.INSTANCE.maxFrames;

        List<ItemFrameEntity> activeFrames = new ArrayList<>(allDetectedFrames);
        activeFrames.sort((f1, f2) -> Float.compare(f1.distanceTo(mc.player), f2.distanceTo(mc.player)));
        if (activeFrames.size() > maxFrameLimit) {
            activeFrames = activeFrames.subList(0, maxFrameLimit);
        }

        prepareDupeItemInHand(inv, Config.INSTANCE.maxSwaps, Config.INSTANCE.maxInventoryMoves);

        boolean hasItem = Config.isItemAllowed(inv.getMainHandStack().getItem());
        long currentTime = System.currentTimeMillis();

        for (ItemFrameEntity frame : activeFrames) {
            if (frame.isRemoved() || !frame.isAlive()) continue;
            if (frame.squaredDistanceTo(mc.player.getEyePos()) > rangeSq) continue;

            if (!frame.getHeldItemStack().isEmpty() && Config.isItemAllowed(frame.getHeldItemStack().getItem())) {
                if (!dontHit.contains(frame)) {
                    if (shouldSkipDoubleClick(frame, currentTime))
                        continue;

                    attackItemFrame(frame, currentTime);
                }
            } else if (frame.getHeldItemStack().isEmpty() && hasItem) {
                interactItemFrame(frame);
                dontHit.remove(frame);
            }
        }
    }

    private static void handleSpeedMode(List<ItemFrameEntity> allDetectedFrames,
            double rangeSq) {
        List<ItemFrameEntity> presentTargets = new ArrayList<>();
        List<FrameSnapshot> missingTargets = new ArrayList<>();

        for (FrameSnapshot target : targetFrames) {
            if (target.pos.squaredDistanceTo(mc.player.getEyePos()) > rangeSq) {
                continue;
            }

            ItemFrameEntity found = null;
            for (ItemFrameEntity detected : allDetectedFrames) {
                if (detected.isRemoved() || !detected.isAlive())
                    continue;
                Vec3d rotationVec = detected.getRotationVector();
                if (rotationVec == null)
                    continue;
                BlockPos detectedSupportPos = Utils
                        .Vec3d2BlockPos(detected.getPos().subtract(rotationVec.normalize()));
                if (detected.getHorizontalFacing() == target.facing &&
                        detectedSupportPos.equals(target.supportPos)) {
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

            boolean isAir = frame.getHeldItemStack().isEmpty();
            boolean isDupeItem = Config.isItemAllowed(frame.getHeldItemStack().getItem());

            if (isAir) {
                checkAndFillFrame(frame, inv, maxSwaps);
            } else if (!dontHit.contains(frame) && isDupeItem) {
                long now = System.currentTimeMillis();
                attackItemFrame(frame, now);
                checkAndFillFrame(frame, inv, maxSwaps);
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
        Utils.rotate(Vec3d.ofCenter(snapshot.supportPos), Config.INSTANCE.rotationMode);
        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(
                Vec3d.ofCenter(snapshot.supportPos), snapshot.facing, snapshot.supportPos, false));
        placements++;
    }

    private static void checkAndFillFrame(ItemFrameEntity frame, PlayerInventory inv,
            int maxSwapsLimit) {
        prepareDupeItemInHand(inv, maxSwapsLimit, Config.INSTANCE.maxInventoryMoves);
        if (Config.isItemAllowed(inv.getMainHandStack().getItem())) {
            interactItemFrame(frame);
            dontHit.remove(frame);
            placements++;
        }
    }

    private static void interactItemFrame(ItemFrameEntity itemFrame) {
        long now = System.currentTimeMillis();
        if (Config.INSTANCE.mode != Config.Mode.Speed && shouldSkipDoubleClick(itemFrame, now))
            return;

        Utils.rotate(itemFrame.getPos(), Config.INSTANCE.rotationMode);
        mc.interactionManager.interactEntity(mc.player, itemFrame, Hand.MAIN_HAND);
        accumulatedDupes++;
        interactionCooldowns.put(itemFrame.getId(), now);
    }

    private static void prepareDupeItemInHand(PlayerInventory inv, int maxSwapsLimit, int maxInventoryMovesLimit) {
        if (Config.isItemAllowed(inv.getMainHandStack().getItem())) {
            return;
        }

        if (swaps < maxSwapsLimit) {
            attemptSwapToItem(inv);
        }

        boolean swapped = (swaps > 0);
        if (!Config.isItemAllowed(inv.getMainHandStack().getItem()) && moves < maxInventoryMovesLimit && !swapped) {
            int slot = findItemInInventory(inv);
            if (slot != -1) {
                moveItemToHotbar(inv, slot);
            }
        }
    }

    private static boolean shouldSkipDoubleClick(ItemFrameEntity frame, long currentTime) {
        return Config.INSTANCE.antiDoubleClick &&
                currentTime - interactionCooldowns.getOrDefault(frame.getId(), 0L) < Config.INSTANCE.doubleClickDelay;
    }

    private static void attackItemFrame(ItemFrameEntity frame, long currentTime) {
        dontHit.add(frame);
        Utils.rotate(frame.getPos(), Config.INSTANCE.rotationMode);
        mc.interactionManager.attackEntity(mc.player, frame);
        accumulatedDupes++;
        interactionCooldowns.put(frame.getId(), currentTime);
    }

    private static void attemptSwapToItem(PlayerInventory inv) {
        int slot = findItemInHotbar(inv);
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

    private static int findItemInHotbar(PlayerInventory inv) {
        int currentSlot = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot)
                continue;
            if (Config.isItemAllowed(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInHotbar(PlayerInventory inv, Class<?> clazz) {
        int currentSlot = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (i == currentSlot)
                continue;
            if (clazz.isInstance(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static int findItemInInventory(PlayerInventory inv) {
        for (int i = 9; i < inv.size(); i++) {
            if (Config.isItemAllowed(inv.getStack(i).getItem()))
                return i;
        }
        return -1;
    }

    private static void moveItemToHotbar(PlayerInventory inv, int invSlot) {
        if (!Config.INSTANCE.invPick) {
            mc.interactionManager.pickFromInventory(invSlot);
            moves++;
            return;
        }

        int targetHotbar = inv.selectedSlot;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isEmpty()) {
                targetHotbar = i;
                break;
            }
        }

        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                invSlot,
                targetHotbar,
                SlotActionType.SWAP,
                mc.player);

        inv.selectedSlot = targetHotbar;
        moves++;
    }

    private static void swap(PlayerInventory inv, int slot) {
        inv.selectedSlot = slot;
    }

    private static void cleanupDeadEntities() {

        Iterator<ItemFrameEntity> iterator = dontHit.iterator();
        while (iterator.hasNext()) {
            ItemFrameEntity frame = iterator.next();
            if (frame.isRemoved() || !frame.isAlive()) {
                iterator.remove();
            }
        }

        long now = System.currentTimeMillis();
        interactionCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > COOLDOWN_EXPIRY_MS);
    }
}
