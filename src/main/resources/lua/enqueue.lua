-- enqueue.lua
-- 대기 토큰 진입 (원자적)
--
-- 흐름:
--   1. 역인덱스 (SETNX) 로 중복 진입 방지
--   2. Score 생성 (epoch_milli * 1000000 + INCR) — tie-breaker
--   3. Sorted Set 추가 (ZADD)
--   4. Hash 메타 저장 (HSET) + TTL (EXPIRE)
--
-- KEYS:
--   [1] userProgramKey   (queue:user:{userId}:program:{programId})
--   [2] programKey       (queue:program:{programId})
--   [3] tokenKey         (queue:token:{tokenId})
--   [4] seqKey           (queue:seq:{programId})
--
-- ARGV:
--   [1] tokenId
--   [2] userId
--   [3] programId
--   [4] issuedAtEpochMilli
--   [5] status
--   [6] ttlSeconds
--
-- 반환:
--   1 = 성공
--   0 = 이미 존재 (중복 진입)

local userProgramKey = KEYS[1]
local programKey = KEYS[2]
local tokenKey = KEYS[3]
local seqKey = KEYS[4]

local tokenId = ARGV[1]
local userId = ARGV[2]
local programId = ARGV[3]
local issuedAtEpochMilli = tonumber(ARGV[4])
local status = ARGV[5]
local ttlSeconds = tonumber(ARGV[6])

-- 1. 역인덱스 (SETNX) — 같은 user+program 토큰 이미 있으면 실패
local acquired = redis.call('SET', userProgramKey, tokenId, 'NX', 'EX', ttlSeconds)
if not acquired then
    return 0
end

-- 2. tie-breaker score 생성 (epoch_milli * 1000000 + 시퀀스)
local seq = redis.call('INCR', seqKey)
redis.call('EXPIRE', seqKey, ttlSeconds)
local issuedAtEpochSecond = math.floor(issuedAtEpochMilli / 1000)
local score = issuedAtEpochSecond * 1000000 + seq

-- 3. Sorted Set 추가
redis.call('ZADD', programKey, score, tokenId)

-- 4. Hash 메타 저장 + TTL
redis.call('HSET', tokenKey,
    'userId', userId,
    'programId', programId,
    'issuedAt', tostring(issuedAtEpochMilli),
    'status', status)
redis.call('EXPIRE', tokenKey, ttlSeconds)

return 1
