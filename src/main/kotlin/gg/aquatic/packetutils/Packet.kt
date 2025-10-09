package gg.aquatic.packetutils

import com.google.common.collect.LinkedHashMultimap
import com.google.common.hash.HashCode
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import gg.aquatic.packetutils.profile.ProfileEntry
import gg.aquatic.packetutils.util.EntityContainer
import gg.aquatic.packetutils.util.ProtectedPacket
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.papermc.paper.adventure.AdventureComponent
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.RegistryAccess
import net.minecraft.network.Connection
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.RegistryOps
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.util.HashOps
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.level.GameType
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.PalettedContainerFactory
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftMenuType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.absoluteValue

object Packet {

    private val propertiesMapField = ReflectionUtils.getField("properties", PropertyMap::class.java).apply {
        this.isAccessible = true
    }

    private val entriesField =
        ReflectionUtils.getField("entries", ClientboundPlayerInfoUpdatePacket::class.java).apply {
            this.isAccessible = true
        }

    fun closeInventory(id: Int): ClientboundContainerClosePacket {
        return ClientboundContainerClosePacket(id)
    }

    fun updatePlayerInfo(actionId: Int, profileEntry: ProfileEntry): ClientboundPlayerInfoUpdatePacket {
        return updatePlayerInfo(listOf(actionId), listOf(profileEntry))
    }

    fun updatePlayerInfo(
        actionIds: Collection<Int>,
        profileEntries: Collection<ProfileEntry>,
    ): ClientboundPlayerInfoUpdatePacket {
        val entries = ArrayList<ClientboundPlayerInfoUpdatePacket.Entry>()

        entries += profileEntries.map { profileEntry ->
            ClientboundPlayerInfoUpdatePacket.Entry(
                profileEntry.userProfile.uuid,
                GameProfile(profileEntry.userProfile.uuid, profileEntry.userProfile.name).apply {
                    val multiMap = LinkedHashMultimap.create<String, Property>()
                    for (property in profileEntry.userProfile.textureProperties) {
                        multiMap.put("textures", Property(property.name, property.value, property.signature))
                    }
                    propertiesMapField.set(properties, multiMap)
                },
                profileEntry.listed,
                profileEntry.latency,
                GameType.entries[profileEntry.gameMode.ordinal],
                profileEntry.displayName?.toNMSComponent(),
                profileEntry.showHat,
                profileEntry.listOrder,
                null
            )
        }

        val packet = ClientboundPlayerInfoUpdatePacket(
            EnumSet.copyOf(actionIds.map { ClientboundPlayerInfoUpdatePacket.Action.entries[it] }.toMutableList()),
            mutableListOf<ServerPlayer>()
        )
        entriesField.set(packet, entries)
        return packet
    }

    fun changeGameState(action: ClientboundGameEventPacket.Type, value: Float): ClientboundGameEventPacket {
        val packet = ClientboundGameEventPacket(action, value)
        return packet
    }

    private val cameraPacketConstructor =
        ClientboundSetCameraPacket::class.java.getDeclaredConstructor(FriendlyByteBuf::class.java).apply {
            isAccessible = true
        }

    fun setCamera(entityId: Int): ClientboundSetCameraPacket {
        val bytebuf = FriendlyByteBuf(Unpooled.buffer())
        bytebuf.writeVarInt(entityId)
        return cameraPacketConstructor.newInstance(bytebuf)
    }

    fun setBlockChange(location: Location, blockState: BlockData): ClientboundBlockUpdatePacket {
        val packet = ClientboundBlockUpdatePacket(
            BlockPos(location.blockX, location.blockY, location.blockZ),
            (blockState as CraftBlockData).state
        )
        return packet
    }

    fun teleport(entityId: Int, location: Location): ClientboundEntityPositionSyncPacket {
        val container = EntityContainer(entityId)
        container.setPosRaw(location.x, location.y, location.z)
        container.setRot(location.yaw, location.pitch)
        return ClientboundEntityPositionSyncPacket(entityId, PositionMoveRotation.of(container), false)
    }

    fun setSlot(containerId: Int, stateId: Int, slot: Int, item: ItemStack?): ClientboundContainerSetSlotPacket {
        return setSlot(containerId, stateId, slot, item?.asNMS())
    }

    fun setEntityData(entityId: Int, data: List<SynchedEntityData.DataValue<*>>): ClientboundSetEntityDataPacket {
        return ClientboundSetEntityDataPacket(entityId, data)
    }

