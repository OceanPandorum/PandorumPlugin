package pandorum.vote;

import arc.struct.ObjectSet;
import arc.util.Log;
import arc.util.Time;
import arc.util.Timer;
import mindustry.game.Gamemode;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.io.SaveIO;
import mindustry.maps.MapException;
import mindustry.net.WorldReloader;
import pandorum.PandorumPlugin;

import static mindustry.Vars.*;
import static mindustry.Vars.net;

public abstract class VoteSession {
    public static final float voteRatio = 0.5f;
    public static final float voteDuration = Time.toMinutes * 1.5f;

    protected final ObjectSet<String> voted = new ObjectSet<>();

    protected Timer.Task task;
    protected int votes;

    public VoteSession() {
        this.task = Timer.schedule(() -> {
            if (!checkPass()) {
                end();
                PandorumPlugin.currentSession = null;
                voted.clear();
                task.cancel();
            }
        }, voteDuration);
    }

    protected abstract void end();

    public final void vote(Player player, int delta) {
        // TODO: хочется менять свой вариант ответа
        if (voted.contains(player.uuid()) || voted.contains(netServer.admins.getInfo(player.uuid()).lastIP)) {
            player.sendMessage("[scarlet]Вы уже проголосовали. Успокойтесь.");
            return;
        }

        votes += delta;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);
        notifyVote(player);
        checkPass();
    }

    protected abstract void notifyVote(Player player);

    protected final boolean checkPass() {
        if(votes >= votesRequired()){
            PandorumPlugin.currentSession = null;
            task.cancel();

            passed();
            return true;
        }
        return false;
    }

    protected abstract void passed();

    protected final int votesRequired() {
        return (int) Math.ceil(voteRatio * Groups.player.size());
    }
}
