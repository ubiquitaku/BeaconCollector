package ubiquitaku.beaconcollector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class BeaconCollector extends JavaPlugin implements @NotNull Listener {
    String prefix = "§l[BeaconCollector]§r ";
    FileConfiguration config;
    Map<String,UUID> map = new HashMap<>();
    Map<String,Integer> owners = new HashMap<>();
    boolean ret;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this,this);
        saveDefaultConfig();
        config = getConfig();
        ret = config.getBoolean("options.returnPlanter");
        mapLoad();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        mapSave();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("bcol")) {
            if (args[0].equals("get")) {
                if (!ret) {
                    sender.sendMessage(prefix+"返却機能は現在使用できません");
                }
                Player p = (Player) sender;
                if (!owners.containsKey(p.getUniqueId())) {
                    sender.sendMessage(prefix+"あなたのベーコンは保管されていません");
                    return true;
                }
                if (!p.getInventory().isEmpty()) {
                    sender.sendMessage(prefix+"インベントリに空きを作ってから入力して下さい");
                    return true;
                }
                ItemStack item = new ItemStack(Material.BEACON);
                item.setAmount(owners.get(p.getPlayer().getUniqueId()));
                p.getInventory().addItem(item);
                owners.remove(p.getUniqueId());
            }
        }
        return true;
    }

    //
    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!ret) {
            return;
        }
        if (!owners.containsKey(e.getPlayer().getUniqueId())) {
            return;
        }
        if (!e.getPlayer().getInventory().isEmpty()) {
            e.getPlayer().sendMessage(prefix+"インベントリに空きを作ってから/bcol getと入力してください");
            return;
        }
        ItemStack item = new ItemStack(Material.BEACON);
        item.setAmount(owners.get(e.getPlayer().getUniqueId()));
        e.getPlayer().getInventory().addItem(item);
    }

    //ベーコンが置かれた場所と置いたプレイヤーの情報をmapに保存
    @EventHandler
    public void placeBeacon(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Material.BEACON) {
            return;
        }
        map.put(locStr(e.getBlock().getLocation()),e.getPlayer().getUniqueId());
        e.getPlayer().sendMessage(prefix+"ベーコンを保護します(できてる確証は無いけど(∀｀*ゞ)ﾃﾍｯ)");
    }

    //ベーコンが破壊されたときのイベント
    @EventHandler
    public void breakBeacon(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BEACON) {
            return;
        }
        if (!map.containsKey(locStr(e.getBlock().getLocation()))) {
            return;
        }
        if (map.get(locStr(e.getBlock().getLocation())) == e.getPlayer().getUniqueId()) {
            map.remove(locStr(e.getBlock().getLocation()));
            e.getPlayer().sendMessage(prefix+"ベーコンの回収を確認したため保護も削除します");
            return;
        }
        if (map.get(locStr(e.getBlock().getLocation())) == e.getPlayer().getUniqueId()) {
            e.getPlayer().sendMessage(prefix+"保護されていないようなのでこのベーコンの破壊はキャンセルされません");
            return;
        }
        e.setCancelled(true);
        e.getPlayer().sendMessage(prefix+"あなたが設置したベーコンではないため破壊できません");
    }

    //configに保存されている取り残されたかわいそうなベーコンの情報を取り出します
    public void mapLoad() {
        if (!config.contains("Beacons")) {
            return;
        }
        List<UUID> list = new ArrayList<>();
        for (String key : config.getConfigurationSection("Beacons").getKeys(false)) {
            if (list.contains(config.getString("Beacons."+key))) {
                owners.put(key,owners.get(key)+1);
                return;
            }
            list.add(UUID.fromString(config.getString("Beacons."+key)));
            owners.put(config.getString(key),1);
        }
    }

    //mapの情報をconfigに保存します
    public void mapSave() {
        for (String key : map.keySet()) {
            config.set(String.valueOf(key),map.get(key));
        }
    }

    //Location型から必要な情報だけを取り出して取り出しに対応した形にします
    public String locStr(Location location) {
        return location.getWorld().getName()+"/"+location.getBlockX()+"/"+location.getBlockY()+"/"+location.getBlockZ();
    }

    //このpluginに必要な情報だけが書かれたStringのデータをLocation型に直します
    public Location strLoc(String stringLocation) {
        String[] str = stringLocation.split("/");
        return new Location(Bukkit.getWorld(str[0]),Integer.parseInt(str[1]),Integer.parseInt(str[2]),Integer.parseInt(str[3]));
    }
}
