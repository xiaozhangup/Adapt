package com.volmit.adapt.util.decree.handlers;

import com.volmit.adapt.util.collection.KList;
import com.volmit.adapt.util.decree.DecreeParameterHandler;
import com.volmit.adapt.util.decree.exceptions.DecreeParsingException;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

public class SoundHandler implements DecreeParameterHandler<Sound> {
    @Override
    public KList<Sound> getPossibilities() {
        return new KList<>(Registry.SOUND_EVENT.stream().toList());
    }

    @Override
    public String toString(Sound sound) {
        return Registry.SOUND_EVENT.getKeyOrThrow(sound).toString();
    }

    @Override
    public Sound parse(String in, boolean force) throws DecreeParsingException {
        String normalized = in.trim().toLowerCase(Locale.ROOT);
        NamespacedKey key = NamespacedKey.fromString(normalized);
        Sound sound = key == null ? null : Registry.SOUND_EVENT.get(key);
        if (sound == null) {
            String legacyName = in.trim().toUpperCase(Locale.ROOT);
            sound = Registry.SOUND_EVENT.stream()
                    .filter(candidate -> {
                        NamespacedKey candidateKey = Registry.SOUND_EVENT.getKey(candidate);
                        return candidateKey != null
                                && candidateKey.getNamespace().equals(NamespacedKey.MINECRAFT)
                                && candidateKey.getKey().replace('.', '_').toUpperCase(Locale.ROOT)
                                        .equals(legacyName);
                    })
                    .findFirst()
                    .orElse(null);
        }
        if (sound == null) {
            throw new DecreeParsingException("Invalid sound: " + in);
        }
        return sound;
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.equals(Sound.class);
    }
}
