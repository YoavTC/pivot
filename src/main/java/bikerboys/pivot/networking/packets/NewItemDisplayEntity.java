package bikerboys.pivot.networking.packets;

import static bikerboys.pivot.Pivot.MOD_ID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NewItemDisplayEntity(String string) implements CustomPacketPayload {
    public static final ResourceLocation UPDATE_BLOCK_DISPLAY_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "new_item_display");
    public static final Type<NewItemDisplayEntity> ID = new Type<>(NewItemDisplayEntity.UPDATE_BLOCK_DISPLAY_ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, NewItemDisplayEntity> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, NewItemDisplayEntity::string, NewItemDisplayEntity::new);


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
