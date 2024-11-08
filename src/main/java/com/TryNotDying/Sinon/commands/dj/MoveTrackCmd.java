package com.TryNotDying.Sinon.commands.dj;


import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.QueuedTrack;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.TryNotDying.Sinon.queue.AbstractQueue;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Command that provides users the ability to move a track in the playlist.
 */
public class MoveTrackCmd extends SlashDJCommand
{

    public MoveTrackCmd(Bot bot)
    {
        super(bot);
        this.name = "movetrack";
        this.help = "move a track in the current queue to a different position";
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("from", "The original position of the song", OptionType.INTEGER, true),
            new Option("to", "The new position of the song", OptionType.INTEGER, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event)
    {
        Member member = event.getMember();
        if (member == null) {
            event.replyError("You do not have permission to use this command.");
            return;
        }

        // Check if the user has the DJ role
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getIdLong() == bot.getConfig().getDjRoleId()) {
                int from;
                int to;

                // Validate the args
                from = event.getOption("from").getAsInt();
                to = event.getOption("to").getAsInt();

                if (from == to)
                {
                    event.replyError("Can't move a track to the same position.");
                    return;
                }

                // Validate that from and to are available
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                AbstractQueue<QueuedTrack> queue = handler.getQueue();
                if (isUnavailablePosition(queue, from))
                {
                    String reply = String.format("`%d` is not a valid position in the queue!", from);
                    event.replyError(reply);
                    return;
                }
                if (isUnavailablePosition(queue, to))
                {
                    String reply = String.format("`%d` is not a valid position in the queue!", to);
                    event.replyError(reply);
                    return;
                }

                // Move the track
                QueuedTrack track = queue.moveItem(from - 1, to - 1);
                String trackTitle = track.getTrack().getInfo().title;
                String reply = String.format("Moved **%s** from position `%d` to `%d`.", trackTitle, from, to);
                event.replySuccess(reply);
                return; // Allow the user
            }
        }

        // If the user doesn't have the DJ role, send an error embed.
        event.reply(new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Error")
                .setDescription("You do not have permission to use this command.")
                .build());
    }

    private static boolean isUnavailablePosition(AbstractQueue<QueuedTrack> queue, int position)
    {
        return (position < 1 || position > queue.size());
    }
}