local function onClientCommand(module, command, player, args)
    if module ~= "GlasVoip" or command ~= "setTier" then
        return
    end

    local onlinePlayers = getOnlinePlayers()
    for i = 0, onlinePlayers:size() - 1 do
        sendServerCommand(onlinePlayers:get(i), "GlasVoip", "tierChanged", {
            onlineId = player:getOnlineID(),
            tier = args.tier,
        })
    end
end

Events.OnClientCommand.Add(onClientCommand)
