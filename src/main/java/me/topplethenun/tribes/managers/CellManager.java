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
package me.topplethenun.tribes.managers;

import me.topplethenun.tribes.data.Cell;
import me.topplethenun.tribes.math.Vec2;
import org.nunnerycode.kern.shade.google.common.base.Optional;
import org.nunnerycode.kern.shade.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CellManager {

    private final Map<Vec2, Cell> cellMap;

    public CellManager() {
        this.cellMap = new HashMap<>();
    }

    public Optional<Cell> getCell(Vec2 vec2) {
        Preconditions.checkNotNull(vec2, "vec2 cannot be null");
        Optional<Cell> optional = Optional.absent();
        if (!cellMap.containsKey(vec2)) {
            return optional;
        }
        return Optional.of(cellMap.get(vec2));
    }

    public void placeCell(Vec2 vec2, Cell cell) {
        Preconditions.checkNotNull(vec2, "vec2 cannot be null");
        Preconditions.checkNotNull(cell, "cell cannot be null");
        cellMap.put(vec2, cell);
    }

    public Set<Cell> getCells() {
        return new HashSet<>(cellMap.values());
    }

}