package com.example.catchapp

import androidx.compose.ui.graphics.Color
import java.util.Random

data class CatchCharacter(
    val name: String,
    val color: Color,
    val size: Float = 1.0f
)

class CharacterGenerator {
    // 캐릭터 목록을 private 상수로 선언 (컴파일 시점에 결정)
    private val characters = listOf(
        CatchCharacter("빨강이", Color(0xFFD71D1D), 0.9f), //
        CatchCharacter("주황이", Color(0xFFFC6C00), 1.0f), //
        CatchCharacter("노랑이", Color(0xFFFFEB3B), 0.9f),
        CatchCharacter("초록이", Color(0xFF4CAF50), 0.9f), //
        CatchCharacter("파랑이", Color(0xFF1047EF), 0.8f), //
        CatchCharacter("남이", Color(0xFF000796), 0.8f), //
        CatchCharacter("보라미", Color(0xFF9927FF), 1.2f), //
        CatchCharacter("별이", Color(0xFFFFEB3B), 1.0f),
        CatchCharacter("달이", Color(0xFFFFC107), 1.0f),
        CatchCharacter("구름이", Color(0xFFB8FFFF), 1.1f), //
        CatchCharacter("하늘이", Color(0xFFB8FFFF), 1.1f), //
        CatchCharacter("핫핑키", Color(0xFFFF00AA), 1.1f),  //
        CatchCharacter("모니", Color(0xFFFFEA52), 1.1f),  //
        CatchCharacter("핑키", Color(0xFFFF1FF4), 1.1f),  //
        CatchCharacter("누니", Color(0xFFFFFFff), 1.1f),  //
        CatchCharacter("민티", Color(0xFF52E6FF), 0.9f),  //
        CatchCharacter("올리비", Color(0x8000A903), 0.9f),  //
        CatchCharacter("라이미", Color(0xFF4BFF52), 1.0f),  //
        CatchCharacter("크리미", Color(0xFFFDEAB5), 0.9f),  //
        CatchCharacter("바다", Color(0xFF00CCAB), 0.8f),  //
    )

    // 캐릭터가 나타날 확률 (20%)
    private val CHARACTER_APPEARANCE_PROBABILITY = 1

    /**
     * 랜덤하게 캐릭터를 생성합니다.
     * 10% 확률로 캐릭터가 나타나며, 캐릭터가 나타나지 않으면 null을 반환합니다.
     */
    fun generateRandomCharacter(): CatchCharacter? {
        return if (Random().nextDouble() < CHARACTER_APPEARANCE_PROBABILITY) {
            characters.random()
        } else {
            null
        }
    }

    /**
     * 지정된 확률로 캐릭터를 생성합니다.
     * @param probability 0.0 ~ 1.0 사이의 확률값
     */
    fun generateRandomCharacter(probability: Int): CatchCharacter? {
        return if (Random().nextDouble() < probability / 100.0) {
            characters.random()
        } else {
            null
        }
    }
} 