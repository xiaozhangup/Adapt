/*------------------------------------------------------------------------------
-   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
-   Copyright (c) 2022 Arcane Arts (Volmit Software)
-
-   This program is free software: you can redistribute it and/or modify
-   it under the terms of the GNU General Public License as published by
-   the Free Software Foundation, either version 3 of the License, or
-   (at your option) any later version.
-
-   This program is distributed in the hope that it will be useful,
-   but WITHOUT ANY WARRANTY; without even the implied warranty of
-   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-   GNU General Public License for more details.
-
-   You should have received a copy of the GNU General Public License
-   along with this program.  If not, see <https://www.gnu.org/licenses/>.
-----------------------------------------------------------------------------*/

package com.volmit.adapt;

import com.jeff_media.customblockdata.CustomBlockData;
import com.volmit.adapt.api.advancement.AdvancementManager;
import com.volmit.adapt.api.Component;
import com.volmit.adapt.api.data.WorldData;
import com.volmit.adapt.api.potion.BrewingManager;
import com.volmit.adapt.api.protection.ProtectorRegistry;
import com.volmit.adapt.api.tick.Ticker;
import com.volmit.adapt.api.value.MaterialValue;
import com.volmit.adapt.api.world.AdaptServer;
import com.volmit.adapt.content.protector.OrangDomainProtector;
import com.volmit.adapt.content.protector.SlimeCargoProtector;
import com.volmit.adapt.content.protector.WorldProtector;
import com.volmit.adapt.util.*;
import com.volmit.adapt.util.collection.KList;
import com.volmit.adapt.util.collection.KMap;
import de.slikey.effectlib.EffectManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.volmit.adapt.util.decree.context.AdaptationListingHandler.initializeAdaptationListings;

public class Adapt extends VolmitPlugin {
    public static Adapt instance;
    public static final Map<String, String> wordKey = new ConcurrentHashMap<>();
    private static VolmitSender sender;
    public final EffectManager adaptEffectManager = new EffectManager(this);
    @Getter
    private final Map<String, Window> guiLeftovers = new HashMap<>();
    private KMap<Class<? extends AdaptService>, AdaptService> services;
    @Getter
    private Ticker ticker;
    @Getter
    private AdaptServer adaptServer;
    @Getter
    private SQLManager sqlManager;
    @Getter
    private ProtectorRegistry protectorRegistry;
    @Getter
    private AdvancementManager manager;
    private PapiExpansion papiExpansion;

    public Adapt() {
        super();
        instance = this;
    }

    @SuppressWarnings("unchecked")
    public static <T> T service(Class<T> c) {
        return (T) instance.services.get(c);
    }

    public static VolmitSender getSender() {
        if (sender == null) {
            sender = new VolmitSender(Bukkit.getConsoleSender());
            sender.setTag(instance.getTag());
        }
        return sender;
    }

    public static List<Object> initialize(String s) {
        return initialize(s, null);
    }

    public static KList<Object> initialize(String s, Class<? extends Annotation> slicedClass) {
        JarScanner js = new JarScanner(instance.getFile(), s);
        KList<Object> v = new KList<>();
        J.attempt(js::scan);
        for (Class<?> i : js.getClasses()) {
            if (slicedClass == null || i.isAnnotationPresent(slicedClass)) {
                try {
                    Adapt.verbose("Found class: " + i.getName());
                    v.add(i.getDeclaredConstructor().newInstance());
                } catch (Throwable e) {
                    Adapt.verbose("Failed to load class: " + i.getName());
                    e.printStackTrace();
                }
            }
        }

        return v;
    }

    public static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    public static void printInformation() {
        debug("XP Curve: " + AdaptConfig.get().getXpCurve());
        debug("XP/Level base: " + AdaptConfig.get().getPlayerXpPerSkillLevelUpBase());
        debug("XP/Level multiplier: " + AdaptConfig.get().getPlayerXpPerSkillLevelUpLevelMultiplier());
        info("Language: " + AdaptConfig.get().getLanguage() + " - Language Fallback: "
                + AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing());
    }

