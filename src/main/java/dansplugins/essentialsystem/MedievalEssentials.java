package dansplugins.essentialsystem;

import dansplugins.essentialsystem.Commands.*;
import dansplugins.essentialsystem.Objects.MOTD;
import dansplugins.essentialsystem.Objects.NicknameRecord;
import dansplugins.essentialsystem.Objects.PlayerActivityRecord;
import dansplugins.essentialsystem.bStats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class MedievalEssentials extends JavaPlugin implements Listener {

    private static MedievalEssentials instance;

    public MOTD motd = new MOTD();
    public ArrayList<PlayerActivityRecord> activityRecords = new ArrayList<>();
    public ArrayList<String> vanishedPlayers = new ArrayList<>();
    public ArrayList<String> mutedPlayers = new ArrayList<>();
    public ArrayList<NicknameRecord> nicknames = new ArrayList<>();
    public HashMap<Player, Location> lastLogins = new HashMap<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Essentials in enabling...");

        instance = this;

        this.getServer().getPluginManager().registerEvents(this, this);

        StorageManager.getInstance().load();

        int pluginId = 9527;
        Metrics metrics = new Metrics(this, pluginId);

        System.out.println("Medieval Essentials is enabled!");
    }

    @Override
    public void onDisable() {
        System.out.println("Medieval Essentials in disabling...");

        StorageManager.getInstance().save();

        System.out.println("Medieval Essentials is disabled!");
    }

    public static MedievalEssentials getInstance() {
        return instance;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (label.equalsIgnoreCase("medievalessentials")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
                HelpCommand command = new HelpCommand();
                command.sendHelpInfo(sender);
            }
        }

        if (label.equalsIgnoreCase("fly")) {
            FlyCommand command = new FlyCommand();
            command.toggleFlight(sender, args);
        }

        if (label.equalsIgnoreCase("broadcast")) {
            BroadcastCommand command = new BroadcastCommand();
            if (args.length != 0) {
                command.broadcast(sender, createStringFromArgs(0, args.length, args));
            }
            else {
                sender.sendMessage(ChatColor.RED + "Usage: /broadcast (message)");
                return false;
            }
        }

        if (label.equalsIgnoreCase("motd")) {
            MOTDCommand command = new MOTDCommand(this);
            command.showMOTD(sender);
        }

        if (label.equalsIgnoreCase("setmotd")) {
            SetMOTDCommand command = new SetMOTDCommand(this);
            command.setMOTD(sender, args);
        }

        if (label.equalsIgnoreCase("seen")) {
            SeenCommand command = new SeenCommand(this);
            command.showLastLogout(sender, args);
        }

        if (label.equalsIgnoreCase("vanish")) {
            VanishCommand command = new VanishCommand(this);
            command.toggleVisibility(sender);
        }

        if (label.equalsIgnoreCase("mute")) {
            MuteCommand command = new MuteCommand(this);
            command.mutePlayer(sender, args);
        }

        if (label.equalsIgnoreCase("unmute")) {
            UnmuteCommand command = new UnmuteCommand(this);
            command.unmutePlayer(sender, args);
        }

        if (label.equalsIgnoreCase("nick")) {
            NickCommand command = new NickCommand(this);
            command.changeDisplayName(sender, args);
        }
