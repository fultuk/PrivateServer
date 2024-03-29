/*
 * MIT License
 *
 * Copyright (c) 2020 0utplay (Aldin Sijamhodzic)
 * Copyright (c) 2020 contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.tentact.privateserver.provider.command;

import de.tentact.languageapi.LanguageAPI;
import de.tentact.languageapi.player.LanguagePlayer;
import de.tentact.languageapi.player.PlayerExecutor;
import de.tentact.privateserver.PrivateServer;
import de.tentact.privateserver.i18n.I18N;
import de.tentact.privateserver.provider.config.NPCLocation;
import de.tentact.privateserver.provider.config.NPCSetting;
import de.tentact.privateserver.provider.config.PrivateServerConfig;
import de.tentact.privateserver.provider.util.SkinFetcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PrivateServerCommand implements CommandExecutor {

    private final PrivateServerConfig privateServerConfig;
    private final PrivateServer privateServer;
    private final PlayerExecutor playerExecutor = LanguageAPI.getInstance().getPlayerExecutor();

    public PrivateServerCommand(PrivateServer privateServer) {
        this.privateServer = privateServer;
        this.privateServerConfig = privateServer.getConfiguration().getPrivateServerConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        LanguagePlayer languagePlayer = this.playerExecutor.getLanguagePlayer(player.getUniqueId());
        if (languagePlayer == null) {
            return false;
        }
        if (!player.hasPermission(privateServerConfig.getBaseCommandPermission())) {
            languagePlayer.sendMessage(I18N.BASECOMMAND_PERMISSION.get());
            return false;
        }
        if (this.privateServer.getCurrentPrivateServiceUtil() != null) {
            languagePlayer.sendMessage(I18N.BASECOMMAND_ON_PSERVER.get());
            return false;
        }
        if (args.length < 2) {
            languagePlayer.sendMessage(I18N.COMMAND_HELP.get());
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "create":
                this.privateServer.getPrivateServerUtil().createPrivateServer(player, args[1]);
                break;
            case "npc": // pserver npc delete || pserver npc create imitadeplayer lookatPlayer NAME
                if (args[1].equalsIgnoreCase("delete")) {
                    this.privateServer.getPrivateServerUtil().removeNPC();
                    NPCSetting currentNPCSetting = this.privateServerConfig.getNPCSettings();
                    this.privateServerConfig.setNPCSettings(currentNPCSetting.setNPCLocation(null));
                    this.privateServer.getConfiguration().writeConfiguration(this.privateServerConfig);
                } else if (args[1].equalsIgnoreCase("create")) {
                    if (args.length < 6) {
                        languagePlayer.sendMessage(I18N.COMMAND_HELP.get());
                        return false;
                    }
                    if (this.isNoBoolean(args[2]) || this.isNoBoolean(args[3])) {
                        languagePlayer.sendMessage(I18N.COMMAND_HELP.get());
                        return false;
                    }
                    if (this.privateServer.getPrivateServerUtil().getNPCPool().getNPCs().size() != 0) {
                        this.privateServer.getPrivateServerUtil().removeNPC();
                    }
                    boolean imitatePlayer = Boolean.parseBoolean(args[2]);
                    boolean lookAtPlayer = Boolean.parseBoolean(args[3]);
                    String skinValue = SkinFetcher.fetchValue(args[4]);
                    String skinSignature = SkinFetcher.fetchSignature(args[4]);
                    String npcName = args[5];

                    if(skinValue.isEmpty() || skinSignature.isEmpty()) {
                        languagePlayer.sendMessage(I18N.SKIN_FETCH_FAIL.get().replace("%PLAYER%", args[4]));
                    }
                    NPCSetting setting = new NPCSetting(
                            npcName,
                            imitatePlayer,
                            lookAtPlayer,
                            skinValue,
                            skinSignature,
                            new NPCLocation(player.getLocation())
                    );
                    this.privateServerConfig.setNPCSettings(setting);
                    this.privateServer.getConfiguration().writeConfiguration(this.privateServerConfig);
                    this.privateServer.getPrivateServerUtil().spawnNPC();
                } else {
                    languagePlayer.sendMessage(I18N.COMMAND_HELP.get());
                }
        }
        return false;
    }

    private boolean isNoBoolean(String input) {
        return !input.equalsIgnoreCase("true") && !input.equalsIgnoreCase("false");
    }

}
