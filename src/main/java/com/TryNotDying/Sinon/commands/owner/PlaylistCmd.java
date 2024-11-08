/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.TryNotDying.Sinon.commands.owner;

import java.io.IOException;
import java.util.List;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.OwnerCommand;
import com.TryNotDying.Sinon.playlist.PlaylistLoader.Playlist;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the playlist command class
 */
public class PlaylistCmd extends OwnerCommand {
    private final Bot bot;

    public PlaylistCmd(Bot bot) {
        super(bot);
        this.bot = bot;
        this.name = "playlist";
        this.help = "playlist management";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("action", "Action to perform (append, delete, make, setdefault)", OptionType.STRING, true),
            new Option("playlist", "Playlist name", OptionType.STRING, false),
            new Option("url", "URL to add", OptionType.STRING, false)
        };
        this.children = new OwnerCommand[]{
            new ListCmd(),
            new AppendlistCmd(),
            new DeletelistCmd(),
            new MakelistCmd(),
            new DefaultlistCmd(bot)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String action = event.getOption("action").getAsString();

        switch (action.toLowerCase()) {
            case "make":
            case "create":
                new MakelistCmd().doCommand(event);
                break;
            case "delete":
            case "remove":
                new DeletelistCmd().doCommand(event);
                break;
            case "append":
            case "add":
                new AppendlistCmd().doCommand(event);
                break;
            case "setdefault":
            case "default":
                new DefaultlistCmd(bot).doCommand(event);
                break;
            case "all":
            case "available":
            case "list":
                new ListCmd().doCommand(event);
                break;
            default:
                StringBuilder builder = new StringBuilder(event.getClient().getWarning() + " Playlist Management Commands:\n");
                for (Command cmd : this.children) {
                    builder.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                            .append(" ").append(cmd.getArguments() == null ? "" : cmd.getArguments()).append("` - ").append(cmd.getHelp());
                }
                event.reply(builder.toString());
                break;
        }
    }

    public class MakelistCmd extends OwnerCommand {
        public MakelistCmd() {
            this.name = "make";
            this.aliases = new String[]{"create"};
            this.help = "makes a new playlist";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("playlist", "Playlist name", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String pname = event.getOption("playlist").getAsString().replaceAll("\\s+", "_");
            pname = pname.replaceAll("[*?|\\/\":<>]", "");
            if (pname == null || pname.isEmpty()) {
                event.replyError("Please provide a name for the playlist!");
            } else if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
                try {
                    bot.getPlaylistLoader().createPlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " Successfully created playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to create the playlist: " + e.getLocalizedMessage());
                }
            } else {
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` already exists!");
            }
        }
    }

    public class DeletelistCmd extends OwnerCommand {
        public DeletelistCmd() {
            this.name = "delete";
            this.aliases = new String[]{"remove"};
            this.help = "deletes an existing playlist";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("playlist", "Playlist name", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String pname = event.getOption("playlist").getAsString().replaceAll("\\s+", "_");
            if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` doesn't exist!");
            } else {
                try {
                    bot.getPlaylistLoader().deletePlaylist(pname);
                    event.reply(event.getClient().getSuccess() + " Successfully deleted playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to delete the playlist: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public class AppendlistCmd extends OwnerCommand {
        public AppendlistCmd() {
            this.name = "append";
            this.aliases = new String[]{"add"};
            this.help = "appends songs to an existing playlist";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("playlist", "Playlist name", OptionType.STRING, true),
                new Option("url", "URL to add", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String pname = event.getOption("playlist").getAsString();
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(pname);
            if (playlist == null) {
                event.reply(event.getClient().getError() + " Playlist `" + pname + "` doesn't exist!");
            } else {
                StringBuilder builder = new StringBuilder();
                playlist.getItems().forEach(item -> builder.append("\r\n").append(item));
                String url = event.getOption("url").getAsString();
                builder.append("\r\n").append(url);
                try {
                    bot.getPlaylistLoader().writePlaylist(pname, builder.toString());
                    event.reply(event.getClient().getSuccess() + " Successfully added item to playlist `" + pname + "`!");
                } catch (IOException e) {
                    event.reply(event.getClient().getError() + " I was unable to append to the playlist: " + e.getLocalizedMessage());
                }
            }
        }
    }

    public class DefaultlistCmd extends AutoplaylistCmd {
        public DefaultlistCmd(Bot bot) {
            super(bot);
            this.name = "setdefault";
            this.aliases = new String[]{"default"};
            this.help = "sets the default playlist for the server";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("playlist", "Playlist name (or NONE to clear)", OptionType.STRING, true)
            };
        }
    }

    public class ListCmd extends OwnerCommand {
        public ListCmd() {
            this.name = "all";
            this.aliases = new String[]{"available", "list"};
            this.help = "lists all available playlists";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        }

        @Override
        protected void doCommand(CommandEvent event) {
            if (!bot.getPlaylistLoader().folderExists()) {
                bot.getPlaylistLoader().createFolder();
            }
            if (!bot.getPlaylistLoader().folderExists()) {
                event.reply(event.getClient().getWarning() + " Playlists folder does not exist and could not be created!");
                return;
            }
            List<String> list = bot.getPlaylistLoader().getPlaylistNames();
            if (list == null) {
                event.reply(event.getClient().getError() + " Failed to load available playlists!");
            } else if (list.isEmpty()) {
                event.reply(event.getClient().getWarning() + " There are no playlists in the Playlists folder!");
            } else {
                StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
                list.forEach(str -> builder.append("`").append(str).append("` "));
                event.reply(builder.toString());
            }
        }
    }
}