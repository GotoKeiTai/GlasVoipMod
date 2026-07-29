require "ISUI/ISPanel"

GlasVoipIndicatorPanel = ISPanel:derive("GlasVoipIndicatorPanel")

local TIER_RADII = { [0] = 3, [1] = 10, [2] = 15 }
local DISPLAY_MS = 1000
local DOT_COUNT = 16
local DOT_SIZE = 4

function GlasVoipIndicatorPanel:render()
    local elapsed = getTimestampMs() - self.startedMs
    if elapsed > self.durationMs then
        self:removeFromUIManager()
        return
    end

    local alpha = 1.0 - (elapsed / self.durationMs)
    local radius = TIER_RADII[self.tier] or TIER_RADII[1]

    for i = 0, DOT_COUNT - 1 do
        local angle = (i / DOT_COUNT) * 2 * math.pi
        local worldX = self.player:getX() + radius * math.cos(angle)
        local worldY = self.player:getY() + radius * math.sin(angle)

        local screenX = isoToScreenX(self.player, worldX, worldY, self.player:getZ())
        local screenY = isoToScreenY(self.player, worldX, worldY, self.player:getZ())

        self:drawRect(screenX - DOT_SIZE / 2, screenY - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE, alpha, 0.78, 0.64, 0.37)
    end
end

function GlasVoipIndicatorPanel:new(player, tier)
    local o = ISPanel:new(0, 0, getCore():getScreenWidth(), getCore():getScreenHeight())
    setmetatable(o, self)
    self.__index = self

    o.player = player
    o.tier = tier
    o.startedMs = getTimestampMs()
    o.durationMs = DISPLAY_MS
    o.moveWithMouse = false
    o.backgroundColor.a = 0

    return o
end

function GlasVoip_showIndicator(player, tier)
    local panel = GlasVoipIndicatorPanel:new(player, tier)
    panel:initialise()
    panel:addToUIManager()
end

GlasVoip = GlasVoip or {}
GlasVoip.showIndicator = GlasVoip_showIndicator
