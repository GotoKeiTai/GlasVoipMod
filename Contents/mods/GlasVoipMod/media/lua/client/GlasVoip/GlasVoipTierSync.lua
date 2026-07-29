GlasVoipTiers = GlasVoipTiers or {}

local function onServerCommand(module, command, args)
    if module ~= "GlasVoip" or command ~= "tierChanged" then
        return
    end

    GlasVoipTiers[args.onlineId] = args.tier
end

Events.OnServerCommand.Add(onServerCommand)
