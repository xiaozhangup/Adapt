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

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Particle sender using the Bukkit particles API for 1.9+ servers
 *
 * @author MrMicky
 */
@SuppressWarnings("deprecation")
interface ParticleSender {

    void spawnParticle(Object receiver, ParticleType particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data);

    Object getParticle(ParticleType particle);

    boolean isValidData(Object particle, Object data);

    default double color(double color) {
        return color / 255.0;
    }

    class ParticleSenderImpl implements ParticleSender {

        @Override
        public void spawnParticle(Object receiver, ParticleType particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, Object data) {
            Particle bukkitParticle = Particle.valueOf(particle.toString());

            if (data instanceof Color color) {
                if (particle.getDataType() == Color.class) {
                    count = 0;
                    offsetX = color(color.getRed());
                    offsetY = color(color.getGreen());
                    offsetZ = color(color.getBlue());
                    extra = 1.0;
                }
                data = null;
            }

            if (receiver instanceof World) {
                ((World) receiver).spawnParticle(bukkitParticle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
            } else if (receiver instanceof Player) {
                ((Player) receiver).spawnParticle(bukkitParticle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
            }
        }

        @Override
        public Particle getParticle(ParticleType particle) {
            try {
                return Particle.valueOf(particle.toString());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @Override
        public boolean isValidData(Object particle, Object data) {
            return isValidDataBukkit((Particle) particle, data);
        }

        public boolean isValidDataBukkit(Particle particle, Object data) {
            return particle.getDataType() == Void.class || particle.getDataType().isInstance(data);
        }
    }
}