    @SneakyThrows
    public static void autoUpdateCheck() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new URL("https://raw.githubusercontent.com/VolmitSoftware/Adapt/main/build.gradle").openStream()))) {
            info("Checking for updates...");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.contains("version '")) {
                    String version = inputLine.replace("version '", "").replace("'", "")
                            .replace("// Needs to be version specific", "").replace(" ", "");
                    if (instance.getDescription().getVersion().contains("development")) {
                        info("Development build detected. Skipping update check.");
                        return;
                    } else if (!version.equals(instance.getDescription().getVersion())) {
                        info(MessageFormat.format(
                                "Please update your Adapt plugin to the latest version! (Current: {0} Latest: {1})",
                                instance.getDescription().getVersion(), version));
                    } else {
                        info("You are running the latest version of Adapt!");
                    }
                    break;
                }
            }
        } catch (Throwable e) {
            error("Failed to check for updates.");
        }
    }

    public static void actionbar(Player p, net.kyori.adventure.text.Component message) {
        p.sendActionBar(message);
    }

    public static void debug(String string) {
        if (AdaptConfig.get().isDebug()) {
            msg(Components.mini("<dark_purple><message>", Placeholder.unparsed("message", string)));
        }
    }

    public static void warn(String string) {
        msg(Components.mini("<yellow><message>", Placeholder.unparsed("message", string)));
    }

    public static void error(String string) {
        msg(Components.mini("<red><message>", Placeholder.unparsed("message", string)));
    }

    public static void verbose(String string) {
        if (AdaptConfig.get().isVerbose()) {
            msg(Components.mini("<light_purple><message>", Placeholder.unparsed("message", string)));
        }
    }

    public static void success(String string) {
        msg(Components.mini("<green><message>", Placeholder.unparsed("message", string)));
    }

    public static void info(String string) {
        msg(Components.mini("<white><message>", Placeholder.unparsed("message", string)));
    }

    public static void messagePlayer(Player p, net.kyori.adventure.text.Component message) {
        p.sendMessage(prefixed(message));
    }

    public static void msg(net.kyori.adventure.text.Component message) {
        try {
            if (instance == null) {
                System.out.println("[Adapt]: " + Components.plain(message));
                return;
            }

            Bukkit.getConsoleSender().sendMessage(prefixed(message));
        } catch (Throwable e) {
            System.out.println("[Adapt]: " + Components.plain(message));
        }
    }

    private static net.kyori.adventure.text.Component prefixed(net.kyori.adventure.text.Component message) {
        return Components.mini("<dark_gray>[<#cddced>属性<dark_gray>] <message>",
                Placeholder.component("message", message));
    }

    @Override
    public void onLoad() {
        manager = new AdvancementManager();
    }

    @Override
    public void start() {
        services = new KMap<>();
        initialize("com.volmit.adapt.service")
                .forEach((i) -> services.put((Class<? extends AdaptService>) i.getClass(), (AdaptService) i));

        Localizer.updateLanguageFile();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            papiExpansion = new PapiExpansion();
            papiExpansion.register();
        }
        printInformation();
        sqlManager = new SQLManager();
        if (AdaptConfig.get().isUseSql()) {
            sqlManager.establishConnection();
        }
        startSim();
        CustomBlockData.registerListener(this);
        registerListener(new BrewingManager());
        // setupMetrics();
        // startupPrint(); // Splash screen
        if (AdaptConfig.get().isAutoUpdateCheck()) {
            // autoUpdateCheck();
        }
        protectorRegistry = new ProtectorRegistry();
        if (getServer().getPluginManager().getPlugin("SlimeCargoNext") != null) {
            protectorRegistry.registerProtector(new SlimeCargoProtector());
            info("Enabled SlimeCargoProtector!");
        }
        if (getServer().getPluginManager().getPlugin("OrangDomain") != null) {
            protectorRegistry.registerProtector(new OrangDomainProtector());
            protectorRegistry.registerProtector(new WorldProtector());
            info("Enabled OrangDomainProtector!");
        }
        initializeAdaptationListings();
        services.values().forEach(AdaptService::onEnable);
        services.values().forEach(this::registerListener);
    }

    public void startSim() {
        ticker = new Ticker();
        adaptServer = new AdaptServer();
        manager.enable();
    }

    public void stopSim() {
        stopSafely("ticker", () -> {
            if (ticker != null) {
                ticker.clear();
            }
        });
        stopSafely("block XP", () -> Component.flushBlockXP(30, TimeUnit.SECONDS));
        stopSafely("players and skills", () -> {
            if (adaptServer != null) {
                adaptServer.unregister();
            }
        });
        stopSafely("advancements", () -> {
            if (manager != null) {
                manager.disable();
            }
        });
        stopSafely("brewing", BrewingManager::clear);
        stopSafely("material values", MaterialValue::save);
        stopSafely("world data", WorldData::stop);
        stopSafely("custom models", CustomModel::clear);
    }

    @Override
    public void stop() {
        stopSafely("PlaceholderAPI", () -> {
            if (papiExpansion != null) {
                papiExpansion.unregister();
                papiExpansion = null;
            }
        });
        stopSafely("services", () -> {
            if (services != null) {
                services.values().forEach(service ->
                        stopSafely(service.getClass().getSimpleName(), service::onDisable));
            }
        });
        stopSafely("GUI", () -> {
            List.copyOf(guiLeftovers.values()).forEach(Window::close);
            guiLeftovers.clear();
        });
        stopSafely("simulation", this::stopSim);
        stopSafely("SQL", () -> {
            if (sqlManager != null) {
                sqlManager.closeConnection();
            }
        });
        stopSafely("EffectLib", adaptEffectManager::dispose);
        stopSafely("protectors", () -> {
            if (protectorRegistry != null) {
                protectorRegistry.unregisterAll();
            }
        });
        stopSafely("service registry", () -> {
            if (services != null) {
                services.clear();
            }
        });
        stopSafely("async executor", MultiBurst::shutdownAll);
    }

    private void stopSafely(String component, Runnable action) {
        try {
            action.run();
        } catch (Throwable e) {
            error("Failed to stop " + component + ".");
            e.printStackTrace();
        }
    }

    public File getJarFile() {
        return getFile();
    }

    @Override
    public net.kyori.adventure.text.Component getTag(String subTag) {
        return Components.mini("<dark_gray>[<dark_red>Adapt<dark_gray>]<reset><gray>: ")
                .colorIfAbsent(NamedTextColor.GRAY);
    }
}
