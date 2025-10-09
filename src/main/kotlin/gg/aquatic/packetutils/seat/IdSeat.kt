package gg.aquatic.packetutils.seat

import gg.aquatic.packetutils.Packet
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket

class IdSeat(val entityId: Int, passenger: Int): PacketSeat {
    val packet = Packet.setPassengers(entityId, intArrayOf(passenger))

    override fun packet(): ClientboundSetPassengersPacket {
        return packet
    }
}