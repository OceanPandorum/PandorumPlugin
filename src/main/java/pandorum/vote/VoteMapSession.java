package pandorum.vote;

import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import arc.util.Timer.Task;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.maps.MapException;
import mindustry.net.WorldReloader;

import static mindustry.Vars.*;

public class VoteMapSession extends VoteSession {
    private final Map target;

    public VoteMapSession(Map target) {
        this.target = target;
    }

    @Override
    protected void end() {
        Call.sendMessage(Strings.format("[lightgray]Голосование провалилось. Не хватает голосов, " +
                "чтобы сменить карту на [orange]@[lightgray].", target.name()));
    }

    @Override
    protected void notifyVote(Player player) {
        Call.sendMessage(Strings.format("@[lightgray] проголосовал за карту [orange]@[].[accent] (@/@)\n" +
                        "[lightgray]Напишите[orange] vote y/n[] чтобы проголосовать.",
                player.coloredName(), target.name(), votes, votesRequired()));
    }

    @Override
    protected void passed() {
        Call.sendMessage(Strings.format("[orange]Голосование закончено. " +
                "Загружаем карту [scarlet]@[orange]...", target.name()));

        Runnable r = () -> {
            WorldReloader reloader = new WorldReloader();

            reloader.begin();

            Vars.world.loadMap(target, target.applyRules(Gamemode.survival));

            Vars.state.rules = Vars.state.map.applyRules(Gamemode.survival);
            Vars.logic.play();

            reloader.end();
        };

        Timer.schedule(new Task() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (MapException e) {
                    Log.err(e);
                    net.closeServer();
                }
            }
        }, 10);
    }
}
