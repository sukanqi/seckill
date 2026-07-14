-- seckill.lua
-- Atomic stock deduction script for flash sale
-- KEYS[1] = Redis key for stock (e.g., "seckill:stock:123")
-- ARGV[1] = decrement amount (e.g., "1")
--
-- Returns:
--   -1 if insufficient stock
--   remaining stock count after decrement if successful

local stockKey = KEYS[1]
local decrementBy = tonumber(ARGV[1])

if decrementBy == nil or decrementBy <= 0 then
    return -1
end

-- Get current stock
local currentStock = redis.call('GET', stockKey)

-- If key doesn't exist, stock is not initialized
if currentStock == false then
    return -1
end

currentStock = tonumber(currentStock)

-- Check if sufficient stock is available
if currentStock < decrementBy then
    return -1
end

-- Perform atomic decrement using DECRBY
local remaining = redis.call('DECRBY', stockKey, decrementBy)

return remaining
