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
package me.topplethenun.tribes;

import me.topplethenun.tribes.data.Cell;
import me.topplethenun.tribes.data.Member;
import me.topplethenun.tribes.managers.CellManager;
import me.topplethenun.tribes.managers.MemberManager;
import me.topplethenun.tribes.storage.DataStorage;
import me.topplethenun.tribes.storage.MySQLDataStorage;
import org.nunnerycode.facecore.configuration.MasterConfiguration;
import org.nunnerycode.facecore.configuration.VersionedSmartConfiguration;
import org.nunnerycode.facecore.configuration.VersionedSmartYamlConfiguration;
import org.nunnerycode.facecore.logging.PluginLogger;
import org.nunnerycode.facecore.plugin.FacePlugin;

import java.io.File;

public class TribesPlugin extends FacePlugin {

    private static TribesPlugin INSTANCE;
    private DataStorage dataStorage;
    private CellManager cellManager;
    private MemberManager memberManager;
    private PluginLogger debugPrinter;
    private MasterConfiguration settings;

    @Override
    public void enable() {
        INSTANCE = this;
        debugPrinter = new PluginLogger(this);
        debug("Enabling v" + getDescription().getVersion());

        VersionedSmartYamlConfiguration dbYAML = new VersionedSmartYamlConfiguration(
                new File(getDataFolder(), "db.yml"), getResource("db.yml"),
                VersionedSmartConfiguration.VersionUpdateType.BACKUP_AND_UPDATE);
        if (dbYAML.update()) {
            debug("Updating db.yml");
        }

        settings = MasterConfiguration.loadFromFiles(dbYAML);

        dataStorage = new MySQLDataStorage(this);
        dataStorage.initialize();

        cellManager = new CellManager();
        memberManager = new MemberManager();

        for (Cell cell : dataStorage.loadCells()) {
            cellManager.placeCell(cell.getLocation(), cell);
        }
        for (Member member : dataStorage.loadMembers()) {
            memberManager.addMember(member);
        }
        debug("cells loaded: " + cellManager.getCells().size());
    }

    @Override
    public void disable() {
        dataStorage.saveCells(cellManager.getCells());
        dataStorage.saveMembers(memberManager.getMembers());
        dataStorage.shutdown();
    }

    public static TribesPlugin getInstance() {
        return INSTANCE;
    }

    public void debug(String... messages) {
        for (String message : messages) {
            debugPrinter.log(message);
        }
    }

    public MasterConfiguration getSettings() {
        return settings;
    }
}
