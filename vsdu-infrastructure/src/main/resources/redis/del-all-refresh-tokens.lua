-- KEYS[1] = vsdu:auth:rt-set:{userId}
-- ARGV[1] = vsdu:auth:rt:（refresh token key前缀）

local tokenHashes = redis.call('SMEMBERS', KEYS[1])
for _, tokenHash in ipairs(tokenHashes) do
    redis.call('DEL', ARGV[1] .. tokenHash)
end
redis.call('DEL', KEYS[1])

return #tokenHashes
