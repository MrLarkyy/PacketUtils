package gg.aquatic.packetutils.event

import gg.aquatic.eventutils.CancellableAquaticEvent
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerGamePacketListener
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class PacketReceiveEvent(
    val player: Player,
    var packet: Packet<ServerGamePacketListener>
): CancellableAquaticEvent(true) {

    val then: CompletableFuture<Unit> = CompletableFuture()
}