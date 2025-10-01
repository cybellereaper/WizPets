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

pet({
    id = "default",
    displayName = function(ctx)
        return ctx:getOwner():getName() .. "'s Arcane Familiar"
    end,
    entity = {
        type = "FOX",
        invisible = true,
        silent = true,
        followSpeed = 1.3,
        followStart = 2.5,
        followStop = 1.25,
        leashDistance = 32.0,
        mount = true,
        flight = true
    },
    stats = {
        health = 44.0,
        attack = 6.5,
        defense = 3.5,
        magic = 8.0
    },
    behavior = {
        onSummon = function(ctx)
            ctx:debug("Default Lua pet summoned.")
            ctx:playSequence("summon_glow", ctx:getLocation())
            ctx:healOwner(4.0)
        end,
        tick = function(ctx)
            local owner = ctx:getOwner()
            if owner:getTicksLived() % 40 == 0 then
                ctx:playAreaEffect("hatchling_heal", ctx:getLocation())
            end
        end,
        onDismiss = function(ctx)
            ctx:debug("Default Lua pet dismissed.")
        end,
        onRaycastHit = function(ctx, entity)
            ctx:debug("Arcane bolt brushed " .. entity:getName())
        end
    },
    moves = {
        {
            name = "Arcane Bolt",
            cooldown = 40,
            range = 12.0,
            execute = function(ctx, target)
                local direction = ctx:directionTo(target)
                ctx:playRaycast("arcane_bolt", ctx:getLocation(), direction)
                ctx:attack(target, ctx:stat("magic") * 1.35)
            end
        },
        {
            name = "Healing Pulse",
            cooldown = 80,
            range = 6.0,
            select = function(ctx)
                local owner = ctx:getOwner()
                if owner:getHealth() < owner:getMaxHealth() then
                    return owner
                end
            end,
            execute = function(ctx, target)
                if target == ctx:getOwner() then
                    ctx:healOwner(3.5)
                    ctx:playAreaEffect("hatchling_heal", target:getLocation())
                end
            end
        }
    }
})
