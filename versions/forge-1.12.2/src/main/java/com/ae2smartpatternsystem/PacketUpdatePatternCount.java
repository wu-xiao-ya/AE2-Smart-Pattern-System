package com.ae2smartpatternsystem;


import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.List;

/**
 */
public class PacketUpdatePatternCount implements IMessage {
    
    private boolean isInput;
    private int count;
    

    public PacketUpdatePatternCount() {}
    
    public PacketUpdatePatternCount(boolean isInput, int count) {
        this.isInput = isInput;
        this.count = count;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        this.isInput = buf.readBoolean();
        this.count = buf.readInt();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.isInput);
        buf.writeInt(this.count);
    }
    
    public static class Handler implements IMessageHandler<PacketUpdatePatternCount, IMessage> {
        @Override
        public IMessage onMessage(PacketUpdatePatternCount message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            

            player.getServerWorld().addScheduledTask(() -> {

                ItemStack patternStack = player.getHeldItemMainhand();
                if (patternStack.isEmpty() || !(patternStack.getItem() instanceof ItemTest)) {
                    patternStack = player.getHeldItemOffhand();
                }
                
                if (!patternStack.isEmpty() && patternStack.getItem() instanceof ItemTest) {
                    ItemTest patternItem = (ItemTest) patternStack.getItem();

                    List<String> inputOres = ItemTest.getInputOreNamesStatic(patternStack);
                    List<Integer> inputCounts = ItemTest.getInputCountsStatic(patternStack);
                    List<String> outputOres = ItemTest.getOutputOreNamesStatic(patternStack);
                    List<Integer> outputCounts = ItemTest.getOutputCountsStatic(patternStack);
                    String displayName = patternItem.getEncodedItemName(patternStack);

                    if (message.isInput) {
                        setFirstCount(inputCounts, message.count);
                    } else {
                        setFirstCount(outputCounts, message.count);
                    }

                    if (!inputOres.isEmpty() && !outputOres.isEmpty()) {
                        patternItem.setEncodedItemWithFluidsAndGases(
                            patternStack,
                            inputOres,
                            inputCounts,
                            outputOres,
                            outputCounts,
                            ItemTest.getInputFluidsStatic(patternStack),
                            ItemTest.getInputFluidAmountsStatic(patternStack),
                            ItemTest.getOutputFluidsStatic(patternStack),
                            ItemTest.getOutputFluidAmountsStatic(patternStack),
                            ItemTest.getInputGasesStatic(patternStack),
                            ItemTest.getInputGasAmountsStatic(patternStack),
                            ItemTest.getOutputGasesStatic(patternStack),
                            ItemTest.getOutputGasAmountsStatic(patternStack),
                            ItemTest.getInputGasItemsStatic(patternStack),
                            ItemTest.getOutputGasItemsStatic(patternStack),
                            displayName
                        );
                    }
                }
            });
            
            return null;
        }

        private static void setFirstCount(List<Integer> counts, int count) {
            int normalized = Math.max(1, count);
            if (counts.isEmpty()) {
                counts.add(normalized);
            } else {
                counts.set(0, normalized);
            }
        }
    }
}
