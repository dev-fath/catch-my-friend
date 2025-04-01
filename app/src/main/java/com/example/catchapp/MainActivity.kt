package com.example.catchapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var scoreManager: ScoreManager
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraWithWarning()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        scoreManager = ScoreManager(this)
        
        setContent {
            CatchApp()
        }
    }
    
    @Composable
    fun CatchApp() {
        val score = remember { mutableStateOf(0) }
        
        // 앱이 시작될 때 저장된 점수를 불러옵니다
        LaunchedEffect(Unit) {
            score.value = scoreManager.scoreFlow.first()
        }
        
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
                MainScreen(score.value)
            }
        }
    }
    
    @Composable
    fun MainScreen(score: Int) {
        val context = LocalContext.current
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 점수 표시
            Text(
                text = stringResource(id = R.string.current_score, score),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(top = 24.dp)
            )
            
            // 메인 이미지와 버튼
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 여기에 메인 이미지 추가 (자원이 실제로 있다면)
                // Image(...)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // 캐치 버튼
                Button(
                    onClick = { checkCameraPermissionAndStart() },
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 16.dp
                    )
                ) {
                    Text(
                        text = stringResource(id = R.string.catch_button),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 하단 메시지
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.2f),
                elevation = 0.dp
            ) {
                Text(
                    text = "안녕! 캐치 버튼을 눌러서 귀여운 캐릭터를 찾아보세요!",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            }
        }
    }
    
    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCameraWithWarning()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun startCameraWithWarning() {
        val intent = Intent(this, CameraWarningActivity::class.java)
        startActivity(intent)
    }
} 