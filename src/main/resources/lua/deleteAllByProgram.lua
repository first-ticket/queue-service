-- deleteAllByProgram.lua
-- 프로그램 취소 시 해당 프로그램의 모든 토큰 일괄 삭제 (원자적)
--
-- 흐름:
--   1. 프로그램 단위 토큰 인덱스 (Set) 의 멤버 조회 (SMEMBERS)
--   2. 각 토큰별 compare-and-delete:
--      - 역인덱스 (현재 값 일치 시만 DEL)
--      - Hash DEL
--   3. Sorted Set 자체 + seqKey + 프로그램 단위 인덱스 일괄 DEL
--
-- SCAN 사용 X — 프로그램 단위 인덱스로 토큰 범위 제한 (성능 + 블로킹 회피)
--
-- KEYS:
--   [1] programKey         (queue:program:{programId})
--   [2] seqKey             (queue:seq:{programId})
--   [3] programTokensKey   (queue:program:{programId}:tokens)
--
-- ARGV:
--   [1] programIdStr
--   [2] tokenKeyPrefix       (queue:token:)
--   [3] userProgramKeyPrefix (queue:user:)
--   [4] programKeyInfix      (:program:)
--
-- 반환: 처리된 토큰 수

local programKey = KEYS[1]
local seqKey = KEYS[2]
local programTokensKey = KEYS[3]

local programIdStr = ARGV[1]
local tokenKeyPrefix = ARGV[2]
local userProgramKeyPrefix = ARGV[3]
local programKeyInfix = ARGV[4]

-- 1. 프로그램 단위 토큰 인덱스 조회 (SCAN X)
local tokenIds = redis.call('SMEMBERS', programTokensKey)
local processedCount = 0

-- 2. 각 토큰별 정리
for i, tokenIdStr in ipairs(tokenIds) do
    local tokenKey = tokenKeyPrefix .. tokenIdStr
    local userId = redis.call('HGET', tokenKey, 'userId')

    -- 역인덱스 compare-and-delete
    if userId then
        local userProgramKey = userProgramKeyPrefix .. userId .. programKeyInfix .. programIdStr
        local currentTokenIdInIndex = redis.call('GET', userProgramKey)
        if currentTokenIdInIndex == tokenIdStr then
            redis.call('DEL', userProgramKey)
        end
    end

    -- Hash 삭제
    redis.call('DEL', tokenKey)

    processedCount = processedCount + 1
end

-- 3. Sorted Set 자체 + seqKey + 프로그램 단위 인덱스 일괄 삭제
redis.call('DEL', programKey)
redis.call('DEL', seqKey)
redis.call('DEL', programTokensKey)

return processedCount
