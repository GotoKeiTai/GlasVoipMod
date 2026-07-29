GlasVoip = GlasVoip or {}
GlasVoip.Tiers = { WHISPER = 0, TALK = 1, SHOUT = 2 }
GlasVoip.TierNames = { [0] = "Chuchoter", [1] = "Parler", [2] = "Hurler" }
GlasVoip.currentTier = GlasVoip.Tiers.TALK

local KEYBIND_NAME = "Cycle VOIP Tier"

-- Registers the tier-cycle bind in the real Options -> Keybinding menu so players can rebind
-- it like any vanilla key. Must run on OnGameBoot (not at file load time) so the global
-- keyBinding table (media/lua/shared/keyBinding.lua) is already available to insert into.
local function initBinds()
    table.insert(keyBinding, { value = "[Glas VOIP]" })
    table.insert(keyBinding, { value = KEYBIND_NAME, key = Keyboard.KEY_B })
end
Events.OnGameBoot.Add(initBinds)

local function cycleTier()
    local player = getPlayer()
    if not player then return end

    GlasVoip.currentTier = (GlasVoip.currentTier + 1) % 3

    sendClientCommand(player, "GlasVoip", "setTier", { tier = GlasVoip.currentTier })

    if GlasVoip.showIndicator then
        GlasVoip.showIndicator(player, GlasVoip.currentTier)
    end

    if GlasVoip.TierNames[GlasVoip.currentTier] then
        player:Say(GlasVoip.TierNames[GlasVoip.currentTier])
    end
end

local function onKeyStartPressed(key)
    -- Events.OnKeyStartPressed is a raw keyboard event delivered regardless of UI focus --
    -- unlike vanilla actions routed through the keybinding system, it fires even while the
    -- player is typing in chat. Without this guard, every "b" typed in a chat message (e.g.
    -- "brb") would also cycle the tier and fire a network command. ISChat.focused is the same
    -- flag ISChat.lua itself checks for this purpose (confirmed in the vanilla source).
    if ISChat and ISChat.focused then
        return
    end

    if key == getCore():getKey(KEYBIND_NAME) then
        cycleTier()
    end
end

Events.OnKeyStartPressed.Add(onKeyStartPressed)
