-- delete.lua
-- 대기 토큰 삭제 (원자적)
--
-- 흐름:
--   1. 역인덱스 값 검증 (compare-and-delete)
--      — 다른 토큰이 차지했으면 역인덱스 보존 (orphan 방어)
--   2. Sorted Set 멤버 제거 (ZREM)
--   3. Hash 키 삭제 (DEL)
--
-- KEYS:
--   [1] userProgramKey
--   [2] programKey
--   [3] tokenKey
--
-- ARGV:
--   [1] tokenId
--
-- 반환:
--   1 = 역인덱스도 삭제됨
--   2 = 역인덱스는 보존 (다른 토큰이 차지)

local userProgramKey = KEYS[1]
local programKey = KEYS[2]
local tokenKey = KEYS[3]

local tokenId = ARGV[1]

-- 1. 역인덱스 compare-and-delete
local current = redis.call('GET', userProgramKey)
local indexDeleted = 0
if current == tokenId then
    redis.call('DEL', userProgramKey)
    indexDeleted = 1
end

-- 2. Sorted Set 제거
redis.call('ZREM', programKey, tokenId)

-- 3. Hash 삭제
redis.call('DEL', tokenKey)

if indexDeleted == 1 then
    return 1
else
    return 2
end
