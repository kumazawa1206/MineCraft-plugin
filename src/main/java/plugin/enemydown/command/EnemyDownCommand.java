package plugin.enemydown.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import plugin.enemydown.EnemyDown;
import plugin.enemydown.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンドです。 敵によってスコアは変わり、倒せた敵の合計によってスコアが加算されます。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 **/
public class EnemyDownCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 20;
  private EnemyDown enemyDown;
  private List<PlayerScore> playerScoreList = new ArrayList<>();
  private List<Entity> spanEntityList = new ArrayList<>();


  public EnemyDownCommand(EnemyDown enemyDown) {
    this.enemyDown = enemyDown;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player) {
    PlayerScore nowPlayerScore = getPlayerScore(player);

    initPlayerStatus(player);

    gamePlay(player, nowPlayerScore);
    return true;
  }


  @Override
  public boolean onExecuteNPCCommand(CommandSender sender) {
    return false;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();

    if (Objects.isNull(player) || spanEntityList.stream()
        .noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    playerScoreList.stream()
        .filter(p -> p.getPlayerName().equals(player.getName()))
        .findFirst()
        .ifPresent(p -> {
          int point = switch (enemy.getType()) {
            case ZOMBIE -> 10;
            case SKELETON, WITCH -> 20;
            default -> 0;
          };

          p.setScore(p.getScore() + point);
          player.sendMessage("敵を倒した！現在のスコアは" + p.getScore() + "点");

        });
  }

  /**
   * 現在実行しているプレーヤーの情報を取得する。
   *
   * @param player コマンドを実行したプレイヤー
   * @return 現在実行しているプレイヤーのスコア情報
   **/
  private PlayerScore getPlayerScore(Player player) {
    PlayerScore playerScore = new PlayerScore(player.getName());

    if (playerScoreList.isEmpty()) {
      playerScore = addNewPlayer(player);
    } else {
      playerScore = playerScoreList.stream().findFirst()
          .map(ps -> ps.getPlayerName().equals(player.getName())
              ? ps
              : addNewPlayer(player)).orElse(playerScore);
    }

    playerScore.setGameTime(GAME_TIME);
    playerScore.setScore(0);
    return playerScore;
  }

  /**
   * 新規のプレイヤー情報をリストに追加
   *
   * @param player コマンドを実行したプレイヤー情報
   * @return 新規プレイヤー
   **/
  private PlayerScore addNewPlayer(Player player) {
    PlayerScore newPlayer = new PlayerScore(player.getName());
    playerScoreList.add(newPlayer);
    return newPlayer;
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
   * ゲームを実行します。 規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   *
   * @param player         　コマンドを実行したプレイヤー
   * @param nowPlayerScore 　プレイヤースコア情報
   **/
  private void gamePlay(Player player, PlayerScore nowPlayerScore) {
    Bukkit.getScheduler().runTaskTimer(enemyDown, Runnable -> {
      if (nowPlayerScore.getGameTime() <= 0) {
        Runnable.cancel();

        player.sendTitle("ゲームが終了しました",
            nowPlayerScore.getPlayerName() + "合計" + nowPlayerScore.getScore() + "点!",
            0, 60, 0);

        spanEntityList.forEach(Entity::remove);
        spanEntityList = new ArrayList<>();
        return;
      }
      Entity spawnEntity = player.getWorld().spawnEntity(getEnemySpawnLocation(player), getEnemy());
      spanEntityList.add(spawnEntity);
      nowPlayerScore.setGameTime(nowPlayerScore.getGameTime() - 5);
    }, 0, 5 * 20);
  }

  /**
   * 敵の出現エリアを取得
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return　敵の出現場所
   */

  private Location getEnemySpawnLocation(Player player) {
    Location playerlocation = player.getLocation();
    int randomX = new SplittableRandom().nextInt(10) - 8;
    int randomZ = new SplittableRandom().nextInt(10) - 8;

    double x = playerlocation.getX() + randomX;
    double y = playerlocation.getY();
    double Z = playerlocation.getZ() + randomZ;

    return new Location(player.getWorld(), (x), y, (Z));
  }

  /**
   * ランダムで敵を出現させる
   *
   * @return　敵
   */
  private EntityType getEnemy() {
    List<EntityType> enemyList = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
    return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));
  }
}