    private val setPassengersConstructor =
        ClientboundSetPassengersPacket::class.java.getDeclaredConstructor(FriendlyByteBuf::class.java).apply {
            isAccessible = true
        }

    fun setPassengers(entityId: Int, ids: IntArray): ClientboundSetPassengersPacket {
        val byteBuf = FriendlyByteBuf(Unpooled.buffer())
        byteBuf.writeVarInt(entityId)
        byteBuf.writeVarIntArray(ids)
        return setPassengersConstructor.newInstance(byteBuf)
    }

    fun setSlot(
        containerId: Int,
        stateId: Int,
        slot: Int,
        item: net.minecraft.world.item.ItemStack?
    ): ClientboundContainerSetSlotPacket {
        return ClientboundContainerSetSlotPacket(
            containerId,
            stateId,
            slot,
            item ?: net.minecraft.world.item.ItemStack.EMPTY
        )
    }

    fun setContent(
        containerId: Int,
        stateId: Int,
        items: List<ItemStack?>,
        cursor: ItemStack?
    ): ClientboundContainerSetContentPacket {
        return setContent(containerId, stateId, items.map { it?.asNMS() }, cursor?.asNMS())
    }

    fun setContent(
        containerId: Int,
        stateId: Int,
        items: List<net.minecraft.world.item.ItemStack?>,
        cursor: net.minecraft.world.item.ItemStack?
    ): ClientboundContainerSetContentPacket {
        return ClientboundContainerSetContentPacket(
            containerId,
            stateId,
            items.map { it ?: net.minecraft.world.item.ItemStack.EMPTY },
            cursor ?: net.minecraft.world.item.ItemStack.EMPTY
        )
    }

    fun openContainer(
        containerId: Int,
        menuType: org.bukkit.inventory.MenuType,
        title: net.kyori.adventure.text.Component
    ): ClientboundOpenScreenPacket {
        val nmsType = CraftMenuType.bukkitToMinecraft(menuType)
        val nmsTitle = title.toNMSComponent()
        return openContainer(containerId, nmsType, nmsTitle)
    }

    fun openContainer(containerId: Int, menuType: MenuType<*>, title: Component): ClientboundOpenScreenPacket {
        return ClientboundOpenScreenPacket(containerId, menuType, title)
    }

    fun getWindowClick(
        containerId: Int,
        stateId: Int,
        slotId: Int,
        buttonNum: Int,
        clickTypeNum: Int,
        carriedItem: net.minecraft.world.item.ItemStack?,
        changedSlots: Map<Int, net.minecraft.world.item.ItemStack?>,
        vararg players: Player
    ) {
        getWindowClick(
            containerId,
            stateId,
            slotId,
            buttonNum,
            clickTypeNum,
            carriedItem?.toHashed(),
            changedSlots,
            *players
        )
    }

    fun getWindowClick(
        containerId: Int,
        stateId: Int,
        slotId: Int,
        buttonNum: Int,
        clickTypeNum: Int,
        carriedItem: HashedStack?,
        changedSlots: Map<Int, net.minecraft.world.item.ItemStack?>,
        vararg players: Player
    ) {
        val map = Int2ObjectOpenHashMap<HashedStack>()
        changedSlots.forEach { (key, value) ->
            val nmsItem = value ?: net.minecraft.world.item.ItemStack.EMPTY
            map[key] = nmsItem.toHashed()
        }

        getWindowClick(containerId, stateId, slotId, buttonNum, clickTypeNum, carriedItem, map, *players)
    }

    fun getWindowClick(
        containerId: Int,
        stateId: Int,
        slotId: Int,
        buttonNum: Int,
        clickTypeNum: Int,
        carriedItem: HashedStack?,
        changedSlots: Int2ObjectMap<HashedStack>,
        vararg players: Player
    ) {
        val packet = ServerboundContainerClickPacket(
            containerId,
            stateId,
            slotId.toShort(),
            buttonNum.toByte(),
            ClickType.entries[clickTypeNum],
            changedSlots,
            carriedItem ?: HashedStack.EMPTY
        )

        (Bukkit.getServer() as CraftServer).handle.server.scheduleOnMain {
            for (player in players) {
                (player as CraftPlayer).handle.connection.handleContainerClick(packet)
            }
        }
    }

    interface WrappedChunkSection {

        fun set(x: Int, y: Int, z: Int, blockState: BlockData)
        fun get(x: Int, y: Int, z: Int): BlockData

    }

