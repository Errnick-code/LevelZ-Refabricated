package dev.errnicraft.levelz_refabricated.init;

import com.mojang.blaze3d.platform.InputConstants;
import dev.errnicraft.levelz_refabricated.screen.PlayerLevelSkillsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class KeyInit {
    public static KeyMapping screenKey = new KeyMapping("key.levelz.openskillscreen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, KeyMapping.Category.MISC);

    public static void init() {
        // Registering
        KeyBindingHelper.registerKeyBinding(screenKey);
        // Callback
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (screenKey.consumeClick()) {
                client.setScreen(new PlayerLevelSkillsScreen());
                return;
            }
        });
    }

    public static void writeId(String string) {
        try (FileWriter idFile = new FileWriter("idlist.json", true)) {
            idFile.append("\"" + string + "\",");
            idFile.append(System.lineSeparator());
        } catch (IOException ignored) {
        }
    }

}
