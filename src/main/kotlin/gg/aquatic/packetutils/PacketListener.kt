package gg.aquatic.packetutils

import gg.aquatic.eventutils.EventUtils
import gg.aquatic.eventutils.call
import gg.aquatic.eventutils.event
import gg.aquatic.packetutils.event.PacketReceiveEvent
import gg.aquatic.packetutils.event.PacketSendEvent
import gg.aquatic.packetutils.util.ProtectedPacket
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.Connection
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ServerGamePacketListener
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class PacketListener(
    val player: Player
) : ChannelDuplexHandler() {

    companion object {

        fun initialize(plugin: JavaPlugin) {
            if (EventUtils.plugin == null) {
                EventUtils.initialize(plugin)
            }

            event<PlayerJoinEvent> {
                register(it.player)
            }
            event<PlayerQuitEvent> {
                unregister(it.player)
            }
        }


        private val playerConnectionField =
            ReflectionUtils.getField("connection", ServerCommonPacketListenerImpl::class.java)

        fun register(player: Player) {
            val listener = PacketListener(player)

            val craftPlayer = (player as CraftPlayer)
            val connection = playerConnectionField.get(craftPlayer.handle.connection) as Connection
            val pipeline = connection.channel.pipeline()

            for ((_, handler) in pipeline.toMap()) {
                if (handler is Connection) {
                    pipeline.addBefore("packet_handler", "cosmo_packet_listener", listener)
                    break
                }
            }
        }

        fun unregister(player: Player) {
            val craftPlayer = (player as CraftPlayer)
            val connection = playerConnectionField.get(craftPlayer.handle.connection) as Connection
            val channel = connection.channel
            val pipeline = channel.pipeline()
            if (channel != null) {
                try {
                    if (pipeline.names().contains("cosmo_packet_listener")) {
                        pipeline.remove("cosmo_packet_listener")
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg !is Packet<*>) {
            super.channelRead(ctx, msg)
            return
        }

        val event = PacketReceiveEvent(player, msg as Packet<ServerGamePacketListener>)
        event.call()

        if (event.isCancelled) return
        super.channelRead(ctx, msg)
        event.then.complete(null)
    }

    override fun write(ctx: ChannelHandlerContext?, msg: Any?, promise: ChannelPromise?) {
        if (msg is ProtectedPacket) {
            val packet = msg.packet
            super.write(ctx, packet, promise)
            return
        }

        if (msg !is Packet<*>) {
            super.write(ctx, msg, promise)
            return
        }

        val event = PacketSendEvent(player, msg as Packet<ClientGamePacketListener>)
        event.call()

        if (event.isCancelled) return
        super.write(ctx, msg, promise)
        event.then.complete(null)
    }


}