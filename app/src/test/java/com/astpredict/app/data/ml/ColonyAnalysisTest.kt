package com.astpredict.app.data.ml

import org.junit.Assert.*
import org.junit.Test

class ColonyAnalysisTest {

    @Test
    fun testBacterialSpeciesMapping() {
        // Test that valid class IDs map correctly
        val eColi = BacterialSpecies.fromClassId(10)
        assertNotNull("E. Coli should not be null", eColi)
        assertEquals("Escherichia coli", eColi?.scientificName)
        assertEquals("E. coli", eColi?.commonName)
        assertEquals(0xFF7CB342L, eColi?.colorHex)

        val aureus = BacterialSpecies.fromClassId(20)
        assertNotNull("S. aureus should not be null", aureus)
        assertEquals("Staphylococcus aureus", aureus?.scientificName)
        assertEquals("S. aureus", aureus?.commonName)

        // Test boundary IDs
        val actinobacillus = BacterialSpecies.fromClassId(0)
        assertNotNull(actinobacillus)
        assertEquals("Actinobacillus equuli", actinobacillus?.scientificName)

        val trueperella = BacterialSpecies.fromClassId(23)
        assertNotNull(trueperella)
        assertEquals("Trueperella pyogenes", trueperella?.scientificName)

        // Test invalid class ID returns null
        assertNull(BacterialSpecies.fromClassId(-1))
        assertNull(BacterialSpecies.fromClassId(24))
    }

    @Test
    fun testSpeciesHelpers() {
        // Test name helper
        assertEquals("Escherichia coli", BacterialSpecies.getSpeciesName(10))
        assertEquals("Unknown (class 99)", BacterialSpecies.getSpeciesName(99))

        // Test color helper
        assertEquals(0xFF7CB342L, BacterialSpecies.getColor(10))
        assertEquals(0xFF9E9E9EL, BacterialSpecies.getColor(99))
    }

    @Test
    fun testBoundingBoxToPixelCoords() {
        // Normalized center box (center: 0.5, 0.5; size: 0.2, 0.2) in 1000x1000 image
        val box = BoundingBox(xCenter = 0.5f, yCenter = 0.5f, width = 0.2f, height = 0.2f)
        val pixelBox = box.toPixelCoords(imageWidth = 1000, imageHeight = 1000)

        // Expected pixel box coordinates:
        // x1 = (0.5 - 0.2 / 2) * 1000 = 0.4 * 1000 = 400
        // y1 = (0.5 - 0.2 / 2) * 1000 = 400
        // x2 = (0.5 + 0.2 / 2) * 1000 = 600
        // y2 = (0.5 + 0.2 / 2) * 1000 = 600
        assertEquals(400f, pixelBox.x1, 0.001f)
        assertEquals(400f, pixelBox.y1, 0.001f)
        assertEquals(600f, pixelBox.x2, 0.001f)
        assertEquals(600f, pixelBox.y2, 0.001f)
        assertEquals(200f, pixelBox.width, 0.001f)
        assertEquals(200f, pixelBox.height, 0.001f)
        assertEquals(500f, pixelBox.centerX, 0.001f)
        assertEquals(500f, pixelBox.centerY, 0.001f)
        assertEquals(40000f, pixelBox.area, 0.001f)
    }

    @Test
    fun testBoundingBoxPixelCoordsBoundaryCoercion() {
        // Box outside boundaries (e.g. going outside the image size)
        val box = BoundingBox(xCenter = -0.1f, yCenter = 1.2f, width = 0.5f, height = 0.6f)
        val pixelBox = box.toPixelCoords(imageWidth = 1000, imageHeight = 1000)

        // Min coordinate should be coerced to 0, max to image width/height
        assertTrue(pixelBox.x1 >= 0f)
        assertTrue(pixelBox.y1 >= 0f)
        assertTrue(pixelBox.x2 <= 1000f)
        assertTrue(pixelBox.y2 <= 1000f)
    }

    @Test
    fun testAnalysisResultCalculations() {
        val detection1 = Detection(
            classId = 10,
            className = "Escherichia coli",
            confidence = 0.90f,
            boundingBox = BoundingBox(0.3f, 0.3f, 0.1f, 0.1f)
        )
        val detection2 = Detection(
            classId = 10,
            className = "Escherichia coli",
            confidence = 0.80f,
            boundingBox = BoundingBox(0.4f, 0.4f, 0.1f, 0.1f)
        )
        val detection3 = Detection(
            classId = 20,
            className = "Staphylococcus aureus",
            confidence = 0.70f,
            boundingBox = BoundingBox(0.7f, 0.7f, 0.15f, 0.15f)
        )

        val result = AnalysisResult(
            imageUri = "content://media/external/images/media/1",
            detections = listOf(detection1, detection2, detection3),
            inferenceTimeMs = 45L,
            imageWidth = 1200,
            imageHeight = 1200
        )

        // Assert core calculations
        assertEquals(3, result.totalDetections)
        assertEquals(0.80f, result.averageConfidence, 0.001f)

        // Assert species breakdown grouping
        val breakdown = result.speciesBreakdown
        assertEquals(2, breakdown.size)
        assertEquals(2, breakdown["Escherichia coli"]?.size)
        assertEquals(1, breakdown["Staphylococcus aureus"]?.size)

        // Assert counts mapping
        val counts = result.speciesCounts
        assertEquals(2, counts["Escherichia coli"])
        assertEquals(1, counts["Staphylococcus aureus"])

        // Assert dominant species determination
        assertEquals("Escherichia coli", result.dominantSpecies)
    }

    @Test
    fun testEmptyAnalysisResultGracefulHandling() {
        val result = AnalysisResult(
            imageUri = "content://media/external/images/media/1",
            detections = emptyList(),
            inferenceTimeMs = 12L,
            imageWidth = 640,
            imageHeight = 640
        )

        assertEquals(0, result.totalDetections)
        assertEquals(0f, result.averageConfidence, 0.001f)
        assertTrue(result.speciesBreakdown.isEmpty())
        assertTrue(result.speciesCounts.isEmpty())
        assertNull(result.dominantSpecies)
    }
}
