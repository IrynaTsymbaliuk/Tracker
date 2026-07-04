package com.tracker.core.config

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class KnownAppsManifestTest {

    @Test
    fun manifestQueries_includeEveryKnownAppPackage_andNoUnexpectedPackages() {
        val knownPackages = buildSet {
            addAll(KnownApps.languageLearning.keys)
            addAll(KnownApps.reading.keys)
            addAll(KnownApps.socialMedia.keys)
            addAll(KnownApps.meditation.keys)
        }
        val expectedNonCatalogQueries = setOf("com.google.android.apps.healthdata")
        val manifestPackages = manifestQueryPackages()

        val missingKnownPackages = knownPackages - manifestPackages
        val unexpectedPackages = manifestPackages - knownPackages - expectedNonCatalogQueries

        assertTrue("Missing manifest <queries> packages: $missingKnownPackages", missingKnownPackages.isEmpty())
        assertTrue("Unexpected manifest <queries> packages: $unexpectedPackages", unexpectedPackages.isEmpty())
    }

    private fun manifestQueryPackages(): Set<String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(manifestFile())
        val nodes = document.getElementsByTagName("package")
        return buildSet {
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                element.getAttribute("android:name")
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
    }

    private fun manifestFile(): File {
        val candidates = listOf(
            File("core/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Could not locate core AndroidManifest.xml from ${File(".").absolutePath}")
    }
}
