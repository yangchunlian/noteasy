package dowhat.is.right.redis;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;

/**
 * @author 杨春炼
 * @since 2020-04-23
 */
public class RateLimiter {

  private void handle() {
    Redisson redisson = null;
    boolean needRate = true;
    while (true) {
      RRateLimiter rateLimiter = redisson.getRateLimiter("myRateLimiter");
      Stopwatch watch;
      if (needRate) {
        boolean b = rateLimiter
            .trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS);//保证rate
        if (b) {
          watch = Stopwatch.createStarted();
          needRate = customLogic(redisson, watch);
        }
      } else {
        watch = Stopwatch.createStarted();
        needRate = customLogic(redisson, watch);
      }
    }
  }

  private boolean customLogic(Redisson redisson, Stopwatch watch) {
    RLock result = redisson.getLock("handle result?");//验证上一条消息是否处理完
    try {
      boolean tryLock = result.tryLock(1, TimeUnit.MINUTES);//一条消息的最大处理时间，根据自己需求设置
      if (tryLock) {
        String mqMsg = "";//主动拿mq一条消息
        try {
          result.lock();
          System.out.println("handle this mqMsg~" + mqMsg);
        } catch (Exception e) {
          e.printStackTrace();
          //业务处理异常
        } finally {
          result.unlock();
        }
      } else {
        //上一条消息处理异常，做业务处理
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
      //业务处理异常
    }
    watch.stop();
    long duration = watch.elapsed(TimeUnit.SECONDS);
    return duration < 1;
  }

  public static void main(String[] args) {
    Redisson redisson = null;
    String msg = "同步拿到mq消息";
    RBlockingQueue<String> queue = redisson.getBlockingQueue("queue");
    queue.add(msg);//redis queue 保证fifo
    while (true) {
      RRateLimiter rateLimiter = redisson.getRateLimiter("myRateLimiter");
      boolean b = rateLimiter.trySetRate(RateType.OVERALL, 1, 1, RateIntervalUnit.SECONDS);//保证rate
      if (b) {
        RLock result = redisson.getLock("handle result?");//验证上一条消息是否处理完
        try {
          boolean tryLock = result.tryLock(1, TimeUnit.MINUTES);//一条消息的最大处理时间
          if (tryLock) {
            try {
              result.lock();
              String take = queue.take();
              System.out.println("handle this message~" + take);
            } catch (InterruptedException e) {
              e.printStackTrace();
              //业务处理异常
            } finally {
              result.unlock();
            }
          } else {
            //上一条消息处理异常，做业务处理
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
          //业务处理异常
        }
      }
    }
  }
}
