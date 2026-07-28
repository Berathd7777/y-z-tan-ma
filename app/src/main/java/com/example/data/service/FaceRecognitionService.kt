package com.example.data.service

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.example.data.db.FriendWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaceRecognitionService {
    suspend fun recognizeFace(
        queryPhotoBytes: ByteArray,
        registeredFriends: List<FriendWithPhotos>
    ): FaceMatchResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext FaceMatchResult(
                matchedFriendId = null,
                confidence = 0.0,
                matchName = null,
                reason = "API anahtarı eksik veya yapılandırılmamış. Lütfen AI Studio Secrets panelinden GEMINI_API_KEY değerini girin."
            )
        }

        if (registeredFriends.isEmpty()) {
            return@withContext FaceMatchResult(
                matchedFriendId = null,
                confidence = 0.0,
                matchName = null,
                reason = "Veritabanında kayıtlı arkadaş bulunamadı. Lütfen önce arkadaş ekleyin."
            )
        }

        // Build the prompt describing the registered friends
        val friendsListText = StringBuilder()
        friendsListText.append("Kayıtlı Arkadaşlar:\n")
        registeredFriends.forEach { fw ->
            friendsListText.append("- ID: ${fw.friend.id}, İsim: ${fw.friend.name}, Sosyal Medya: ${fw.friend.socialMedia}\n")
        }

        val prompt = """
            Yüz tanıma asistanı olarak görevin, en sonda 'QUERY_IMAGE' olarak işaretlenen fotoğraftaki kişinin, yukarıda listelenen kayıtlı arkadaşlardan hangisi olduğunu yüksek doğrulukla bulmaktır.
            Aşağıda, her arkadaşın referans fotoğrafları ve ardından sorgulanan QUERY_IMAGE yer almaktadır.
            
            $friendsListText
            
            Lütfen QUERY_IMAGE'deki yüzü referans fotoğraflarla dikkatlice karşılaştır. Yüz yapılarını, gözleri, burnu, ağzı, çeneyi ve genel hatları analiz et.
            Eğer bir eşleşme bulursan (güven skoru en az 0.70 olmalıdır), eşleşen arkadaşın ID'sini, ismini, güven skorunu (0.0 ile 1.0 arası) ve neden eşleştiğine dair kısa bir açıklamayı döndür.
            Eğer fotoğraftaki kişi kayıtlı arkadaşlardan biri değilse veya emin değilsen 'matchedFriendId' değerini null döndür.
            
            Lütfen sonucu kesinlikle şu şemada geçerli bir JSON olarak döndür:
            {
              "matchedFriendId": 1 (veya eşleşme yoksa null),
              "confidence": 0.92 (veya 0.0),
              "matchName": "Ahmet" (veya null),
              "reason": "Yüz hatları ve göz yapısı Referans 1 ile uyuşmaktadır."
            }
        """.trimIndent()

        // Construct parts
        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))

        // Interleave references
        registeredFriends.forEach { fw ->
            fw.photos.forEachIndexed { index, photo ->
                parts.add(Part(text = "REFERANS_IMAGE (Friend ID: ${fw.friend.id}, Photo: ${index + 1})"))
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = photo.photoBytes.toBase64())))
            }
        }

        // Add query image
        parts.add(Part(text = "QUERY_IMAGE (Sorgulanan Yüz)"))
        parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = queryPhotoBytes.toBase64())))

        val requestBody = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, requestBody)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                Log.d("FaceRecognitionService", "Gemini Response: $jsonText")
                val adapter = RetrofitClient.moshiInstance.adapter(FaceMatchResult::class.java)
                adapter.fromJson(jsonText) ?: FaceMatchResult(
                    matchedFriendId = null,
                    confidence = 0.0,
                    matchName = null,
                    reason = "Yanıt ayrıştırılamadı."
                )
            } else {
                FaceMatchResult(
                    matchedFriendId = null,
                    confidence = 0.0,
                    matchName = null,
                    reason = "Yapay zekadan boş yanıt döndü."
                )
            }
        } catch (e: Exception) {
            Log.e("FaceRecognitionService", "Error recognizeFace", e)
            FaceMatchResult(
                matchedFriendId = null,
                confidence = 0.0,
                matchName = null,
                reason = "Hata oluştu: ${e.localizedMessage ?: e.message}"
            )
        }
    }
}
