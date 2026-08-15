-- KEYS[1] = vsdu:auth:rt:{refreshTokenHash}
-- KEYS[2] = vsdu:auth:rt-set:{userId}
--
-- ARGV[1] = userId
-- ARGV[2] = refreshTokenHash
-- ARGV[3] = refreshTokenExpireSeconds

local ttl = tonumber(ARGV[3])
-- 校验refreshTokenHash是否已存在
if redis.call('EXISTS', KEYS[1]) == 1 then
    return false
end

redis.call('SET', KEYS[1], ARGV[1], 'EX', ttl)
redis.call('SADD', KEYS[2], ARGV[2])

-- 每次签发新Token时，刷新集合ttl至最新token过期
redis.call('EXPIRE', KEYS[2], ttl)

return true