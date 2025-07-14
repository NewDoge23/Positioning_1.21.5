package com.newdoge.positioning;

import com.newdoge.positioning.network.DangerZonePayload;
import com.newdoge.positioning.network.RequestGroupSelectionPayload;
import com.newdoge.positioning.network.SubmitGroupSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;

// import java.util.HashMap;
// import java.util.Map;
// import java.util.UUID;

public class PositioningClient implements ClientModInitializer {

    public static int lastSecondsLeft = 0;
    private static int lastNumberTitle = -1;
    private static boolean dangerSoundPlayed = false;

    // // FUTURO: para sync grupal, no usado ahora
    // public static final Map<UUID, Integer> CLIENT_PLAYER_GROUPS = new HashMap<>();

    @Override
    public void onInitializeClient() {
        // // Para cuando vuelva el sync grupal (tab)
        // ClientPlayNetworking.registerGlobalReceiver(
        //         SyncGroupsPayload.ID,
        //         (payload, context) -> {
        //             CLIENT_PLAYER_GROUPS.clear();
        //             CLIENT_PLAYER_GROUPS.putAll(payload.groups());
        //         }
        // );

        // Handler de la barra y peligro
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

            if (secondsLeft == 30 && !dangerSoundPlayed) {
                dangerSoundPlayed = true;
                client.player.playSound(
                        net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_SCREAM,
                        3.0F,
                        1.0F
                );
            } else if (secondsLeft != 30) {
                dangerSoundPlayed = false;
            }

            Window window = client.getWindow();
            int width = window.getScaledWidth();
            int barWidth = 240;
            int barHeight = 6;
            int barX = (width - barWidth) / 2;
            int barY = 22;

            // Fondo barra
            drawContext.fill(barX, barY, barX + barWidth, barY + barHeight, 0xA0000000);

            // Bordes suaves
            drawContext.fill(barX - 2, barY, barX, barY + barHeight, 0x80000000);
            drawContext.fill(barX + barWidth, barY, barX + barWidth + 2, barY + barHeight, 0x80000000);

            // Progreso
            float percent = Math.min(1.0f, Math.max(0.0f, secondsLeft / 30.0f));
            int progressWidth = (int) (barWidth * percent);
            drawContext.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xA0FF4444);

            // Bordes del progreso
            drawContext.fill(barX - 2, barY, barX, barY + barHeight, 0x80FF4444);
            if (progressWidth > 0)
                drawContext.fill(barX + progressWidth, barY, barX + progressWidth + 2, barY + barHeight, 0x80FF4444);

            // Texto arriba de la barra
            String texto = Text.translatable(("bar.seconds_left"), secondsLeft).getString();
            int textX = width / 2 - client.textRenderer.getWidth(texto) / 2;
            drawContext.drawText(client.textRenderer, texto, textX, barY - 12, 0xFFFFFF, true);

            // --- Título vanilla para countdown (últimos 10s) ---
            if (secondsLeft <= 10 && secondsLeft > 0) {
                if (secondsLeft != lastNumberTitle) {
                    lastNumberTitle = secondsLeft;
                    client.inGameHud.setTitle(
                            Text.literal(String.valueOf(secondsLeft)).styled(s -> s.withColor(0xFF4444))
                    );
                }
            } else if (lastNumberTitle != -1) {
                lastNumberTitle = -1;
                client.inGameHud.setTitle(Text.literal(""));
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
                                Text.translatable("menu.positioning.title"),
                                Text.translatable("menu.positioning.subtitle"),
                                Text.translatable("menu.positioning.op1"),
                                Text.translatable("menu.positioning.op2")
                        ));
                    });
                }
        );
    }
}
