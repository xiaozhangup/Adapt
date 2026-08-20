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

package com.volmit.adapt.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class Localizer {
    private static JsonObject primaryLanguage;
    private static JsonObject fallbackLanguage;

    @SneakyThrows
    public static void updateLanguageFile() {
        File langFolder = new File(Adapt.instance.getDataFolder(), "languages");
        Files.createDirectories(langFolder.toPath());

        if (AdaptConfig.get().isAutoUpdateLanguage()) {
            Adapt.verbose("Attempting to update Language File");
            Adapt.verbose("Updating Primary Language File: " + AdaptConfig.get().getLanguage());
            copyBundledLanguage(AdaptConfig.get().getLanguage(), langFolder);
            Adapt.verbose("Loaded Primary Language: " + AdaptConfig.get().getLanguage());

            if (!Objects.equals(AdaptConfig.get().getLanguage(),
                    AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing())) {
                Adapt.verbose("Updating Fallback Language File: "
                        + AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing());
                copyBundledLanguage(AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing(),
                        langFolder);
                Adapt.verbose("Loaded Fallback: "
                        + AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing());
            }
        } else {
            Adapt.error("Auto Update Language is disabled, Expect Errors.");
            Adapt.error("Do not disable this unless you know what you are doing, and dont expect support.");
        }

        primaryLanguage = readLanguage(langFolder, AdaptConfig.get().getLanguage());
        fallbackLanguage = Objects.equals(AdaptConfig.get().getLanguage(),
                AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing())
                        ? primaryLanguage
                        : readLanguage(langFolder,
                                AdaptConfig.get().getFallbackLanguageDontChangeUnlessYouKnowWhatYouAreDoing());
        Adapt.wordKey.clear();
    }

    @SneakyThrows
    public static String raw(String s1, String s2, String s3) {
        String cacheKey = s1 + '\0' + s2 + '\0' + s3;
        if (!Adapt.wordKey.containsKey(cacheKey)) {
            if (primaryLanguage == null) {
                updateLanguageFile();
            }

            String key = s1 + "." + s2 + "." + s3;
            String value = find(primaryLanguage, s1, s2, s3);
            if (value == null) {
                Adapt.warn("Your Language File is missing the following key: " + key);
                value = find(fallbackLanguage, s1, s2, s3);
            }
            if (value == null) {
                value = key;
                Adapt.error("Your Fallback Language File is missing the following key: " + key);
                Adapt.error("Please report this to the developer!");
            }
            Adapt.wordKey.put(cacheKey, value);
        }

        return Adapt.wordKey.get(cacheKey);
    }

    public static Component component(String s1, String s2, String s3, TagResolver... resolvers) {
        return configured(raw(s1, s2, s3), resolvers);
    }

    public static Component configured(String input, TagResolver... resolvers) {
        return Components.mini(input, resolvers);
    }

    private static void copyBundledLanguage(String language, File folder) throws IOException {
        try (InputStream input = Objects.requireNonNull(Adapt.instance.getResource(language + ".json"),
                "Missing bundled language " + language)) {
            Files.copy(input, new File(folder, language + ".json").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static JsonObject readLanguage(File folder, String language) throws IOException {
        JsonElement json = JsonParser.parseString(Files.readString(new File(folder, language + ".json").toPath()));
        return json.getAsJsonObject();
    }

    private static String find(JsonObject root, String first, String second, String third) {
        if (root == null || !root.has(first) || !root.get(first).isJsonObject()) {
            return null;
        }
        JsonObject firstObject = root.getAsJsonObject(first);
        if (!firstObject.has(second) || !firstObject.get(second).isJsonObject()) {
            return null;
        }
        JsonObject secondObject = firstObject.getAsJsonObject(second);
        if (!secondObject.has(third) || !secondObject.get(third).isJsonPrimitive()
                || !secondObject.get(third).getAsJsonPrimitive().isString()) {
            return null;
        }
        return secondObject.get(third).getAsString();
    }
}
