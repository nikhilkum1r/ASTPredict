package com.astpredict.app.data.ml

/**
 * Enum representing the 24 bacterial species detectable by ASTPredict.
 * Each species includes its class ID, scientific name, common name, and clinical significance.
 */
enum class BacterialSpecies(
    val classId: Int,
    val scientificName: String,
    val commonName: String,
    val significance: String,
    val colorHex: Long
) {
    ACTINOBACILLUS_EQUULI(0, "Actinobacillus equuli", "A. equuli",
        "Equine septicemia, joint ill in foals", 0xFFE53935),
    ACTINOBACILLUS_PLEUROPNEUMONIAE(1, "Actinobacillus pleuropneumoniae", "A. pleuropneumoniae",
        "Porcine pleuropneumonia, major swine pathogen", 0xFFD81B60),
    AEROMONAS_HYDROPHILA(2, "Aeromonas hydrophila", "A. hydrophila",
        "Aquaculture infections, motile Aeromonas septicemia", 0xFF8E24AA),
    BACILLUS_CEREUS(3, "Bacillus cereus", "B. cereus",
        "Food safety pathogen, emetic and diarrheal toxins", 0xFF5E35B1),
    BIBERSTEINIA_TREHALOSI(4, "Bibersteinia trehalosi", "B. trehalosi",
        "Ovine systemic pasteurellosis, pneumonia", 0xFF3949AB),
    BORDETELLA_BRONCHISEPTICA(5, "Bordetella bronchiseptica", "B. bronchiseptica",
        "Kennel cough, atrophic rhinitis in swine", 0xFF1E88E5),
    BRUCELLA_OVIS(6, "Brucella ovis", "B. ovis",
        "Ovine brucellosis, epididymitis in rams", 0xFF039BE5),
    CLOSTRIDIUM_PERFRINGENS(7, "Clostridium perfringens", "C. perfringens",
        "Necrotizing enteritis, gas gangrene", 0xFF00ACC1),
    CORYNEBACTERIUM_PSEUDOTUBERCULOSIS(8, "Corynebacterium pseudotuberculosis", "C. pseudotuberculosis",
        "Caseous lymphadenitis in sheep and goats", 0xFF00897B),
    ERYSIPELOTHRIX_RHUSIOPATHIAE(9, "Erysipelothrix rhusiopathiae", "E. rhusiopathiae",
        "Erysipelas in swine, diamond skin disease", 0xFF43A047),
    ESCHERICHIA_COLI(10, "Escherichia coli", "E. coli",
        "Enteric infections, colibacillosis, neonatal diarrhea", 0xFF7CB342),
    GLAESSERELLA_PARASUIS(11, "Glaesserella parasuis", "G. parasuis",
        "Glässer's disease, polyserositis in pigs", 0xFFC0CA33),
    KLEBSIELLA_PNEUMONIAE(12, "Klebsiella pneumoniae", "K. pneumoniae",
        "Nosocomial infections, mastitis in cattle", 0xFFFDD835),
    LISTERIA_MONOCYTOGENES(13, "Listeria monocytogenes", "L. monocytogenes",
        "Listeriosis, circling disease, zoonotic pathogen", 0xFFFFB300),
    PAENIBACILLUS_LARVAE(14, "Paenibacillus larvae", "P. larvae",
        "American foulbrood in honeybees", 0xFFFB8C00),
    PASTEURELLA_MULTOCIDA(15, "Pasteurella multocida", "P. multocida",
        "Hemorrhagic septicemia, fowl cholera", 0xFFF4511E),
    PROTEUS_MIRABILIS(16, "Proteus mirabilis", "P. mirabilis",
        "Urinary tract infections, wound infections", 0xFF6D4C41),
    PSEUDOMONAS_AERUGINOSA(17, "Pseudomonas aeruginosa", "P. aeruginosa",
        "Opportunistic pathogen, otitis in dogs", 0xFF546E7A),
    RHODOCOCCUS_EQUI(18, "Rhodococcus equi", "R. equi",
        "Equine pneumonia in foals, zoonotic risk", 0xFFAB47BC),
    SALMONELLA_ENTERICA(19, "Salmonella enterica", "S. enterica",
        "Salmonellosis, zoonotic, food safety pathogen", 0xFF42A5F5),
    STAPHYLOCOCCUS_AUREUS(20, "Staphylococcus aureus", "S. aureus",
        "Mastitis, skin infections, MRSA concern", 0xFFEF5350),
    STAPHYLOCOCCUS_HYICUS(21, "Staphylococcus hyicus", "S. hyicus",
        "Exudative epidermitis (greasy pig disease)", 0xFF66BB6A),
    STREPTOCOCCUS_AGALACTIAE(22, "Streptococcus agalactiae", "S. agalactiae",
        "Neonatal infections, fish streptococcosis", 0xFF26C6DA),
    TRUEPERELLA_PYOGENES(23, "Trueperella pyogenes", "T. pyogenes",
        "Pyogenic abscesses, liver abscess in cattle", 0xFFFF7043);

    companion object {
        private val classIdMap = entries.associateBy { it.classId }

        fun fromClassId(id: Int): BacterialSpecies? = classIdMap[id]

        fun getSpeciesName(classId: Int): String {
            return fromClassId(classId)?.scientificName ?: "Unknown (class $classId)"
        }

        fun getColor(classId: Int): Long {
            return fromClassId(classId)?.colorHex ?: 0xFF9E9E9E
        }
    }
}
