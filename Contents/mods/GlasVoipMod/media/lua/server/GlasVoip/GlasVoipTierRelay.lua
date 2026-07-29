-- Mirrors GlasVoip.Tiers from the client-side GlasVoipTierController.lua (0=WHISPER,
-- 1=TALK, 2=SHOUT). Can't reference that table directly: client/ and server/ Lua files run
-- in separate Lua environments in PZ, so this range is duplicated here deliberately -- keep
-- both in sync if a tier is ever added or removed.
local MIN_TIER = 0
local MAX_TIER = 2

local function onClientCommand(module, command, player, args)
    if module ~= "GlasVoip" or command ~= "setTier" then
        return
    end

    -- args and args.tier are attacker/bug-controlled (any client can send arbitrary command
    -- args). An out-of-range tier relayed as-is would make TierPatch.apply() on the Java side
    -- throw and get caught every single frame on every connected client, once broadcast --
    -- rejecting client-side only (as before this fix) is too late, since by then it's already
    -- fanned out to everyone. Drop invalid requests here instead of relaying them.
    local tier = args and args.tier
    if type(tier) ~= "number" or tier < MIN_TIER or tier > MAX_TIER then
        return
    end

    local onlinePlayers = getOnlinePlayers()
    for i = 0, onlinePlayers:size() - 1 do
        sendServerCommand(onlinePlayers:get(i), "GlasVoip", "tierChanged", {
            onlineId = player:getOnlineID(),
            tier = tier,
        })
    end
end

Events.OnClientCommand.Add(onClientCommand)
