package com.example.catchapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CameraWarningActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    primary = Color(0xFFFF9DCA),
                    primaryVariant = Color(0xFFFF6FA5),
                    secondary = Color(0xFFFFD166),
                    background = Color(0xFFFFFAEE),
                    surface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    WarningScreen()
                }
            }
        }
    }
    
    @Composable
    fun WarningScreen() {
        var timeRemaining by remember { mutableStateOf(5) }
        var countdownStarted by remember { mutableStateOf(false) }
        var buttonEnabled by remember { mutableStateOf(false) }
        
        // 타이머 효과
        LaunchedEffect(Unit) {
            countdownStarted = true
            object : CountDownTimer(5000, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    timeRemaining = (millisUntilFinished / 1000).toInt() + 1
                }
                
                override fun onFinish() {
                    buttonEnabled = true
                    timeRemaining = 0
                }
            }.start()
        }
        
        // 버튼 애니메이션
        val buttonAlpha = animateFloatAsState(
            targetValue = if (buttonEnabled) 1f else 0.5f
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 경고 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color(0xFFFFF3CD),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "주의!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(id = R.string.camera_warning),
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 카운트다운 숫자
            if (timeRemaining > 0) {
                Text(
                    text = "${timeRemaining}초 후에 버튼이 활성화됩니다",
                    fontSize = 18.sp,
                    color = MaterialTheme.colors.primary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // 확인 버튼
            Button(
                onClick = { 
                    if (buttonEnabled) {
                        startCameraActivity()
                    }
                },
                modifier = Modifier
                    .height(60.dp)
                    .fillMaxWidth(0.7f),
                enabled = buttonEnabled,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.secondary,
                    disabledBackgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = stringResource(id = R.string.confirm_button),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish() // 이 액티비티를 종료하여 뒤로가기 시 이 화면을 건너뛰게 합니다
    }
} 