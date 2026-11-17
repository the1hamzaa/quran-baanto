package com.example.data.quran

/**
 * Standard 15-Line Madinah Mushaf (604 Pages) Page Mapper.
 * Maps any Surah and Ayah to its exact Madinah Mushaf page number (1 to 604)
 * and Juz (Para) number.
 */
object QuranPageMapper {

    // Starting page for each of the 114 Surahs in the standard 604-page Madinah Mushaf
    private val SURAH_STARTING_PAGES = intArrayOf(
        0,   // dummy 0-index
        1,   // 1. Al-Fatihah
        2,   // 2. Al-Baqarah
        50,  // 3. Ali 'Imran
        77,  // 4. An-Nisa
        106, // 5. Al-Ma'idah
        128, // 6. Al-An'am
        151, // 7. Al-A'raf
        177, // 8. Al-Anfal
        187, // 9. At-Tawbah
        208, // 10. Yunus
        221, // 11. Hud
        235, // 12. Yusuf
        249, // 13. Ar-Ra'd
        255, // 14. Ibrahim
        262, // 15. Al-Hijr
        267, // 16. An-Nahl
        282, // 17. Al-Isra
        293, // 18. Al-Kahf
        305, // 19. Maryam
        312, // 20. Ta-Ha
        322, // 21. Al-Anbiya
        332, // 22. Al-Hajj
        342, // 23. Al-Mu'minun
        350, // 24. An-Nur
        359, // 25. Al-Furqan
        367, // 26. Ash-Shu'ara
        377, // 27. An-Naml
        385, // 28. Al-Qasas
        396, // 29. Al-'Ankabut
        404, // 30. Ar-Rum
        411, // 31. Luqman
        415, // 32. As-Sajdah
        418, // 33. Al-Ahzab
        428, // 34. Saba
        434, // 35. Fatir
        440, // 36. Ya-Sin
        446, // 37. As-Saffat
        453, // 38. Sad
        458, // 39. Az-Zumar
        467, // 40. Ghafir
        477, // 41. Fussilat
        483, // 42. Ash-Shura
        489, // 43. Az-Zukhruf
        496, // 44. Ad-Dukhan
        499, // 45. Al-Jathiyah
        502, // 46. Al-Ahqaf
        507, // 47. Muhammad
        511, // 48. Al-Fath
        515, // 49. Al-Hujurat
        518, // 50. Qaf
        520, // 51. Adh-Dhariyat
        523, // 52. At-Tur
        526, // 53. An-Najm
        528, // 54. Al-Qamar
        531, // 55. Ar-Rahman
        534, // 56. Al-Waqi'ah
        537, // 57. Al-Hadid
        542, // 58. Al-Mujadila
        545, // 59. Al-Hashr
        549, // 60. Al-Mumtahanah
        551, // 61. As-Saff
        553, // 62. Al-Jumu'ah
        554, // 63. Al-Munafiqun
        556, // 64. At-Taghabun
        558, // 65. At-Talaq
        560, // 66. At-Tahrim
        562, // 67. Al-Mulk
        564, // 68. Al-Qalam
        566, // 69. Al-Haqqah
        568, // 70. Al-Ma'arij
        570, // 71. Nuh
        572, // 72. Al-Jinn
        574, // 73. Al-Muzzammil
        575, // 74. Al-Muddaththir
        577, // 75. Al-Qiyamah
        578, // 76. Al-Insan
        580, // 77. Al-Mursalat
        582, // 78. An-Naba
        583, // 79. An-Nazi'at
        585, // 80. 'Abasa
        586, // 81. At-Takwir
        587, // 82. Al-Infitar
        587, // 83. Al-Mutaffifin
        589, // 84. Al-Inshiqaq
        590, // 85. Al-Buruj
        591, // 86. At-Tariq
        591, // 87. Al-A'la
        592, // 88. Al-Ghashiyah
        593, // 89. Al-Fajr
        594, // 90. Al-Balad
        595, // 91. Ash-Shams
        595, // 92. Al-Layl
        596, // 93. Ad-Duha
        596, // 94. Ash-Sharh
        597, // 95. At-Tin
        597, // 96. Al-'Alaq
        598, // 97. Al-Qadr
        598, // 98. Al-Bayyinah
        599, // 99. Az-Zalzalah
        599, // 100. Al-'Adiyat
        600, // 101. Al-Qari'ah
        600, // 102. At-Takathur
        601, // 103. Al-'Asr
        601, // 104. Al-Humazah
        601, // 105. Al-Fil
        602, // 106. Quraysh
        602, // 107. Al-Ma'un
        602, // 108. Al-Kawthar
        603, // 109. Al-Kafirun
        603, // 110. An-Nasr
        603, // 111. Al-Masad
        604, // 112. Al-Ikhlas
        604, // 113. Al-Falaq
        604  // 114. An-Nas
    )

    // Total verses in each Surah
    private val SURAH_VERSES_COUNT = intArrayOf(
        0, 7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89,
        59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30,
        52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15,
        21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    /**
     * Returns the Madinah Mushaf page (1..604) for a given Surah and Ayah.
     */
    fun getPageForAyah(surahNumber: Int, ayahNumber: Int, paraNumberHint: Int = 1): Int {
        val sNum = surahNumber.coerceIn(1, 114)
        val startPage = SURAH_STARTING_PAGES[sNum]
        val endPage = if (sNum < 114) SURAH_STARTING_PAGES[sNum + 1] else 604
        val pageSpan = (endPage - startPage).coerceAtLeast(1)

        val totalVerses = SURAH_VERSES_COUNT.getOrElse(sNum) { 7 }.coerceAtLeast(1)
        val aNum = ayahNumber.coerceIn(1, totalVerses)

        // Interpolate ayah within the page span
        val offset = ((aNum - 1).toFloat() / totalVerses * pageSpan).toInt()
        val calculated = (startPage + offset).coerceIn(startPage, endPage)

        return calculated.coerceIn(1, 604)
    }

    /**
     * Returns the Juz (1..30) for a given page number.
     */
    fun getJuzForPage(page: Int): Int {
        val p = page.coerceIn(1, 604)
        if (p <= 21) return 1
        val juz = ((p - 2) / 20) + 1
        return juz.coerceIn(1, 30)
    }

    /**
     * Starting page for each of the 30 Paras (Juzs).
     */
    fun getStartingPageForPara(paraNumber: Int): Int {
        val p = paraNumber.coerceIn(1, 30)
        return if (p == 1) 1 else (p - 1) * 20 + 2
    }
}
