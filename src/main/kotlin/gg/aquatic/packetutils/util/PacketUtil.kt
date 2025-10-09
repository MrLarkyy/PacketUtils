package gg.aquatic.packetutils.util

import gg.aquatic.packetutils.ReflectionUtils
import io.papermc.paper.adventure.AdventureComponent
import net.kyori.adventure.text.Component
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

private val playerConnectionField =
    ReflectionUtils.getField("connection", ServerCommonPacketListenerImpl::class.java)

fun Player.sendPacket(packet: Packet<ClientGamePacketListener>, silent: Boolean = false) {
    val playerConnection = (this as CraftPlayer).handle.connection
    if (silent) {
        val protected = ProtectedPacket(packet)
        val connection = playerConnectionField.get(playerConnection) as Connection
        connection.channel.pipeline().write(protected)
        return
    }

    playerConnection.send(packet)
}

fun Component.toNMSComponent(): AdventureComponent {
    return AdventureComponent(this)
}