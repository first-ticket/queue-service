-- deleteAllByProgram.lua
-- 프로그램 취소 시 해당 프로그램의 모든 토큰 일괄 삭제 (원자적)
--
-- 흐름:
--   1. SCAN 으로 모든 token Hash 키 찾기 (queue:token:*)
--   2. 각 Hash 의 programId 필드 확인
--   3. 일치하면 compare-and-delete:
--      - 역인덱스 (token 의 userId 기반): 현재 값이 이 토큰 ID 일 때만 DEL
--      - Sorted Set 멤버 ZREM
--      - Hash DEL
--   4. 마지막에 Sorted Set 자체 + seqKey 일괄 DEL
--
-- KEYS:
--   [1] programKey  (queue:program:{programId})
--   [2] seqKey      (queue:seq:{programId})
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

local programIdStr = ARGV[1]
local tokenKeyPrefix = ARGV[2]
local userProgramKeyPrefix = ARGV[3]
local programKeyInfix = ARGV[4]

local processedCount = 0
local cursor = "0"

repeat
    -- 1. SCAN 으로 token Hash 키 페이지 조회
    local result = redis.call('SCAN', cursor, 'MATCH', tokenKeyPrefix .. '*', 'COUNT', 100)
    cursor = result[1]
    local tokenKeys = result[2]

    -- 2. 각 토큰 처리
    for i, tokenKey in ipairs(tokenKeys) do
        local tokenProgramId = redis.call('HGET', tokenKey, 'programId')

        if tokenProgramId == programIdStr then
            local userId = redis.call('HGET', tokenKey, 'userId')
            local tokenIdStr = string.sub(tokenKey, string.len(tokenKeyPrefix) + 1)

            -- 3. compare-and-delete (역인덱스)
            if userId then
                local userProgramKey = userProgramKeyPrefix .. userId .. programKeyInfix .. programIdStr
                local currentTokenIdInIndex = redis.call('GET', userProgramKey)
                if currentTokenIdInIndex == tokenIdStr then
                    redis.call('DEL', userProgramKey)
                end
            end

            -- 4. Sorted Set 멤버 제거 + Hash 삭제
            redis.call('ZREM', programKey, tokenIdStr)
            redis.call('DEL', tokenKey)

            processedCount = processedCount + 1
        end
    end
until cursor == "0"

-- 5. Sorted Set 자체 + seqKey 일괄 삭제
redis.call('DEL', programKey)
redis.call('DEL', seqKey)

return processedCount
