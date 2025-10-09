package gg.aquatic.packetutils

import gg.aquatic.packetutils.Packet.asNMS
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import org.bukkit.Color
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.TextDisplay
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f

object PacketEntityData {

    fun displayTeleportInterpolation(value: Int) =
        SynchedEntityData.DataValue(10, EntityDataSerializers.INT, value)

    fun displayInterpolationDelay(value: Int) =
        SynchedEntityData.DataValue(8, EntityDataSerializers.INT, value)

    fun displayInterpolationDuration(value: Int) =
        SynchedEntityData.DataValue(9, EntityDataSerializers.INT, value)

    fun displayLineWidth(value: Int) =
        SynchedEntityData.DataValue(24, EntityDataSerializers.INT, value)

    fun displayBillboard(billboard: Display.Billboard) =
        SynchedEntityData.DataValue(15, EntityDataSerializers.BYTE, billboard.ordinal.toByte())

    fun displayItemTransform(transform: ItemDisplay.ItemDisplayTransform) =
        SynchedEntityData.DataValue(24, EntityDataSerializers.BYTE, transform.ordinal.toByte())

    fun displayItem(itemStack: ItemStack) =
        SynchedEntityData.DataValue(23, EntityDataSerializers.ITEM_STACK, itemStack.asNMS())

    fun displayTextFlags(
        alignment: TextDisplay.TextAlignment,
        hasShadow: Boolean,
        isSeeThrough: Boolean,
        useDefaultBackground: Boolean
    ) =
        displayTextFlags(packFlags(alignment, hasShadow, isSeeThrough, useDefaultBackground))

    private fun packFlags(
        alignment: TextDisplay.TextAlignment, hasShadow: Boolean, isSeeThrough: Boolean, useDefaultBackground: Boolean
    ): Byte {
        var result: Byte = 0

        // Set boolean flags using specific bit masks
        if (hasShadow) result = (0 or 0x01).toByte()
        if (isSeeThrough) result = (result.toInt() or 0x02).toByte()
        if (useDefaultBackground) result = (result.toInt() or 0x04).toByte()

        // Set enum ordinal in bits 3-7
        // We shift the ordinal left by 3 bits to position it correctly (starting at bit mask 0x08)
        result = (result.toInt() or (alignment.ordinal shl 3)).toByte()

        return result
    }

    fun displayTextFlags(packFlags: Byte) =
        SynchedEntityData.DataValue(27, EntityDataSerializers.BYTE, packFlags)

    fun displayTranslation(vector3f: Vector3f) =
        SynchedEntityData.DataValue(
            11,
            EntityDataSerializers.VECTOR3,
            vector3f
        )

    fun displayBackgroundColor(color: Color) =
        SynchedEntityData.DataValue(25, EntityDataSerializers.INT, color.asARGB())

    fun displayBackgroundColor(value: Int) =
        SynchedEntityData.DataValue(25, EntityDataSerializers.INT, value)

    fun displayScale(value: Float) =
        SynchedEntityData.DataValue(12, EntityDataSerializers.VECTOR3, Vector3f(value))

    fun displayText(component: Component) = SynchedEntityData.DataValue(
        23,
        EntityDataSerializers.COMPONENT,
        component
    )
}