/*
 * This file is part of Tribes, licensed under the ISC License.
 *
 * Copyright (c) 2015 Richard Harrah
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted,
 * provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */
package com.tealcube.minecraft.bukkit.tribes.managers;

import com.tealcube.minecraft.bukkit.kern.shade.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PvpManager {

    private final Map<UUID, PvpData> tagData;

    public PvpManager() {
        tagData = new HashMap<>();
    }

    public PvpData getData(UUID uuid) {
        Preconditions.checkNotNull(uuid);
        return tagData.containsKey(uuid) ? tagData.get(uuid) : new PvpData(0, null);
    }

    public void setData(UUID uuid, PvpData data) {
        Preconditions.checkNotNull(uuid);
        tagData.put(uuid, data);
    }

    public void clearTime(UUID uuid) {
        Preconditions.checkNotNull(uuid);
        tagData.remove(uuid);
    }

    public class PvpData {
        private final long time;
        private final UUID tagger;

        private PvpData(long time, UUID tagger) {
            this.time = time;
            this.tagger = tagger;
        }

        public long time() {
            return time;
        }

        public UUID tagger() {
            return tagger;
        }

        public PvpData withTime(long time) {
            return new PvpData(time, tagger);
        }

        public PvpData withTagger(UUID tagger) {
            return new PvpData(time, tagger);
        }
    }

}
