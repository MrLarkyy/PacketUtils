package gg.aquatic.packetutils.seat

import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket

interface PacketSeat {

    fun packet(): ClientboundSetPassengersPacket

}