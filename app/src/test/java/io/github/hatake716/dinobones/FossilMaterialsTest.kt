package io.github.hatake716.dinobones

import io.github.hatake716.dinobones.model.*
import org.junit.Assert.*
import org.junit.Test

class FossilMaterialsTest {
    @Test fun everyReconstructionHasItsOwnStableFinishAndSourceMapsArePreserved(){
        val ids=SkeletonFactory.ids
        val textured=DetailedModels.entries.filterValues{it.textured}.keys
        assertEquals(textured,FossilMaterials.sourceSurfaces)
        val surfaces=ids.mapNotNull(FossilMaterials::forModel)
        assertEquals(97,surfaces.size)
        assertEquals(97,surfaces.map{it.seed}.toSet().size)
        assertEquals(97,surfaces.map{it.light}.toSet().size)
        for(id in ids.reversed()){
            val material=FossilMaterials.forModel(id)
            if(id in textured){assertNull(material);assertFalse(ModelEvidence.note(id).contains(FossilMaterials.NOTE));continue}
            assertEquals(surfaces.first{it.id==id},material)
            assertTrue(ModelEvidence.note(id).contains(FossilMaterials.NOTE))
            for(c in listOf(material!!.dark,material.light,material.soil,material.stain)){
                for(v in listOf(c.x,c.y,c.z))assertTrue(v.isFinite()&&v in 0f..1f)
            }
            assertTrue(material.roughness in .75f..1f)
            assertTrue(material.light.length()>material.dark.length())
        }
    }
    @Test fun previewsAndBothMeshPathsCarryTheSameSpeciesFinish(){
        for(id in listOf("spinosaurus","velociraptor","brachiosaurus","archaeopteryx")){
            val full=SkeletonFactory.create(id);val hall=SkeletonFactory.createTheater(id)
            assertEquals(id,full.fossilMaterial!!.id);assertEquals(full.fossilMaterial,hall.fossilMaterial)
            assertEquals(full.fossilMaterial!!.thumbnailColor(1f,2f,3f,.8f),hall.fossilMaterial!!.thumbnailColor(1f,2f,3f,.8f))
        }
    }
}
