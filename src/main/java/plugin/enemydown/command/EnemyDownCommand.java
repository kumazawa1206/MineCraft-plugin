package plugin.enemydown.command;

import java.util.List;
import java.util.SplittableRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class EnemyDownCommand implements CommandExecutor {

  @Override
  /**空腹時に回復できる**/
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player player) {
      World world = player.getWorld();

      //プレイヤーの状態を初期化(体力と空腹度を最大値にする)
      initPlayerStatus(player);

      world.spawnEntity(getEnemySpawnLocation(player, world), getEnemy());
    }
    return false;
  }

  /**
   * ゲームを始める前にプレーヤーの状態を設定 体力・空腹度を最大値にし、装備はネザライト一式に変更。
   *
   * @param player
   */
  private void initPlayerStatus(Player player) {
    player.setHealth(20);
    player.setFoodLevel(20);

    PlayerInventory inventory = player.getInventory();
    inventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
    inventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
    inventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
    inventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
    inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
  }


  /**
   * 敵の出現エリアを取得
   *
   * @param player 　コマンドを実行したプレイヤー
   * @param world  　コマンドを実行したプレイヤーが所属するワールド
   * @return　敵の出現場所
   */

  private Location getEnemySpawnLocation(Player player, World world) {
    Location playerlocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(20) - 10;
    int randomZ = new SplittableRandom().nextInt(20) - 10;

    double x = playerlocation.getX() + randomX;
    double y = playerlocation.getY();
    double Z = playerlocation.getZ() + randomZ;

    return new Location(world, (x), y, (Z));
  }

  /**
   * ランダムで敵を出現させる
   *
   * @return　敵
   */
  private EntityType getEnemy() {
    List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON);
    return enemyList.get(new SplittableRandom().nextInt(2));
  }
}
