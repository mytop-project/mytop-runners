package dev.droidtop.shared.runners

import dev.droidtop.shared.containers.ContainerArch
import dev.droidtop.shared.containers.Translator

/**
 * How a Ren'Py game's own Linux launcher (`<Game>.sh`) is started in a container (droidtop docs/SPEC.md 3c).
 *
 * The launcher picks `lib/<python>-<platform>` from `uname -m`, which on an arm64 device finds nothing in a PC build:
 * those ship `py2-linux-x86_64` and `py2-linux-i686` (Ren'Py 7) or `py3-linux-x86_64` (Ren'Py 8), and only some
 * builds add `linux-aarch64`. The launcher itself takes two variables, read from Ren'Py 7.6.1's script:
 * `RENPY_PLATFORM` names the folder, and `RENPY_GDB` is a command it puts in front of the game's binary
 * (`exec $RENPY_GDB "$LIB/$BASEFILE"`). So droidtop runs the game's own script, choosing the folder from what the game
 * ships and putting the translator in `RENPY_GDB`; it never rewrites or reimplements the launcher.
 */
data class RenPyLinuxLaunch(
    /** The architecture of the binaries it runs. */
    val arch: ContainerArch,
    /** The translator they run through, or null when they run natively. */
    val translator: Translator?,
    /** `RENPY_PLATFORM`, and `RENPY_GDB` when [translator] is set (to the launcher the caller found in the container). */
    val platform: String,
) {
    /** The variables for the game's script, with [launcher] (one of [TranslatedCommand.launchers]) for a translator. */
    fun environment(launcher: String?): Map<String, String> =
        buildMap {
            put("RENPY_PLATFORM", platform)
            if (translator != null && launcher != null) put("RENPY_GDB", launcher)
        }

    companion object {
        private val FOLDER = Regex("^py[23]-linux-([a-z0-9_]+)$")

        /** Ren'Py's names for the architectures it builds Linux binaries for. */
        private fun archOf(suffix: String): ContainerArch? = when (suffix) {
            "x86_64" -> ContainerArch.X86_64
            "i686" -> ContainerArch.X86
            "aarch64" -> ContainerArch.ARM64
            else -> null
        }

        /**
         * The launch for a game whose `lib/` holds [libFolders] (folder names only), on [host] with the container's
         * [chosen] translator. Native binaries come first; otherwise the 64-bit x86 ones, then the 32-bit ones,
         * through the translator that covers them. Null when the game ships no Linux binaries droidtop can run.
         */
        fun choose(libFolders: Collection<String>, host: ContainerArch, chosen: Translator?): RenPyLinuxLaunch? {
            val shipped = libFolders.mapNotNull { name ->
                FOLDER.matchEntire(name)?.groupValues?.get(1)?.let { suffix -> archOf(suffix)?.let { it to suffix } }
            }.distinctBy { it.first }
            val order = listOf(host, ContainerArch.X86_64, ContainerArch.X86, ContainerArch.ARM64).distinct()
            for (arch in order) {
                val suffix = shipped.firstOrNull { it.first == arch }?.second ?: continue
                if (arch.runsNativelyOn(host)) return RenPyLinuxLaunch(arch, null, "linux-$suffix")
                val translator = Translator.forProgram(arch, host, chosen) ?: continue
                return RenPyLinuxLaunch(arch, translator, "linux-$suffix")
            }
            return null
        }
    }
}
