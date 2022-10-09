package pandorum;

import arc.Events;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.ObjectSet;
import arc.util.CommandHandler;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;
import mindustry.mod.Plugin;
import pandorum.vote.*;

public class PandorumPlugin extends Plugin {
    public static VoteSession currentSession;

    private final ObjectSet<String> rtvVotes = new ObjectSet<>();

    @Override
    public void init() {
        Events.on(EventType.PlayerLeave.class, event -> {
            int cur = rtvVotes.size;
            int req = (int) Math.ceil(VoteSession.voteRatio * Groups.player.size());
            if (rtvVotes.contains(event.player.uuid())) {
                rtvVotes.remove(event.player.uuid());
                Call.sendMessage(Strings.format("[lightgray][[RTV]: @[accent] вышел, голосов: [green]@[accent]," +
                                " требуется голосов для смены карты: [green]@[accent]",
                        event.player.coloredName(), cur - 1, req));
            }
        });

        Events.run(EventType.GameOverEvent.class, rtvVotes::clear);
    }

    // ну так то нужен rtv и прочие голосования это точно

    @Override
    public void registerClientCommands(CommandHandler handler) {

        handler.<Player>register("vote", "<y/n>", "Проголосовать за текущее голосование", (args, player) -> {
            if (currentSession == null) {
                player.sendMessage("[scarlet]Сейчас не идёт никакое голосование.");
                return;
            }

            int sign;
            switch (args[0].toLowerCase()) {
                case "y", "да" -> sign = 1;
                case "n", "нет" -> sign = -1;
                default -> {
                    player.sendMessage("[scarlet]Неправильный вариант ответа '" + args[0] + "'.");
                    return;
                }
            }

            currentSession.vote(player, sign);
        });

        handler.<Player>register("rtv", "Проголосовать за смену карты", (args, player) -> {
            if (rtvVotes.contains(player.uuid())) {
                player.sendMessage("[scarlet]Вы уже проголосовали. Успокойтесь.");
                return;
            }

            rtvVotes.add(player.uuid());
            int cur = rtvVotes.size;
            int req = (int) Math.ceil(VoteSession.voteRatio * Groups.player.size());
            Call.sendMessage(Strings.format("[lightgray][[RTV]: @[accent] хочет сменить карту, " +
                            "голосов: [green]@[accent], требуется голосов для смены карты: [green]@[accent].",
                    player.coloredName(), cur, req)); // ого, анюк добавил coloredName() раньше помню самому приходилось делать

            if (cur < req) {
                return;
            }

            Call.sendMessage("[lightgray][[RTV]: [green]набрано достаточное количество голосов, смена карты...");
            Events.fire(new EventType.GameOverEvent(Vars.state.rules.waveTeam));
        });

        handler.<Player>register("maps", "[страница]", "Вывести список карт.", (args, player) -> {
            if (args.length > 0 && !Strings.canParseInt(args[0])) {
                player.sendMessage("[scarlet]'Страница' должна быть числом!");
                return;
            }

            var mapList = Vars.maps.all();
            int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
            int pages = Mathf.ceil(mapList.size / 6f);

            if (--page >= pages || page < 0) {
                player.sendMessage(Strings.format("[scarlet]'Страница' должна быть между[orange] 1[] и[orange] @[scarlet].", pages));
                return;
            }

            StringBuilder result = new StringBuilder();
            result.append(Strings.format(" [orange]-- Список Карт Страница [lightgray]@[gray]/[lightgray]@[orange] --",
                    page + 1, pages)).append('\n');
            for (int i = 6 * page, n = Math.min(6 * (page + 1), mapList.size); i < n; i++) {
                result.append("[lightgray] ").append(i + 1).append("[orange] ").append(mapList.get(i).name()).append("[white]\n");
            }

            player.sendMessage(result.toString());
        });

        handler.<Player>register("nominate", "<map/save/load> <название...>", "Начать голосование за смену карты/загрузку карты", (args, player) -> {
            if (currentSession != null) {
                player.sendMessage("[scarlet]Голосование уже идет.");
                return;
            }

            VoteMode mode;
            try {
                mode = VoteMode.valueOf(args[0].toLowerCase());
            } catch (IllegalArgumentException t) {
                player.sendMessage("[scarlet]Неверный режим голосования '" + args[0] + "'.");
                return;
            }

            switch (mode) {
                case map -> {
                    Map map = findMap(args[1]);
                    if (map == null) {
                        player.sendMessage("[scarlet]Карта с таким именем '" + args[1] + "' не найдена.");
                        return;
                    }

                    currentSession = new VoteMapSession(map);
                }
                case save -> currentSession = new VoteSaveSession(args[1]);
                case load -> {
                    Fi save = findSave(args[1]);
                    if (save == null) {
                        player.sendMessage("[scarlet]Сохранение с таким именем '" + args[1] + "' не найдено.");
                        return;
                    }

                    currentSession = new VoteLoadSession(save);
                }
            }

            currentSession.vote(player, 1);
        });
    }

    private static Map findMap(String text) {
        for (int i = 0; i < Vars.maps.all().size; i++) {
            Map map = Vars.maps.all().get(i);
            if ((Strings.canParseInt(text) && i == Strings.parseInt(text) - 1) || map.name().equals(text)) {
                return map;
            }
        }
        return null;
    }

    private static Fi findSave(String text) {
        var list = Vars.saveDirectory.list();
        for (int i = 0; i < list.length; i++) {
            Fi save = list[i];
            if ((Strings.canParseInt(text) && i == Strings.parseInt(text) - 1) || save.nameWithoutExtension().equals(text)) {
                return save;
            }
        }
        return null;
    }
}
