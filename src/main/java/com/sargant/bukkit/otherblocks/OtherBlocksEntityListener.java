// OtherBlocks - a Bukkit plugin
// Copyright (C) 2011 Robert Sargant
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package com.sargant.bukkit.otherblocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.material.Colorable;

import com.sargant.bukkit.common.*;

public class OtherBlocksEntityListener extends EntityListener
{	
	private OtherBlocks parent;
	
	public OtherBlocksEntityListener(OtherBlocks instance)
	{
		parent = instance;
	}
	
	@Override
	public void onEntityDamage(EntityDamageEvent event) {
	    
	    // Ignore if a player
	    if(event.getEntity() instanceof Player) return;
		
	    // Check if the damager is a player - if so, weapon is the held tool
		if(event instanceof EntityDamageByEntityEvent) {
		    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
		    if(e.getDamager() instanceof Player) {
		        Player damager = (Player) e.getDamager();
		        parent.damagerList.put(event.getEntity(), damager.getItemInHand().getType().toString());
		        return;
		    } else {
		        CreatureType attacker = CommonEntity.getCreatureType(e.getDamager());
		        if(attacker != null) {
		            parent.damagerList.put(event.getEntity(), "CREATURE_" + attacker.toString());
		            return;
		        }
		    }
		}
		
		// Damager was not a person - switch through damage types
		switch(event.getCause()) {
		    case FIRE:
		    case FIRE_TICK:
		    case LAVA:
		        parent.damagerList.put(event.getEntity(), "DAMAGE_FIRE");
		        break;
		        
		    case ENTITY_ATTACK:
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
		    case CONTACT:
		    case DROWNING:
		    case FALL:
		    case SUFFOCATION:
		        parent.damagerList.put(event.getEntity(), "DAMAGE_" + event.getCause().toString());
		        break;
		        
		    case CUSTOM:
		    case VOID:
		    default:
		        parent.damagerList.remove(event.getEntity());
		        break;
		}
	}
	
	@Override
	public void onEntityDeath(EntityDeathEvent event)
	{
		// At the moment, we only track creatures killed by humans
	    if(event.getEntity() instanceof Player) return;
	    
	    // If there's no damage record, ignore
		if(!parent.damagerList.containsKey(event.getEntity())) return;
		
		String weapon = parent.damagerList.get(event.getEntity());
		Entity victim = event.getEntity();
		CreatureType victimType = CommonEntity.getCreatureType(victim);
		
		parent.damagerList.remove(event.getEntity());
		
		// If victimType == null, the mob type has not been recognized (new, probably)
		// Stop here and do not attempt to process
		if(victimType == null) return;
		
		Location location = victim.getLocation();
		List<OtherBlocksContainer> drops = new ArrayList<OtherBlocksContainer>();
		boolean doDefaultDrop = false;
		
		for(OtherBlocksContainer obc : parent.transformList) {
			
		    Short dataVal = (victim instanceof Colorable) ? ((short) ((Colorable) victim).getColor().getData()) : null;
			
		    if(!obc.compareTo(
		            "CREATURE_" + victimType.toString(), 
		            dataVal,
		            weapon,
		            victim.getWorld().getName())) {
		        
		        continue;
		    }

			// Check probability is great than the RNG
			if(parent.rng.nextDouble() > (obc.chance.doubleValue()/100)) continue;

			if(obc.dropped.equalsIgnoreCase("DEFAULT")) {
			    doDefaultDrop = true;
			} else {
			    drops.add(obc);
			}
		}
		
		// Now do the drops
		if(drops.size() > 0 && doDefaultDrop == false) event.getDrops().clear();
        for(OtherBlocksContainer obc : drops) OtherBlocks.performDrop(location, obc);
	}
}

