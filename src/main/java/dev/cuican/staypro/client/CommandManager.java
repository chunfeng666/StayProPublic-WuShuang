package dev.cuican.staypro.client;

import dev.cuican.staypro.Stay;
import dev.cuican.staypro.command.Command;
import dev.cuican.staypro.command.commands.*;
import dev.cuican.staypro.concurrent.TaskManager;
import dev.cuican.staypro.concurrent.event.Listener;
import dev.cuican.staypro.concurrent.event.Priority;
import dev.cuican.staypro.event.events.client.ChatEvent;
import dev.cuican.staypro.utils.ChatUtil;
import dev.cuican.staypro.utils.Wrapper;
import net.minecraft.network.play.client.CPacketPlayer;

import java.util.*;

public class CommandManager {

    public static String cmdPrefix = ".";
    public List<Command> commands = new ArrayList<>();
    private final Set<Class<? extends Command>> classes = new HashSet<>();

    public static void init() {
        instance = new CommandManager();
        instance.commands.clear();

        register(Bind.class);
        register(Commands.class);
        register(Config.class);
        register(Friend.class);
        register(Help.class);
        register(Prefix.class);
        register(Send.class);
        register(Toggle.class);
        register(TP.class);

        instance.loadCommands();
        Stay.EVENT_BUS.register(instance);
    }

    private static void register(Class<? extends Command> clazz) {
        instance.classes.add(clazz);
    }

    private void loadCommands() {
        classes.stream().sorted(Comparator.comparing(Class::getSimpleName)).forEach(clazz -> {
            try {
                Command command = clazz.newInstance();
                commands.add(command);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Couldn't initiate Command " + clazz.getSimpleName() + "! Err: " + e.getClass().getSimpleName() + ", message: " + e.getMessage());
            }
        });
    }

    @Listener(priority = Priority.HIGHEST)
    public void onChat(ChatEvent event) {
        if (event.getMessage().startsWith(cmdPrefix)) {
            TaskManager.launch(() -> runCommands(event.getMessage()));
            event.cancel();
        }
    }

    public void runCommands(String s) {
        String readString = s.trim().substring(cmdPrefix.length()).trim();
        boolean commandResolved = false;
        boolean hasArgs = readString.trim().contains(" ");
        String commandName = hasArgs ? readString.split(" ")[0] : readString.trim();
        String[] args = hasArgs ? readString.substring(commandName.length()).trim().split(" ") : new String[0];

        for (Command command : commands) {
            if (command.getCommand().trim().equalsIgnoreCase(commandName.trim().toLowerCase())) {
                command.onCall(readString, args);
                commandResolved = true;
                break;
            }
        }
        if (!commandResolved) {
            ChatUtil.sendNoSpamErrorMessage("Unknown command. try 'help' for a list of commands.");
        }
    }

    private static CommandManager instance;

    public static CommandManager getInstance() {
        if (instance == null) instance = new CommandManager();
        return instance;
    }
    private static double x;
    private static double y;
    private static double z;
    private static boolean onground;

    public static void updatePosition() {
        x = Wrapper.mc.player.posX;
        y = Wrapper.mc.player.posY;
        z = Wrapper.mc.player.posZ;
        onground = Wrapper.mc.player.onGround;
    }
    public static void setPositionPacket(double x, double y, double z, boolean onGround, boolean setPos, boolean noLagBack) {
        Wrapper.mc.player.connection.sendPacket(new CPacketPlayer.Position(x, y, z, onGround));
        if (setPos) {
            Wrapper.mc.player.setPosition(x, y, z);
            if (noLagBack) {
                updatePosition();
            }
        }
    }

}
