package pandorum.vote;

import arc.files.Fi;
import arc.util.*;
import arc.util.Timer.Task;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.gen.*;
import mindustry.io.SaveIO;
import mindustry.maps.MapException;
import mindustry.net.WorldReloader;

import static mindustry.Vars.*;

public class VoteLoadSession extends VoteSession {
    private final Fi target;

    public VoteLoadSession(Fi target) {
        this.target = target;
    }

    @Override
    protected void end() {
        Call.sendMessage(Strings.format("[lightgray]Голосование провалилось. Не хватает голосов, " +
                        "чтобы загрузить сохранение [orange]@[lightgray].",
                target.nameWithoutExtension()));
    }

    @Override
    protected void notifyVote(Player player) {
        Call.sendMessage(Strings.format("@[lightgray] проголосовал за " +
                        "загрузку сохранения [orange]@[].[accent] (@/@)\n" +
                        "[lightgray]Напишите[orange] vote y/n[] чтобы проголосовать.",
                player.coloredName(), target.nameWithoutExtension(), votes, votesRequired()));
    }

    @Override
    protected void passed() {
        Call.sendMessage(Strings.format("[orange]Голосование закончено. " +
                "Загружаем сохранение [scarlet] @[orange]...", target.nameWithoutExtension()));

        Runnable r = () -> {
            WorldReloader reloader = new WorldReloader();

            reloader.begin();
            SaveIO.load(target);

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
