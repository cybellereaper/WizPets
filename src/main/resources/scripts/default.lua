particleSequence("summon_glow", {
    repeats = 2,
    loopDelay = 5,
    frames = {
        { delayTicks = 0, particle = "ENCHANTMENT_TABLE", offset = { 0.0, 0.5, 0.0 }, count = 24, spread = { 0.4, 0.2, 0.4 }, speed = 0.01 },
        { delayTicks = 4, particle = "END_ROD", offset = { 0.0, 1.0, 0.0 }, count = 12, spread = { 0.1, 0.25, 0.1 }, speed = 0.05 },
        { delayTicks = 6, particle = "SPELL_WITCH", offset = { 0.0, 0.8, 0.0 }, count = 8, spread = { 0.3, 0.3, 0.3 }, speed = 0.02 }
    }
})

raycastAnimation("arcane_bolt", {
    particle = "END_ROD",
    step = 0.45,
    maxDistance = 18.0,
    periodTicks = 1,
    count = 3,
    spread = { 0.0, 0.0, 0.0 },
    speed = 0.01,
    hitRadius = 0.6
})

areaEffect("hatchling_heal", {
    particle = "HEART",
    radius = 2.5,
    pointsPerLayer = 20,
    layers = 2,
    layerSpacing = 0.45,
    durationTicks = 40,
    intervalTicks = 5,
    count = 3,
    spread = { 0.1, 0.1, 0.1 },
    speed = 0.02,
    affectEntities = false
})

petBehavior("default", {
    onSummon = function(ctx)
        ctx:debug("Default Lua behavior summoned.")
        ctx:playSequence("summon_glow", ctx:getLocation())
        ctx:healOwner(4.0)
    end,
    tick = function(ctx)
        local owner = ctx:getOwner()
        if owner:getTicksLived() % 40 == 0 then
            ctx:playAreaEffect("hatchling_heal", ctx:getLocation())
        end
    end,
    onAttack = function(ctx, target, damage)
        ctx:debug("Striking " .. target:getName() .. " for " .. string.format("%.1f", damage))
        ctx:playRaycast("arcane_bolt", ctx:getLocation(), ctx:getOwner():getLocation():getDirection())
    end,
    onDismiss = function(ctx)
        ctx:debug("Default Lua behavior dismissed.")
    end,
    onRaycastHit = function(ctx, entity)
        ctx:debug("Raycast clipped " .. entity:getName())
    end,
    onAreaHit = function(ctx, entity)
        if entity ~= ctx:getOwner() then
            ctx:debug("Area effect brushed " .. entity:getName())
        end
    end
})
