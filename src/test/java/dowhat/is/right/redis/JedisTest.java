package dowhat.is.right.redis;

import java.util.Date;
import java.util.Random;
import redis.clients.jedis.Jedis;

/**
 * @author 杨春炼
 * @since 2020-04-06
 */
public class JedisTest {

  private static final String REDIS_HOST = "10.39.146.92";
  private static final Integer mutex = -1;
  private static Jedis jedis = new Jedis(REDIS_HOST, 6379, 100000, 100000);

  private static void connect() {
    if (jedis == null) {
      synchronized (mutex) {
        if (jedis == null) {
          //连接本地的 Redis 服务
          jedis = new Jedis(REDIS_HOST, 6379, 100000);
        }
      }
    }
  }

  private static boolean canDoJob() {
    Boolean exists = jedis.exists("qps1");
    System.out.println("canDoJob:" + exists);
    if (!exists) {
      jedis.set("qps1", "");
      jedis.expire("qps1", 1);
      return true;
    }
    return false;
  }

  public static void main(String[] args) {
    Jedis jedis = new Jedis(REDIS_HOST, 6379, 100000, 100000);
    for (int i = 0; i < 1000; i++) {
      Boolean exists = jedis.exists("qps1");
      if (!exists) {
        jedis.set("qps1", "");
        jedis.expire("qps1", 1);
        long now = new Date().getTime() / 1000;
        System.out.println("now:" + now);
        try {
          int sleep = new Random().nextInt(1000);
          Thread.sleep(sleep);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //  @Test
  public void test() {
    Boolean exists = jedis.exists("qps1");
    System.out.println("canDoJob:" + exists);
    if (!exists) {
      jedis.set("qps1", "");
      jedis.expire("qps1", 5);
    }
    exists = jedis.exists("qps1");
    System.out.println("canDoJob:" + exists);
  }

  private void testZk() {
    Jedis jedis = new Jedis(REDIS_HOST, 6379, 100000, 100000);
    String keyName = "qps1";
    //判断是否存在这个key,如果存在说明有人已经拿到"锁"，如果不存在，自己可以创建这个key，获取到"锁"。
    Boolean exists = jedis.exists(keyName);
    if (!exists) {//如果不存在，才会执行业务逻辑（请求三方接口）
      jedis.set(keyName, "");//不存在的时候，创建这个key
      jedis.expire(keyName, 1);//设置这个key的过期时间为1s，那么1s后这个key会自动消失。此处相当于限流了哈。
      try {
        System.out.println("此处做你正确的请求三方的业务逻辑");
      } catch (Exception e) {
        e.printStackTrace();
        //如果还没到请求三方接口就抛出异常了，那么可以删除key
        jedis.del(keyName);
      }
    } else {//如果存在，说明有机器已经创建了这个key，并且执行了操作。
      System.out.println("在这里处理你的超过限流的逻辑");
    }
  }
}
