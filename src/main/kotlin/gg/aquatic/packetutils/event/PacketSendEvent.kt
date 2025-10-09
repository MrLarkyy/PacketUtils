package gg.aquatic.packetutils.event

import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

class PacketSendEvent(
    val player: Player,
    var packet: Packet<ClientGamePacketListener>
): CancellableAquaticEvent(true) {

    val then: CompletableFuture<Unit> = CompletableFuture()

}