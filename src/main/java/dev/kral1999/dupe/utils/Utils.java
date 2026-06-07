package dev.kral1999.dupe.utils;

import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static java.lang.Math.floor;
import net.minecraft.client.MinecraftClient;

public class Utils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void rotate(Vec3d target, dev.kral1999.dupe.Config.RotationMode mode) {
        if (mode == dev.kral1999.dupe.Config.RotationMode.OFF || mc.player == null) return;

        Vec3d eyePos = mc.player.getEyePos();
        double diffX = target.x - eyePos.x;
        double diffY = target.y - eyePos.y;
        double diffZ = target.z - eyePos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

        yaw = mc.player.getYaw() + MathHelper.wrapDegrees(yaw - mc.player.getYaw());
        pitch = mc.player.getPitch() + MathHelper.wrapDegrees(pitch - mc.player.getPitch());
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);

        if (mode == dev.kral1999.dupe.Config.RotationMode.NORMAL) {
            mc.player.setYaw(yaw);
            mc.player.setPitch(pitch);
        } else if (mode == dev.kral1999.dupe.Config.RotationMode.SILENT) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
        }
    }

    public static void TPX(Vec3d pos, Vec3d startPos) {

        if (mc.player.isSneaking()) {
            mc.player.networkHandler
                    .sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }

        double distance = startPos.distanceTo(pos);

        int packetsRequired = (int) Math.ceil(Math.abs(distance / 10));
        for (int packetNumber = 0; packetNumber < (packetsRequired - 1); packetNumber++) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));
        }

        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true));
    }

    public static void TPX(Vec3d pos) {
        TPX(pos, mc.player.getPos());
    }

    public static BlockPos Vec3d2BlockPos(Vec3d pos) {
        return new BlockPos((int) floor(pos.x), (int) floor(pos.y), (int) floor(pos.z));
    }
}
