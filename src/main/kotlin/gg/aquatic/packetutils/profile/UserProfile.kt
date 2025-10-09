package gg.aquatic.packetutils.profile

import java.util.UUID

class UserProfile(
    var uuid: UUID,
    var name: String,
    var textureProperties: MutableCollection<TextureProperty>
) {

    class TextureProperty(
        val name: String,
        val value: String,
        val signature: String
    )

}