package bikerboys.pivot.networking.packets;

import static bikerboys.pivot.Pivot.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NewBlockDisplayEntity(String string) implements CustomPacketPayload {
    public static final ResourceLocation UPDATE_BLOCK_DISPLAY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "new_block_display");
    public static final CustomPacketPayload.Type<NewBlockDisplayEntity> ID = new CustomPacketPayload.Type<>(NewBlockDisplayEntity.UPDATE_BLOCK_DISPLAY_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, NewBlockDisplayEntity> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, NewBlockDisplayEntity::string, NewBlockDisplayEntity::new);


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
