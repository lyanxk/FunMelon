package org.lyy.funmelon.client;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.lyy.funmelon.Funmelon;

@EventBusSubscriber(modid = Funmelon.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        net.minecraft.client.color.block.BlockColor color = (state, level, pos, tintIndex) -> tintIndex == 0
                        ? level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor()
                        : -1;
        event.register(color, Funmelon.MELON_VINE.get());
    }
}
