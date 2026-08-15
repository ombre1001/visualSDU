package cn.sduonline.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 祖传redis操作类
 * &#064;Author Sure
 */
@Component
@RequiredArgsConstructor
public class RedisClient {

    private final StringRedisTemplate redis;

    /* ------------ 通用 key 操作 ------------ */

    /**
     * expire {key} {ttl}
     * 设置key过期时间
     */
    public boolean expire(String key, Duration ttl) {
        Boolean flag = redis.expire(key, ttl);
        return flag != null && flag;
    }

    /**
     * ttl {key}
     * 查看key剩余时间
     * @return
     *      >= 0：剩余秒数
     *      -1：存在但无过期时间
     *      -2：key不存在（从未存在/已过期/被删除）
     */
    public long ttl(String key) {
        Long sec = redis.getExpire(key);
        return sec == null ? -2 : sec;
    }

    /**
     * del {key}
     * 删除key
     */
    public boolean del(String key) {
        Boolean flag = redis.delete(key);
        return flag != null && flag;
    }

    /* String 操作 */

    /**
     * set {key} {value}
     */
    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    /**
     * set {key} {value} ex {ttl}
     * @param ttl Duration.ofSeconds()/.ofMinutes()/.ofDays()/...
     */
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    /**
     * set {key} {value} nx
     * 只有 key 不存在时才设置成功（原子操作）
     */
    public boolean setIfAbsent(String key, String value) {
        Boolean flag = redis.opsForValue().setIfAbsent(key, value);
        return flag != null && flag;
    }

    /**
     * set {key} {value} nx ex {ttl}
     * 只有 key 不存在时才设置成功（原子操作）
     */
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean flag = redis.opsForValue().setIfAbsent(key, value, ttl);
        return flag != null && flag;
    }

    /**
     * get {key}
     * @return value，key不存在或已过期则返回null
     */
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /**
     * getdel {key}
     * 获取key对应value后删除之（原子操作）
     * @return value, key不存在或已过期则返回null
     */
    public String getdel(String key) {
        return redis.opsForValue().getAndDelete(key);
    }

    /**
     * exists {key}
     * @return
     *      true：存在
     *      false：不存在/已过期
     * flag != null && flag 用于防止flag为null是自动拆箱导致的NPE
     */
    public boolean exists(String key) {
        Boolean flag = redis.hasKey(key);
        return flag != null && flag;
    }

    /* ------------ Counter 操作 ------------ */

    public long incr(String key) {
        Long v = redis.opsForValue().increment(key);
        return v == null ? 0 : v;
    }

    public long decr(String key) {
        Long v = redis.opsForValue().decrement(key);
        return v == null ? 0 : v;
    }

    public long incrBy(String key, long delta) {
        Long v = redis.opsForValue().increment(key, delta);
        return v == null ? 0 : v;
    }

    public long decrBy(String key, long delta) {
        Long v = redis.opsForValue().decrement(key, delta);
        return v == null ? 0 : v;
    }

    public long getCounter(String key) {
        String v = redis.opsForValue().get(key);
        return v == null ? 0 : Long.parseLong(v);
    }

    private static final DefaultRedisScript<Long> INCR_WITH_TTL_LUA = new DefaultRedisScript<>(
            """
                local v = redis.call('INCR', KEYS[1])
                if v == 1 then
                  redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return v
            """,
            Long.class
    );

    public long incrWithTtl(String key, Duration ttl) {
        return redis.execute(INCR_WITH_TTL_LUA, List.of(key), String.valueOf(ttl.toSeconds()));
    }

    /* ------------ Hash 操作 ------------ */

    /**
     * hset {key} {field} {value}
     */
    public void hset(String key, String field, String value) {
        redis.opsForHash().put(key, field, value);
    }

    /**
     * hset {key} {field1} {value1} {field2} {value2} ...
     * @param m 其中的键值对将会分别以field value的形式存入key
     */
    public void hset(String key, Map<String, String> m) {
        redis.opsForHash().putAll(key, m);
    }

    /**
     * hget {key} {field}
     * @return key中field对应值，未查找到则返回null
     */
    public String hget(String key, String field) {
        Object v = redis.opsForHash().get(key, field);
        return v == null ? null : v.toString();
    }

    /**
     * hdel {key} {field1} {field2} ...
     * @return 成功删除的 field 的数量。
     */
    public long hdel(String key, String... fields) {
        Long n = redis.opsForHash().delete(key, (Object[]) fields);
        return n == null ? 0 : n;
    }

    /* ------------ Set 操作 ------------ */

    /**
     * sadd {key} {member1} {member2} ...
     * @return 成功加入的 member 的数量
     */
    public long sadd(String key, String... members) {
        Long n = redis.opsForSet().add(key, members);
        return n == null ? 0 : n;
    }

    /**
     * scard {key}
     * @return 集合中的元素数目。0说明key不存在
     */
    public long scard(String key) {
        Long n = redis.opsForSet().size(key);
        return n == null ? 0 : n;
    }

    /**
     * sismember key member
     * 判断member是否为 key的成员之一
     * @return true-是，false-否/不存在key集合
     */
    public boolean sismember(String key, String member) {
        Boolean flag = redis.opsForSet().isMember(key, member);
        return flag != null && flag;
    }

    /**
     * srem {key} {member1} {member2} ...
     * @return 成功删除的成员数量
     */
    public long srem(String key, String... members) {
        Long n = redis.opsForSet().remove(key, (Object[]) members);
        return n == null ? 0 : n;
    }

    /**
     * smembers {key}
     * @return 集合 key 中的所有成员
     * 注意：smembers仅用于小集合！
     * smembers时间复杂度为 O(N)，且会将整个集合拉入JVM。若集合过大，可能导致性能下降或栈溢出。
     */
    public Set<String> smembers(String key) {
        return redis.opsForSet().members(key);
    }

    /* ------------ 通用Lua脚本执行 ------------ */

    public <T> T execute(
            RedisScript<T> script,
            List<String> keys,
            String... args
    ) {
        return redis.execute(script, keys, (Object[]) args);
    }

}