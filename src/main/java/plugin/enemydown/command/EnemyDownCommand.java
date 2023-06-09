package plugin.enemydown.command;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SplittableRandom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
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
import org.bukkit.potion.PotionEffect;
import plugin.enemydown.EnemyDown;
import plugin.enemydown.PlayerScoreData;
import plugin.enemydown.data.ExecutingPlayer;
import plugin.enemydown.mapper.data.PlayerScore;

/**
 * 制限時間内にランダムで出現する敵を倒して、スコアを獲得するゲームを起動するコマンドです。 敵によってスコアは変わり、倒せた敵の合計によってスコアが加算されます。
 * 結果はプレイヤー名、点数、日時などで保存されます。
 **/
public class EnemyDownCommand extends BaseCommand implements Listener {

  public static final int GAME_TIME = 20;

  public static final String EASY = "easy";
  public static final String NORMAL = "normal";
  public static final String HARD = "hard";
  public static final String NONE = "none";
  public static final String LIST = "list";
  private final EnemyDown enemyDown;

  private final PlayerScoreData playerScoreData = new PlayerScoreData();

  private final List<ExecutingPlayer> executingPlayerList = new ArrayList<>();
  private final List<Entity> spanEntityList = new ArrayList<>();


  public EnemyDownCommand(EnemyDown enemyDown) {
    this.enemyDown = enemyDown;
  }

  @Override
  public boolean onExecutePlayerCommand(Player player, Command command, String label,
      String[] args) {
    //最初の引数が『List』だったらスコア一覧を表示して処理を終了する。
    if (args.length == 1 && LIST.equals(args[0])) {
      sendPlayerScoreList(player);
      return false;
    }
    String difficulty = getDifficulty(player, args);
    if (difficulty.equals(NONE)) {
      return false;
    }

    ExecutingPlayer nowExecutingPlayer = getPlayerScore(player);

    initPlayerStatus(player, difficulty);

    gamePlay(player, nowExecutingPlayer, difficulty);
    return true;
  }

  @Override
  public boolean onExecuteNPCCommand(CommandSender sender, Command command, String label,
      String[] args) {

    return false;
  }

  /**
   * 現在登録されているスコア一覧をメッセージに送る
   *
   * @param player プレイヤー
   */
  private void sendPlayerScoreList(Player player) {
    List<PlayerScore> playerScoreList = playerScoreData.selectList();
    for (PlayerScore playerScore : playerScoreList) {
      player.sendMessage(playerScore.getId() + " | "
          + playerScore.getPlayerName() + " | "
          + playerScore.getScore() + " | "
          + playerScore.getDifficulty() + " | "
          + playerScore.getRegisteredAt()
          .format(DateTimeFormatter.ofPattern("yyy-MM-dd HH:mm:ss")));
    }
  }

  /**
   * 難易度をコマンド引数から取得します。
   *
   * @param player 　コマンドを実行したプレイヤー
   * @param args   　コマンド引数
   * @return 難易度
   */
  String getDifficulty(Player player, String[] args) {
    if (args.length == 1 &&
        (EASY.equals(args[0]) || NORMAL.equals(args[0]) || HARD.equals(args[0]))) {
      return args[0];
    }
    player.sendMessage(
        ChatColor.RED + "実行できません。コマンド引数の１つ目に難易度指定が必要です。[easy, normal, hard]");
    return NONE;
  }

  @EventHandler
  public void onEnemyDeath(EntityDeathEvent e) {
    LivingEntity enemy = e.getEntity();
    Player player = enemy.getKiller();

    if (Objects.isNull(player) || spanEntityList.stream()
        .noneMatch(entity -> entity.equals(enemy))) {
      return;
    }

    executingPlayerList.stream()
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
  private ExecutingPlayer getPlayerScore(Player player) {
    ExecutingPlayer executingPlayer = new ExecutingPlayer(player.getName());

    if (executingPlayerList.isEmpty()) {
      executingPlayer = addNewPlayer(player);
    } else {
      executingPlayer = executingPlayerList.stream().findFirst()
          .map(ps -> ps.getPlayerName().equals(player.getName())
              ? ps
              : addNewPlayer(player)).orElse(executingPlayer);
    }

    executingPlayer.setGameTime(GAME_TIME);
    executingPlayer.setScore(0);
    removePotionEffect(player);
    return executingPlayer;
  }


  /**
   * 新規のプレイヤー情報をリストに追加
   *
   * @param player コマンドを実行したプレイヤー情報
   * @return 新規プレイヤー
   **/
  private ExecutingPlayer addNewPlayer(Player player) {
    ExecutingPlayer newPlayer = new ExecutingPlayer(player.getName());
    executingPlayerList.add(newPlayer);
    return newPlayer;
  }

  /**
   * ゲームを始める前にプレーヤーの状態を設定 体力・空腹度を最大値にし、装備はネザライト一式に変更。
   *
   * @param player
   */
  private void initPlayerStatus(Player player, String difficulty) {
    player.setHealth(20);
    player.setFoodLevel(20);

    switch (difficulty) {
      case EASY -> {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.IRON_HELMET));
        inventory.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.IRON_BOOTS));
        inventory.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
      }
      case NORMAL -> {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        inventory.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        inventory.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
      }
      case HARD -> {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.NETHERITE_HELMET));
        inventory.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        inventory.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        inventory.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        inventory.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
      }
    }
  }

  /**
   * ゲームを実行します。 規定の時間内に敵を倒すとスコアが加算されます。合計スコアを時間経過後に表示します。
   *
   * @param player             　コマンドを実行したプレイヤー
   * @param nowExecutingPlayer 　プレイヤースコア情報
   * @param difficulty         難易度
   **/
  private void gamePlay(Player player, ExecutingPlayer nowExecutingPlayer, String difficulty) {
    Bukkit.getScheduler().runTaskTimer(enemyDown, Runnable -> {
      if (nowExecutingPlayer.getGameTime() <= 0) {
        Runnable.cancel();

        player.sendTitle("ゲームが終了しました",
            nowExecutingPlayer.getPlayerName() + "合計" + nowExecutingPlayer.getScore() + "点!",
            0, 60, 0);

        spanEntityList.forEach(Entity::remove);
        spanEntityList.clear();

        removePotionEffect(player);

        playerScoreData.insert(
            new PlayerScore(nowExecutingPlayer.getPlayerName()
                , nowExecutingPlayer.getScore()
                , difficulty));

        return;
      }
      Entity spawnEntity = player.getWorld()
          .spawnEntity(getEnemySpawnLocation(player), getEnemy(difficulty));
      spanEntityList.add(spawnEntity);
      nowExecutingPlayer.setGameTime(nowExecutingPlayer.getGameTime() - 5);
    }, 0, 5 * 20);
  }

  /**
   * 敵の出現エリアを取得
   *
   * @param player 　コマンドを実行したプレイヤー
   * @return 敵の出現場所
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
   * @param difficulty 難易度
   * @return 敵
   */
  private EntityType getEnemy(String difficulty) {
    List<EntityType> enemyList = switch (difficulty) {
      case NORMAL -> List.of(EntityType.ZOMBIE, EntityType.SKELETON);
      case HARD -> List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WITCH);
      default -> List.of(EntityType.ZOMBIE);
    };
    return enemyList.get(new SplittableRandom().nextInt(enemyList.size()));
  }

  /**
   * プレイヤーに設定されている特殊効果を除外します。
   *
   * @param player 　コマンド実行者
   */
  private void removePotionEffect(Player player) {
    player.getActivePotionEffects().stream()
        .map(PotionEffect::getType)
        .forEach(player::removePotionEffect);
  }
}