    private val chunkDataBufferField =
        ReflectionUtils.getField("buffer", ClientboundLevelChunkPacketData::class.java).apply {
            isAccessible = true
        }

    private val palletedContainerFactory: PalettedContainerFactory by lazy {
        PalettedContainerFactory.create((Bukkit.getServer() as CraftServer).handle.server.registryAccess())
    }

    private val blockStatePallete: PalettedContainer<BlockState> by lazy {
        palletedContainerFactory.createForBlockStates()
    }
    private val biomePallete: PalettedContainer<Holder<Biome>> by lazy {
        palletedContainerFactory.createForBiomes()
    }

    fun modifyChunkPacketBlocks(world: World, packet: Any, func: (List<WrappedChunkSection>) -> Unit) {
        val sections = (world.minHeight.absoluteValue + world.maxHeight) shr 4
        val chunkBundlePacket = packet as ClientboundLevelChunkWithLightPacket
        val chunkData = chunkBundlePacket.chunkData
        val readBuffer = chunkData.readBuffer

        val wrappedSections = mutableListOf<Pair<WrappedChunkSection, LevelChunkSection>>()

        for (i in 0 until sections) {
            val section = LevelChunkSection(blockStatePallete, biomePallete)
            section.read(readBuffer)
            val pair = ((object : WrappedChunkSection {
                override fun set(x: Int, y: Int, z: Int, blockState: BlockData) {
                    section.setBlockState(x, y, z, (blockState as CraftBlockData).state, false)
                    //palettedContainer.set(x, y, z, (blockState as CraftBlockState).handle)
                }

                override fun get(x: Int, y: Int, z: Int): BlockData {
                    val state = CraftBlockData.fromData(section.getBlockState(x, y, z))
                    return state
                }
            } as WrappedChunkSection) to section)
            wrappedSections.add(pair)
        }
        func(wrappedSections.map { it.first })

        val bytes = ByteArray(calculateChunkSize(wrappedSections.map { it.second }))
        val writeBuffer: ByteBuf = Unpooled.wrappedBuffer(bytes)
        writeBuffer.writerIndex(0)

        extractChunkData(wrappedSections.map { it.second }, writeBuffer)
        chunkDataBufferField.set(chunkData, bytes)
    }

    private fun calculateChunkSize(sections: Collection<LevelChunkSection>): Int {
        var i = 0

        for (levelChunkSection in sections) {
            i += levelChunkSection.serializedSize
        }

        return i
    }

    private fun extractChunkData(sections: Collection<LevelChunkSection>, wrapper: ByteBuf) {
        var chunkSectionIndex = 0

        val buffer = FriendlyByteBuf(wrapper)

        for (levelChunkSection in sections) {
            levelChunkSection.write(buffer, null, chunkSectionIndex)
            ++chunkSectionIndex
        }
    }

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

    fun net.kyori.adventure.text.Component.toNMSComponent(): AdventureComponent {
        return AdventureComponent(this)
    }

    fun ItemStack.asNMS(): net.minecraft.world.item.ItemStack {
        return CraftItemStack.asNMSCopy(this)
    }

    fun HashedStack.asItem(): ItemStack? {
        val actualItem = this as? HashedStack.ActualItem ?: return null
        val type = actualItem.item.registeredName
        actualItem.components.addedComponents

        var item = NamespacedKey.fromString(type)?.let { typeKey ->
            Registry.ITEM.get(typeKey)
        }?.createItemStack(actualItem.count)

        if (item != null) {
            if (item.type == Material.AIR) return null
            val nmsItem = CraftItemStack.asNMSCopy(item)
            nmsItem.applyComponents(nmsItem.components)
            item = CraftItemStack.asBukkitCopy(nmsItem)
        }
        return item
    }

    private val registryAccess: RegistryAccess = (Bukkit.getWorlds().first() as CraftWorld).handle.registryAccess()
    private val registryOps: RegistryOps<HashCode> = registryAccess.createSerializationContext(HashOps.CRC32C_INSTANCE);
    private val hashOpsGenerator: HashedPatchMap.HashGenerator = HashedPatchMap.HashGenerator { typedDataComponent ->
        typedDataComponent.encodeValue(registryOps).getOrThrow { string ->
            IllegalArgumentException("Failed to hash $typedDataComponent: $string")
        }.asInt()
    }

    fun net.minecraft.world.item.ItemStack.toHashed(): HashedStack {
        return HashedStack.create(this, hashOpsGenerator)
    }
}