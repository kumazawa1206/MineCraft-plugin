package plugin.enemydown.command;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import plugin.enemydown.EnemyDown;

class EnemyDownCommandTest {

  EnemyDownCommand sut;

  @Mock
  EnemyDown enemyDown;

  @Mock
  Player player;

  @BeforeEach
  void before() {
    sut = new EnemyDownCommand(enemyDown);
  }

  @Test
  void getDifficultyに渡す引数のargsの最初の文字列がeasyの時にeasyの文字列が返ること() {
    String actual = sut.getDifficulty(player, new String[]{"easy"});
    Assertions.assertEquals("easy", actual);
  }

  @Test
  void getDifficultyに渡す引数のargsの最初の文字列がnormalの時にnormalの文字列が返ること() {
    String actual = sut.getDifficulty(player, new String[]{"normal"});
    Assertions.assertEquals("normal", actual);
  }
}