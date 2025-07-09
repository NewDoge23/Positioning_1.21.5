package com.newdoge.positioning;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;

public class PositioningClient implements ClientModInitializer {
    public static int lastSecondsLeft = 0;

    @Override
    public void onInitializeClient() {
        // Handler de la barra
        ClientPlayNetworking.registerGlobalReceiver(
                DangerZonePayload.ID,
                (payload, context) -> {
                    lastSecondsLeft = payload.secondsLeft();
                }
        );

        HudRenderCallback.EVENT.register((DrawContext drawContext, RenderTickCounter tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            int secondsLeft = lastSecondsLeft;
            if (secondsLeft <= 0) return;

            Window window = client.getWindow();
            int width = window.getScaledWidth();
            int barWidth = 240;
            int barHeight = 14;
            int barX = (width - barWidth) / 2;
            int barY = 20;

            drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);
            float percent = Math.min(1.0f, Math.max(0.0f, secondsLeft / 60.0f));
            int progressWidth = (int) (barWidth * percent);
            drawContext.fill(barX, barY, barX + progressWidth, barY + barHeight, 0x80FF4444);

            String texto = "¡Tiempo para salir de la zona: " + secondsLeft + "s!";
            int textX = width / 2 - client.textRenderer.getWidth(texto) / 2;
            drawContext.drawText(client.textRenderer, texto, textX, barY - 12, 0xFFFFFF, true);

            if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 15 || (secondsLeft <= 10 && secondsLeft > 0)) {
                String bigNum = String.valueOf(secondsLeft);
                int scale = 4;
                int numWidth = client.textRenderer.getWidth(bigNum) * scale;
                int numHeight = client.textRenderer.fontHeight * scale;
                int centerX = width / 2 - numWidth / 2;
                int centerY = window.getScaledHeight() / 2 - numHeight / 2;

                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(centerX, centerY, 0);
                drawContext.getMatrices().scale(scale, scale, 1.0f);
                drawContext.drawText(client.textRenderer, bigNum, 0, 0, 0xFF4444, false);
                drawContext.getMatrices().pop();
            }
        });

        // Handler del pop-up de selección de grupo
        ClientPlayNetworking.registerGlobalReceiver(
                RequestGroupSelectionPayload.ID,
                (payload, context) -> {
                    context.client().execute(() -> {
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client.player == null) return;
                        client.setScreen(new ConfirmScreen(
                                (choice) -> {
                                    int group = choice ? 1 : 2;
                                    ClientPlayNetworking.send(new SubmitGroupSelectionPayload(group));
                                    client.setScreen(null);
                                },
                                Text.literal("¿A qué grupo querés unirte?"),
                                Text.literal("Norte (Z+) / Sur (Z-)"),
                                Text.literal("Norte (Z+)"),
                                Text.literal("Sur (Z-)")
                        ));
                    });
                }
        );
    }
}
