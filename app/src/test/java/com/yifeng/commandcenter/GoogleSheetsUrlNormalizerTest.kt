package com.yifeng.commandcenter

import com.yifeng.commandcenter.metrics.GoogleSheetsUrlNormalizer
import com.yifeng.commandcenter.metrics.SheetUrlResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSheetsUrlNormalizerTest {

    @Test
    fun normalizesStandardEditDriveLink() {
        val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(
            "https://docs.google.com/spreadsheets/d/1AQQ95oxftH8W5IC4rUSUkg8ItYqS8tF7i4WBFTUehnQ/edit?usp=drive_link",
        )
        assertEquals(
            SheetUrlResult.Valid(
                "https://docs.google.com/spreadsheets/d/1AQQ95oxftH8W5IC4rUSUkg8ItYqS8tF7i4WBFTUehnQ/export?format=csv",
            ),
            result,
        )
    }

    @Test
    fun normalizesEditLinkWithGidFragment() {
        val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(
            "https://docs.google.com/spreadsheets/d/abc123_XYZ/edit#gid=987654",
        )
        assertEquals(
            SheetUrlResult.Valid(
                "https://docs.google.com/spreadsheets/d/abc123_XYZ/export?format=csv&gid=987654",
            ),
            result,
        )
    }

    @Test
    fun normalizesPublishedPubHtmlLinkToCsv() {
        val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vTestPubId/pubhtml?gid=123&single=true",
        )
        assertEquals(
            SheetUrlResult.Valid(
                "https://docs.google.com/spreadsheets/d/e/2PACX-1vTestPubId/pub?output=csv&gid=123&single=true",
            ),
            result,
        )
    }

    @Test
    fun keepsGvizCsvLinkAsIs() {
        val url = "https://docs.google.com/spreadsheets/d/abc123/gviz/tq?tqx=out:csv&sheet=Peso"
        val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(url)
        assertEquals(SheetUrlResult.Valid(url), result)
    }

    @Test
    fun rejectsBlankOrNonUrlInput() {
        assertTrue(GoogleSheetsUrlNormalizer.normalizeToCsvUrl("   ") is SheetUrlResult.Invalid)
        assertTrue(GoogleSheetsUrlNormalizer.normalizeToCsvUrl("not a url") is SheetUrlResult.Invalid)
    }
}
