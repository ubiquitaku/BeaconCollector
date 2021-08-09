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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class BeaconCollector extends JavaPlugin implements @NotNull Listener {
    String prefix = "§l[BeaconCollector]§r ";
    FileConfiguration config;
    Map<String,UUID> map = new HashMap<>();
    List<String> owners = new ArrayList<>();
//    Map<UUID,Integer> owners = new HashMap<>();
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
            if (args.length == 0) {
                sender.sendMessage(prefix+"--------------------------------");
                sender.sendMessage("/bcol get : ベーコンの回収をします");
                sender.sendMessage("/bcol save : かわいそうなベーコンをほぞんします(debug)");
                sender.sendMessage(prefix+"--------------------------------");
                return true;
            }
            if (args[0].equals("get")) {
                if (!ret) {
                    sender.sendMessage(prefix+"返却機能は現在使用できません");
                }
                Player p = (Player) sender;
                if (!owners.contains(String.valueOf(p.getUniqueId()))) {
                    sender.sendMessage(prefix+"あなたのベーコンは保管されていません");
                    return true;
                }
                if (p.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(prefix+"インベントリに空きを作ってから入力して下さい");
                    return true;
                }
                ItemStack item = new ItemStack(Material.BEACON);
//                item.setAmount(owners.get(p.getPlayer().getUniqueId()));
                p.getInventory().addItem(item);
                owners.remove(p.getUniqueId());
                config.set("Beacons."+String.valueOf(p.getUniqueId()),"");
                saveConfig();
                sender.sendMessage(prefix+"ベーコンを回収しました");
                return true;
            }
            if (args[0].equals("save")) {
                mapSave();
                sender.sendMessage(prefix+"保存しました");
            }
        }
        return true;
    }

    //設置者以外設定いじれないように
    @EventHandler
    public void rightClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock().getType() != Material.BEACON) {
                return;
            }
            if (!map.containsKey(locStr(e.getClickedBlock().getLocation()))) {
                e.getPlayer().sendMessage(prefix+"このベーコンは保護されていようなので設置しなおしてから使用してください");
                e.setCancelled(true);
                return;
            }
            if (map.get(locStr(e.getClickedBlock().getLocation())).equals(e.getPlayer().getUniqueId())) {
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"あなたはこのベーコンの操作ができません");
        }
    }

    //次回ログイン時に回収できるか確認、無理であればコマンドへ誘導
    @EventHandler
    public void join(PlayerJoinEvent e) {
        if (!ret) {
            return;
        }
        if (!owners.contains(String.valueOf(e.getPlayer().getUniqueId()))) {
            return;
        }
        if (e.getPlayer().getInventory().firstEmpty() == -1) {
            e.getPlayer().sendMessage(prefix+"インベントリに空きを作ってから/bcol getと入力してください");
            return;
        }
        ItemStack item = new ItemStack(Material.BEACON);
//        item.setAmount(owners.get(e.getPlayer().getUniqueId()));
        e.getPlayer().getInventory().addItem(item);
        owners.remove(e.getPlayer().getUniqueId());
        config.set("Beacons."+String.valueOf(e.getPlayer().getUniqueId()),"");
        saveConfig();
        e.getPlayer().sendMessage(prefix+"ベーコンを回収しました");
    }

    //ベーコンが置かれた場所と置いたプレイヤーの情報をmapに保存
    @EventHandler
    public void placeBeacon(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Material.BEACON) {
            return;
        }
        map.put(locStr(e.getBlock().getLocation()),e.getPlayer().getUniqueId());
        e.getPlayer().sendMessage(prefix+"ベーコンを保護します(できてる確証は無いけど(∀｀*ゞ)ﾃﾍｯ)");
        e.getPlayer().sendMessage(prefix+"再起動等にも耐える保護は一人１個です");
    }

    //ベーコンが破壊されたときのイベント
    @EventHandler
    public void breakBeacon(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.BEACON) {
            return;
        }
        if (!map.containsKey(locStr(e.getBlock().getLocation()))) {
            e.getPlayer().sendMessage(prefix+"保護されていないようなのでこのベーコンの破壊はキャンセルされません");
            return;
        }
        if (map.get(locStr(e.getBlock().getLocation())) == e.getPlayer().getUniqueId()) {
            map.remove(locStr(e.getBlock().getLocation()));
            e.getPlayer().sendMessage(prefix+"ベーコンの回収を確認したため保護も削除します");
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
        try {
            owners = (List<String>) config.getList("Beacons");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    //mapの情報をconfigに保存します
    public void mapSave() {
        List<String> list = new ArrayList<>();
        for (String key : map.keySet()) {
            if (map.get(key) == null) continue;
            list.add(String.valueOf(map.get(key)));
        }
        config.set("Beacons",list);
        saveConfig();
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
