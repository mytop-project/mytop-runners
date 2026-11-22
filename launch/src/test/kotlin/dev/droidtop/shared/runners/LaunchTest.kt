package dev.droidtop.shared.runners

import dev.droidtop.shared.containers.ContainerArch
import dev.droidtop.shared.containers.Translator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchTest {
    private val arm = ContainerArch.ARM64

    private fun elf(bits: Int, machine: Int): ByteArray = ByteArray(ElfHeader.SIZE).also {
        it[0] = 0x7f; it[1] = 'E'.code.toByte(); it[2] = 'L'.code.toByte(); it[3] = 'F'.code.toByte()
        it[4] = (if (bits == 64) 2 else 1).toByte()
        it[5] = 1.toByte()
        it[18] = (machine and 0xff).toByte(); it[19] = (machine shr 8).toByte()
    }

    @Test
    fun elfHeadersGiveTheProgramsArchitecture() {
        assertEquals(ContainerArch.X86_64, ElfHeader.arch(elf(64, 62)))
        assertEquals(ContainerArch.X86, ElfHeader.arch(elf(32, 3)))
        assertEquals(arm, ElfHeader.arch(elf(64, 183)))
        assertNull(ElfHeader.arch("#!/bin/sh\necho not an elf".toByteArray()))
        assertNull(ElfHeader.arch(ByteArray(4)))
    }

    @Test
    fun aTranslatedCommandPutsTheLauncherInFront() {
        assertEquals(listOf("FEX", "FEXInterpreter"), TranslatedCommand.launchers(Translator.FEX))
        assertEquals(listOf("FEX", "/games/Game/lib/py2-linux-x86_64/Game", "-x"), TranslatedCommand.of("FEX", "/games/Game/lib/py2-linux-x86_64/Game", listOf("-x")))
        assertEquals(listOf("/usr/bin/game"), TranslatedCommand.of(null, "/usr/bin/game"))
    }

    // Projekt: Passion (Ren'Py 7.6.1, the console's SD card, 2026-11-22) ships these folders in lib/.
    private val projektPassion = listOf("py2-linux-i686", "py2-linux-x86_64", "py2-windows-i686", "py2-windows-x86_64", "python2.7")

    @Test
    fun aPcBuildOnArmRunsItsX86_64BinariesThroughTheTranslator() {
        val launch = RenPyLinuxLaunch.choose(projektPassion, arm, Translator.FEX)!!
        assertEquals(ContainerArch.X86_64, launch.arch)
        assertEquals(Translator.FEX, launch.translator)
        assertEquals(mapOf("RENPY_PLATFORM" to "linux-x86_64", "RENPY_GDB" to "FEX"), launch.environment("FEX"))
    }

    @Test
    fun anArmBuildRunsNatively() {
        val launch = RenPyLinuxLaunch.choose(listOf("py3-linux-x86_64", "py3-linux-aarch64"), arm, Translator.FEX)!!
        assertEquals(arm, launch.arch)
        assertNull(launch.translator)
        assertEquals(mapOf("RENPY_PLATFORM" to "linux-aarch64"), launch.environment("FEX"))
    }

    @Test
    fun only32BitBinariesTakeFexEvenWhenTheContainerChoseBox64() {
        val launch = RenPyLinuxLaunch.choose(listOf("py2-linux-i686"), arm, Translator.BOX64)!!
        assertEquals(ContainerArch.X86, launch.arch)
        assertEquals(Translator.FEX, launch.translator)
    }

    @Test
    fun onAnX86DeviceThe64BitBuildRunsNatively() {
        val launch = RenPyLinuxLaunch.choose(projektPassion, ContainerArch.X86_64, Translator.FEX)!!
        assertEquals(ContainerArch.X86_64, launch.arch)
        assertNull(launch.translator)
    }

    @Test
    fun aGameWithNoLinuxBinariesHasNoLaunch() {
        assertNull(RenPyLinuxLaunch.choose(listOf("py2-windows-x86_64", "py2-mac-universal"), arm, Translator.FEX))
        assertNull(RenPyLinuxLaunch.choose(listOf("py3-linux-armv7l"), arm, Translator.FEX))
    }
}
