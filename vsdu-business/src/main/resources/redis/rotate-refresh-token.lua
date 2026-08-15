-- KEYS[1] = vsdu:auth:rt:{旧refreshTokenHash}
-- KEYS[2] = vsdu:auth:rt:{新refreshTokenHash}
-- KEYS[3] = vsdu:auth:rt-set:{userId}
--
-- ARGV[1] = oldRefreshTokenHash
-- ARGV[2] = newRefreshTokenHash
-- ARGV[3] = refreshTokenExpireSeconds
-- ARGV[4] = expectedId

local ttl = tonumber(ARGV[3])
local expectedUserId = ARGV[4]
local ownerId = redis.call('GET', KEYS[1])

-- 若旧Refresh Token不存在或已过期
if not ownerId then
    return false
end

if ownerId ~= expectedUserId then
    return false
end

-- 校验newRefreshTokenHash是否已存在
if redis.call('EXISTS', KEYS[2]) == 1 then
    return false
end

-- 消费旧 Token
redis.call('DEL', KEYS[1])
redis.call('SREM', KEYS[3], ARGV[1])

-- 写入新 Token
redis.call('SET', KEYS[2], expectedUserId, 'EX', ttl)
redis.call('SADD', KEYS[3], ARGV[2])
redis.call('EXPIRE', KEYS[3], ttl)

return true