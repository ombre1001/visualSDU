-- KEYS[1] = vsdu:auth:rt:{refreshTokenHash}
-- KEYS[2] = vsdu:auth:rt-set:{userId}
--
-- ARGV[1] = expectedUserId
-- ARGV[2] = tokenHash

local ownerId = redis.call('GET', KEYS[1])

if not ownerId then
    return false
end

if ownerId ~= ARGV[1] then
    return false
end

redis.call('DEL', KEYS[1])
redis.call('SREM', KEYS[2], ARGV[2])

-- 顺便删除空集合
if redis.call('SCARD', KEYS[2]) == 0 then
    redis.call('DEL', KEYS[2])
end

return true