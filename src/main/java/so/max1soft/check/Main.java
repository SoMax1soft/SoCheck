package so.max1soft.check;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements Listener {
    private final Map<Player, String> checkingPlayers = new HashMap<>();
    private File discordFile;
    private FileConfiguration discordConfig;
    Main plugin = this;

    @Override
    public void onEnable() {
        saveConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("");
        getLogger().info("§fИвент: §aЗапущен");
        getLogger().info("§fСоздатель: §b@max1soft");
        getLogger().info("§fВерсия: §c1.0");
        getLogger().info("");
        PluginCommand command = getCommand("check");
        if (command != null) {
            command.setExecutor(this);
        } else {
            getLogger().warning("Не удалось команду наворкать.");
        }
        loadDiscordConfig();
    }

    private void loadDiscordConfig() {
        discordFile = new File(getDataFolder(), "discords.yml");
        if (!discordFile.exists()) {
            discordFile.getParentFile().mkdirs();
            try {
                discordFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать файл disords.yml!");
                e.printStackTrace();
            }
        }
        discordConfig = YamlConfiguration.loadConfiguration(discordFile);
    }

    private void saveDiscordConfig() {
        try {
            discordConfig.save(discordFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить файл дискордов!");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (checkingPlayers.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (checkingPlayers.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (checkingPlayers.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (checkingPlayers.containsKey(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (checkingPlayers.containsKey(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.hasMetadata("socheck_checking")) {
            player.removeMetadata("socheck_checking", plugin);
            String playerName = player.getName();
            String adminName = checkingPlayers.get(player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip " + playerName + " 14d Выход с проверки (Автоматическое наказание проверял - > " + adminName + " ) -s");
            checkingPlayers.remove(player);
            player.setInvulnerable(false);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "Только игроки.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("check")) {
            if (args.length == 0) {
                sender.sendMessage(this.getConfig().getString("usage_message").replace("&", "§"));
                sender.sendMessage(this.getConfig().getString("invalid_command_usage").replace("&", "§"));
                sender.sendMessage(this.getConfig().getString("usage_confirm").replace("&", "§"));
                return true;
            }

            if (args[0].equalsIgnoreCase("setmydiscord")) {
                if (args.length < 2) {
                    sender.sendMessage(this.getConfig().getString("set_discord_usage").replace("&", "§"));
                    return true;
                }

                String discord = args[1];
                discordConfig.set(sender.getName(), discord);
                saveDiscordConfig();
                sender.sendMessage(this.getConfig().getString("discord_set").replace("&", "§") + discord);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(this.getConfig().getString("player_not_found").replace("&", "§"));
                return true;
            }

            if (!sender.hasPermission("socheck.check")) {
                sender.sendMessage(this.getConfig().getString("no_permission").replace("&", "§"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("§fИспользуйте: §6/check §7(Ник) (start|stop|confirm) (Ваш ник)");
                return true;
            }

            if (args[1].equalsIgnoreCase("start")) {
                if (checkingPlayers.containsKey(target)) {
                    sender.sendMessage(this.getConfig().getString("already_checking").replace("&", "§"));
                    return true;
                }

                String adminName = sender.getName();
                String discord = discordConfig.getString(adminName);
                if (discord == null) {
                    sender.sendMessage(this.getConfig().getString("no_discord").replace("&", "§"));
                    return true;
                }
                if (target.equals(sender)) {
                    sender.sendMessage(this.getConfig().getString("self_check").replace("&", "§"));
                    return true;
                }

                double x = Double.parseDouble(this.getConfig().getString("player_location_x"));
                double y = Double.parseDouble(this.getConfig().getString("player_location_y"));
                double z = Double.parseDouble(this.getConfig().getString("player_location_z"));
                double yaw = Double.parseDouble(this.getConfig().getString("player_location_yaw"));
                double pitch = Double.parseDouble(this.getConfig().getString("player_location_pitch"));
                double xa = Double.parseDouble(this.getConfig().getString("admin_location_x"));
                double ya = Double.parseDouble(this.getConfig().getString("admin_location_y"));
                double za = Double.parseDouble(this.getConfig().getString("admin_location_z"));
                double yawa = Double.parseDouble(this.getConfig().getString("admin_location_yaw"));
                double pitcha = Double.parseDouble(this.getConfig().getString("admin_location_pitch"));

                ArrayList<String> messages = new ArrayList<>();
                checkingPlayers.put(target, adminName);
                Location adminLocation = new Location(Bukkit.getWorld(this.getConfig().getString("admin_location_world")), xa, ya, za, (float) yaw, (float) pitch);
                Location checkLocation = new Location(Bukkit.getWorld(this.getConfig().getString("player_location_world")), x, y, z,  (float) yawa, (float) pitcha);
                target.teleport(checkLocation);
                target.setMetadata("socheck_checking", new FixedMetadataValue(plugin, true));
                ((Player) sender).teleport(adminLocation);
                target.setInvulnerable(true);

                // Отправляем сообщение каждую секунду
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (checkingPlayers.containsKey(target)) {
                            target.sendTitle(
                                    Main.this.getConfig().getString("check_title").replace("&", "§"),
                                    Main.this.getConfig().getString("check_title_sub").replace("&", "§")
                            );
                            target.sendMessage("");
                            target.sendMessage(Main.this.getConfig().getString("check_message").replace("&", "§"));
                            target.sendMessage(Main.this.getConfig().getString("admin_name").replace("&", "§") + adminName);
                            target.sendMessage(Main.this.getConfig().getString("admin_discord").replace("&", "§") + discord);
                            target.sendMessage(Main.this.getConfig().getString("admin_time").replace("&", "§"));
                            target.sendMessage("");
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this, 0L, 20L);

                sender.sendMessage(this.getConfig().getString("check_started").replace("&", "§"));
                for (String message : messages) {
                    target.sendMessage(message);
                }
            } else if (args[1].equalsIgnoreCase("stop")) {
                if (!checkingPlayers.containsKey(target)) {
                    sender.sendMessage("§fИгрок не на проверке.");
                    return true;
                }

                checkingPlayers.remove(target);

                Location spawnLocation = new Location(Bukkit.getWorld("spawn"), 0.548, 51, -33.403);
                target.teleport(spawnLocation);
                target.setInvulnerable(false);
                target.removeMetadata("socheck_checking", plugin);
                target.sendTitle(this.getConfig().getString("check_end_title").replace("&", "§"), "");
                target.sendMessage(this.getConfig().getString("check_ended").replace("&", "§"));
                sender.sendMessage(this.getConfig().getString("check_stopped").replace("&", "§"));
            } else if (args[1].equalsIgnoreCase("confirm")) {
                if (!checkingPlayers.containsKey(target)) {
                    sender.sendMessage("§fИгрок не на проверке.");
                    return true;
                }

                String adminName = sender.getName();
                checkingPlayers.remove(target);

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "banip " + target.getName() + " 14d Читер (Автоматическое наказание проверял - > " + adminName + " )");

                target.teleport(new Location(Bukkit.getWorld("spawn"), 3.500, 89, 38.500));
                target.setInvulnerable(false);
                target.removeMetadata("socheck_checking", plugin);
                target.sendTitle(this.getConfig().getString("check_end_title").replace("&", "§"), "");
                target.sendMessage(this.getConfig().getString("check_ended").replace("&", "§"));
                sender.sendMessage(this.getConfig().getString("check_confirmed").replace("&", "§"));
            } else {
                sender.sendMessage("§fИспользуйте: §6/check §7(Ник) (start|stop|confirm) (Ваш ник)");
                return true;
            }
        }

        return true;
    }
}
