package pandorum.vote;

import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.io.SaveIO;

public class VoteSaveSession extends VoteSession {
    private final String target;

    public VoteSaveSession(String target) {
        this.target = target;
    }

    @Override
    protected void end() {
        Call.sendMessage(Strings.format("[lightgray]Голосование провалилось. Не хватает голосов, " +
                "чтобы сохранить карту в файл [orange]@[lightgray].", target));
    }

    @Override
    protected void notifyVote(Player player) {
        Call.sendMessage(Strings.format("@[lightgray] проголосовал за сохранения карты в файл [orange]@[].[accent] (@/@)\n" +
                "[lightgray]Напишите[orange] vote y/n[] чтобы проголосовать.",
                player.coloredName(), target, votes, votesRequired()));
    }

    @Override
    protected void passed() {
        Call.sendMessage(Strings.format("[orange]Голосование закончено. Сохраняю карту в [scarlet]@.", target));
        SaveIO.save(Vars.saveDirectory.child(target + '.' + Vars.saveExtension));
    }
}
