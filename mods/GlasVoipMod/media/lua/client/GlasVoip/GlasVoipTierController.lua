GlasVoip = GlasVoip or {}
GlasVoip.Tiers = { WHISPER = 0, TALK = 1, SHOUT = 2 }
GlasVoip.TierNames = { [0] = "Chuchoter", [1] = "Parler", [2] = "Hurler" }
GlasVoip.currentTier = GlasVoip.Tiers.TALK

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
    if key == Keyboard.KEY_B then
        cycleTier()
    end
end

Events.OnKeyStartPressed.Add(onKeyStartPressed)
