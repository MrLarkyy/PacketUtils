package gg.aquatic.packetutils.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

fun Event.call() {
    Bukkit.getServer().pluginManager.callEvent(this)
}

fun Listener.unregister() {
    HandlerList.unregisterAll(this)
}