package gg.aquatic.packetutils

import com.mojang.authlib.GameProfile
import gg.aquatic.packetutils.seat.PacketSeat
import gg.aquatic.packetutils.util.sendPacket
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ClientInformation
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.Mob
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.*
import kotlin.jvm.optionals.getOrNull

class PacketEntity(
    location: Location,
    val entityId: Int,
    val instance: Entity,
    var spawnPacket: Packet<ClientGamePacketListener>,
    var updatePacket: ClientboundSetEntityDataPacket? = null,
    var passengerPacket: ClientboundSetPassengersPacket? = null,
    var seat: PacketSeat? = null,
) {

    companion object {
        fun create(location: Location, entityType: EntityType, uuid: UUID? = null): PacketEntity? {
            val nmsEntityType =
                net.minecraft.world.entity.EntityType.byString(entityType.name.lowercase()).getOrNull() ?: return null

            val worldServer = (location.world as CraftWorld).handle
            val entity =
                createEntity(
                    nmsEntityType,
                    uuid,
                    worldServer,
                    BlockPos(location.blockX, location.blockY, location.blockZ)
                )
                    ?: return null

            entity.setPos(location.x, location.y, location.z)
            entity.setRot(location.yaw, location.pitch)
            entity.yHeadRot = location.yaw

            val trackedEntity = worldServer.chunkSource.chunkMap.TrackedEntity(
                entity,
                50,
                50,
                true
            )
            val tracker = ServerEntity(
                worldServer,
                entity,
                entity.type.updateInterval(),
                true,
                trackedEntity,
                HashSet(),
            )
            return PacketEntity(
                location,
                entity.id,
                entity,
                entity.getAddEntityPacket(tracker),
            )
        }

        private fun <T : Entity> createEntity(
            entityType: net.minecraft.world.entity.EntityType<T>,
            uuid: UUID?,
            worldServer: ServerLevel,
            blockPos: BlockPos,
        ): T? {
            val entity = if (entityType == net.minecraft.world.entity.EntityType.PLAYER) {
                val server = (Bukkit.getServer() as CraftServer).server
                val serverPlayer = ServerPlayer(
                    server,
                    worldServer,
                    GameProfile(uuid ?: UUID.randomUUID(), "Player"),
                    ClientInformation.createDefault()
                )
                serverPlayer
            } else {
                entityType.create(worldServer, EntitySpawnReason.COMMAND)
            }
            entity?.let {
                it.setPos(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
                it.setRot(0.0f, 0.0f)

                if (uuid != null) {
                    it.uuid = uuid
                }

                if (it is Mob) {
                    it.yHeadRot = it.yRot
                    it.yBodyRot = it.yRot
                }
            }
            return entity as T?
        }
    }

    var location: Location = location
        private set

    fun sendSpawnComplete(vararg players: Player) {
        val packets = ArrayList<Packet<ClientGamePacketListener>>()
        packets.add(spawnPacket)
        updatePacket?.let { packets.add(it) }
        passengerPacket?.let { packets.add(it) }
        seat?.let { packets.add(it.packet()) }

        val bundle = ClientboundBundlePacket(packets)
        for (player in players) {
            player.sendPacket(bundle)
        }
    }

    fun teleport(location: Location, vararg players: Player) {
        setLocation(location)
        val packet = gg.aquatic.packetutils.Packet.teleport(entityId, location)
        for (player in players) {
            player.sendPacket(packet)
        }
    }

    fun setLocation(location: Location) {
        this.location = location
        instance.setPos(location.x, location.y, location.z)
        instance.setRot(location.yaw, location.pitch)
        val worldServer = (location.world as CraftWorld).handle
        val trackedEntity = worldServer.chunkSource.chunkMap.TrackedEntity(
            instance,
            50,
            50,
            true
        )

        val packet = instance.getAddEntityPacket(
            ServerEntity(
                worldServer,
                instance,
                instance.type.updateInterval(),
                true,
                trackedEntity,
                HashSet()
            )
        )
        spawnPacket = packet
    }

    fun destroy(vararg players: Player) {
        val packet = ClientboundRemoveEntitiesPacket(entityId)
        for (player in players) {
            player.sendPacket(packet)
        }
    }
}