/*
        if (label.equalsIgnoreCase("whois")) {
            WhoIsCommand command = new WhoIsCommand();
            command.showIGNToPlayer(sender, args);
        }
*/

        if (label.equalsIgnoreCase("getpos")) {
            GetPosCommand command = new GetPosCommand(this);
            command.sendCoordinates(sender);
        }

        if (label.equalsIgnoreCase("flyspeed")) {
            FlySpeedCommand command = new FlySpeedCommand();
            command.setFlySpeed(sender, args);
        }

        if (label.equalsIgnoreCase("gm")) {
            GamemodeCommand command = new GamemodeCommand(this);
            command.setGamemode(sender, args);
        }

        if (label.equalsIgnoreCase("back")) {
            BackCommand command = new BackCommand(this);
            command.teleportToLastLocation(sender);
        }

        if (label.equalsIgnoreCase("logins")) {
            LoginsCommand command = new LoginsCommand(this);
            command.showLogins(sender);
        }

        if (label.equalsIgnoreCase("label")) {
            LabelCommand command = new LabelCommand(this);
            command.renameItemInMainHand(sender, args);
        }

        if (label.equalsIgnoreCase("invsee"))  {
            InvseeCommand command = new InvseeCommand();
            command.invseePlayer(sender, args);
        }

        if (label.equalsIgnoreCase("clearinv"))  {
            ClearInvCommand command = new ClearInvCommand();
            command.clearInv(sender, args);
        }

        return true;
    }

    public static String createStringFromArgs(int start, int end, String[] args) {
        String toReturn = "";
        for (int i = start; i < end; i++) {
            toReturn = toReturn + args[i];
            if (i < end - 1) {
                toReturn = toReturn + " ";
            }
        }
        return toReturn;
    }

    @EventHandler()
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(ChatColor.GREEN + "Welcome " + player.getName() + " to the server!");
            }
        }

        // show motd
        if (motd.isMessageSet()) {
            if (player.hasPermission("me.motd") || player.hasPermission("me.default")) {
                player.sendMessage(ChatColor.AQUA + motd.getMessage());
            }
        }

        // assign activity record if player doesn't have one
        if (!hasActivityRecord(player.getName())) {
            PlayerActivityRecord newRecord = new PlayerActivityRecord();
            newRecord.setPlayerName(player.getName());
            newRecord.incrementLogins();
            activityRecords.add(newRecord);
        }
        else {
            // increment logins for player if player already has record
            getActivityRecord(player.getName()).incrementLogins();
        }

        // hide vanished players from this player
        for (String vanishedPlayer : vanishedPlayers) {
            event.getPlayer().hidePlayer(this, getServer().getPlayer(vanishedPlayer));
        }

        // assign nickname
        if (hasNicknameRecord(event.getPlayer().getName())) {

            // if nickname not assigned
            if (!event.getPlayer().getName().equalsIgnoreCase(getNicknameRecord(event.getPlayer().getName()).getNickname())) {
                // assign it
                event.getPlayer().setDisplayName(ChatColor.translateAlternateColorCodes('&', getNicknameRecord(event.getPlayer().getName()).getNickname() + "&r"));
            }

        }

    }

    public boolean hasActivityRecord(String playerName) {
        for (PlayerActivityRecord record : activityRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public PlayerActivityRecord getActivityRecord(String playerName) {
        for (PlayerActivityRecord record : activityRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return record;
            }
        }
        return null;
    }

    public boolean hasNicknameRecord(String playerName) {
        for (NicknameRecord record : nicknames) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public NicknameRecord getNicknameRecord(String playerName) {
        for (NicknameRecord record : nicknames) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return record;
            }
        }
        return null;
    }

    @EventHandler()
    public void onQuit(PlayerQuitEvent event) {
        ZonedDateTime now = ZonedDateTime.now();
        getActivityRecord(event.getPlayer().getName()).setLastLogout(now);
    }

    @EventHandler()
    public void onChat(AsyncPlayerChatEvent event) {
        if (mutedPlayers.contains(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(ChatColor.RED + "You are currently muted.");
            event.setCancelled(true);
        }
    }

    public int[] getPlayersPosition(Player player) {
        int[] coords = new int[3];
        coords[0] = player.getLocation().getBlockX();
        coords[1] = player.getLocation().getBlockY();
        coords[2] = player.getLocation().getBlockZ();
        return coords;
    }

    public String getPlayersDirection(Player player) {
        // TODO: implement this
        return null;
    }

    @EventHandler()
    public void onTeleport(PlayerTeleportEvent event) {
        lastLogins.put(event.getPlayer(), event.getFrom());
    }

}