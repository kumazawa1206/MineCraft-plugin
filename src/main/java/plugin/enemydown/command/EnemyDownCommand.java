package plugin.enemydown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.enemydown.EnemyDown;
import plugin.enemydown.data.PlayerScore;

public class EnemyDownCommand implements CommandExecutor, Listener {

  private EnemyDown enemyDown;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private int gameTime = 20;

  public EnemyDownCommand(EnemyDown enemyDown) {
    this.enemyDown = enemyDown;
  }


  @Override
  /**空腹時に回復できる**/
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    if (sender instanceof Player player) {
      if (playerScoreList.isEmpty()) {
        addNewPlayer(player);
      } else {
        for (PlayerScore playerScore : playerScoreList) {
          if (!playerScore.getPlayerName().equals(player.getName())) {
            addNewPlayer(player);
          }
        }
      }
      gameTime = 20;
      World world = player.getWorld();

      initPlayerStatus(player);

      Bukkit.getScheduler().runTaskTimer(enemyDown, Runnable -> {
        if (gameTime <= 0) {
          Runnable.cancel();
          player.sendMessage("ゲームが終了しました！");
          return;
        }
        world.spawnEntity(getEnemySpawnLocation(player, world), getEnemy());
        gameTime -= 5;
      }, 0, 5 * 20);
    }
    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    Player player = e.getEntity().getKiller();
    if (Objects.isNull(player) || playerScoreList.isEmpty()) {
      return;
    }

    for (PlayerScore playerScore : playerScoreList) {
      if (playerScore.getPlayerName().equals(player.getName())) {
        playerScore.setScore(playerScore.getScore() + 10);
        player.sendMessage("敵を倒した！現在のスコアは" + playerScore.getScore() + "点");
      }
    }
  }

  /**
   * 新規のプレイヤー情報をリストに追加
   *
   * @param player コマンドを実行したプレイヤー情報
   **/
  private void addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore();
    newPlayer.setPlayerName(player.getName());
    playerScoreList.add(newPlayer);
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
