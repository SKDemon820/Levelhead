package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.gson
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.config.MasterConfig
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.ChatDisplay
import club.sk1er.mods.levelhead.display.TabDisplay
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.essential.api.utils.Multithreading
import net.minecraft.entity.player.EntityPlayer
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class DisplayManager(val file: File) {

    var config = MasterConfig()
    val aboveHead: MutableList<AboveHeadDisplay> = ArrayList()
    var chat: ChatDisplay? = null
        private set
    var tab: TabDisplay? = null
        private set

    init {
        this.readConfig()
    }

    fun readConfig() {
        try {
            val source = jsonParser.parse(FileUtils.readFileToString(file)).asJsonObject
            if (source.has("master")) this.config = gson.fromJson(source["master"].asJsonObject, MasterConfig::class.java)

            for (head in source["head"].asJsonArray) {
                aboveHead.add(AboveHeadDisplay(gson.fromJson(head.asJsonObject, DisplayConfig::class.java)))
            }

            if (aboveHead.isEmpty()) {
                aboveHead.add(AboveHeadDisplay(DisplayConfig()))
            }

            if (source.has("chat"))
                this.chat = ChatDisplay(gson.fromJson(source["chat"].asJsonObject, DisplayConfig::class.java))
            else
                this.chat = ChatDisplay(DisplayConfig().also { it.type = "GUILD_NAME" })

            if (source.has("tab"))
                this.tab = TabDisplay(gson.fromJson(source["tab"].asJsonObject, DisplayConfig::class.java))
            else
                this.tab = TabDisplay(DisplayConfig().also { it.type = "QUESTS" })

            adjustIndices()

        } catch (e: IOException) {
            Levelhead.logger.error("Failed to initialize display manager.", e)
        }
    }

    fun saveConfig() {
        val jsonObject = JsonObject()

        jsonObject.addProperty("master", gson.toJson(config))
        jsonObject.addProperty("tab", gson.toJson(tab?.config))
        jsonObject.addProperty("chat", gson.toJson(chat?.config))

        val head = JsonArray()

        this.aboveHead.forEach { display ->
            head.add(gson.toJson(display.config))
        }

        jsonObject.add("head", head)

        try {
            FileUtils.writeStringToFile(file, jsonObject.toString(), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            Levelhead.logger.error("Failed to write to config.", e)
        }
    }

    fun adjustIndices() {
        for (i in aboveHead.indices) {
            aboveHead[i].bottomValue = i == 0
            aboveHead[i].index = i
        }
    }

    fun joinWorld() {
        Multithreading.runAsync(Levelhead::refreshPurchaseStates)
        aboveHead.forEach { head ->
            head.joinWorld()
        }
        chat?.joinWorld()
        tab?.joinWorld()
    }

    fun playerJoin(player: EntityPlayer) {
        aboveHead.forEach { head ->
            head.playerJoin(player)
        }
        chat?.playerJoin(player)
        tab?.playerJoin(player)
    }

    fun checkCacheSizes() {
        aboveHead.forEach { head ->
            head.checkCacheSize()
        }
        chat?.checkCacheSize()
        tab?.checkCacheSize()
    }
}