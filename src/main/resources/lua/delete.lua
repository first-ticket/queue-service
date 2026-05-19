-- delete.lua
-- 대기 토큰 삭제 (원자적)
--
-- 흐름:
--   1. 역인덱스 compare-and-delete
--   2. Sorted Set 멤버 제거 (ZREM)
--   3. Hash 키 삭제 (DEL)
--   4. 프로그램 단위 토큰 인덱스에서 제거 (SREM)
--
-- KEYS:
--   [1] userProgramKey
--   [2] programKey
--   [3] tokenKey
--   [4] programTokensKey   ← 새 본문
--
-- ARGV:
--   [1] tokenId

local userProgramKey = KEYS[1]
local programKey = KEYS[2]
local tokenKey = KEYS[3]
local programTokensKey = KEYS[4]

local tokenId = ARGV[1]

-- 1. 역인덱스 compare-and-delete
local current = redis.call('GET', userProgramKey)
local indexDeleted = 0
if current == tokenId then
    redis.call('DEL', userProgramKey)
    indexDeleted = 1
end

-- 2. Sorted Set 멤버 제거
redis.call('ZREM', programKey, tokenId)

-- 3. Hash 삭제
redis.call('DEL', tokenKey)

-- 4. 프로그램 단위 토큰 인덱스에서 제거
redis.call('SREM', programTokensKey, tokenId)

if indexDeleted == 1 then
    return 1
else
    return 2
end
