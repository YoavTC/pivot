package bikerboys.pivot.networking.packets;

import bikerboys.pivot.Pivot;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static bikerboys.pivot.Pivot.MOD_ID;

public record UpdateBlockDisplayPacketC2S(String uuid, @Nullable Vector3f translation, @Nullable Quaternionf right_rotation, @Nullable Vector3f scale) implements CustomPacketPayload {
    public static final ResourceLocation UPDATE_BLOCK_DISPLAY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "update_block_display");
    public static final CustomPacketPayload.Type<UpdateBlockDisplayPacketC2S> ID = new CustomPacketPayload.Type<>(UpdateBlockDisplayPacketC2S.UPDATE_BLOCK_DISPLAY_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateBlockDisplayPacketC2S> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UpdateBlockDisplayPacketC2S::uuid,
            ByteBufCodecs.optional(ByteBufCodecs.VECTOR3F), pkt -> Optional.ofNullable(pkt.translation()),
            ByteBufCodecs.optional(ByteBufCodecs.QUATERNIONF), pkt -> Optional.ofNullable(pkt.right_rotation()),
            ByteBufCodecs.optional(ByteBufCodecs.VECTOR3F), pkt -> Optional.ofNullable(pkt.scale()),
            (uuid, optPos, optRot, optScale) -> new UpdateBlockDisplayPacketC2S(
                    uuid,
                    optPos.orElse(null),
                    optRot.orElse(null),
                    optScale.orElse(null)
            )
    );


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
