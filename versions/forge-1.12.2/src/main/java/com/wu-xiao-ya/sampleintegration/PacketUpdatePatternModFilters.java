package com.lwx1145.sampleintegration;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

public class PacketUpdatePatternModFilters implements IMessage {

    private static final int OFFHAND_SLOT = -100;

    private int slot;
    private String[] excludedInputModIds;
    private String[] excludedOutputModIds;

    public PacketUpdatePatternModFilters() {
        this.slot = -1;
        this.excludedInputModIds = new String[0];
        this.excludedOutputModIds = new String[0];
    }

    public PacketUpdatePatternModFilters(int slot, String[] excludedInputModIds, String[] excludedOutputModIds) {
        this.slot = slot;
        this.excludedInputModIds = excludedInputModIds == null ? new String[0] : excludedInputModIds;
        this.excludedOutputModIds = excludedOutputModIds == null ? new String[0] : excludedOutputModIds;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.slot = buf.readInt();
        this.excludedInputModIds = readStringArray(buf);
        this.excludedOutputModIds = readStringArray(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.slot);
        writeStringArray(buf, this.excludedInputModIds);
        writeStringArray(buf, this.excludedOutputModIds);
    }

    private static void writeStringArray(ByteBuf buf, String[] values) {
        String[] source = values == null ? new String[0] : values;
        buf.writeInt(source.length);
        for (String value : source) {
            ByteBufUtils.writeUTF8String(buf, value == null ? "" : value);
        }
    }

    private static String[] readStringArray(ByteBuf buf) {
        int size = Math.max(0, buf.readInt());
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String value = ByteBufUtils.readUTF8String(buf);
            if (value != null && !value.isEmpty()) {
                values.add(value);
            }
        }
        return values.toArray(new String[0]);
    }

    public static class Handler implements IMessageHandler<PacketUpdatePatternModFilters, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdatePatternModFilters message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                ItemStack target = resolveTargetPatternStack(player, message.slot);
                if (target.isEmpty() || !(target.getItem() instanceof ItemTest)) {
                    return;
                }
                ItemTest.setExcludedModFiltersStatic(target, message.excludedInputModIds, message.excludedOutputModIds);
                player.inventory.markDirty();
            });
            return null;
        }

        private ItemStack resolveTargetPatternStack(EntityPlayerMP player, int slot) {
            if (player == null) {
                return ItemStack.EMPTY;
            }
            if (slot == OFFHAND_SLOT) {
                ItemStack offhand = player.getHeldItemOffhand();
                if (!offhand.isEmpty() && offhand.getItem() instanceof ItemTest) {
                    return offhand;
                }
            }
            if (slot >= 0 && slot < player.inventory.getSizeInventory()) {
                ItemStack stack = player.inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                    return stack;
                }
            }

            ItemStack main = player.getHeldItemMainhand();
            if (!main.isEmpty() && main.getItem() instanceof ItemTest) {
                return main;
            }

            ItemStack off = player.getHeldItemOffhand();
            if (!off.isEmpty() && off.getItem() instanceof ItemTest) {
                return off;
            }

            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemTest) {
                    return stack;
                }
            }
            return ItemStack.EMPTY;
        }
    }
}

