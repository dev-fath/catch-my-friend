package com.example.catchapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.ui.unit.toArgb
import androidx.compose.ui.platform.LocalSystemUiController
import androidx.compose.ui.platform.SideEffect
import androidx.compose.ui.platform.rememberSystemUiController

class CameraActivity : ComponentActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: Executor
    private lateinit var photoUri: Uri
    private lateinit var scoreManager: ScoreManager
    private val characterGenerator = CharacterGenerator()
    
    private var shouldShowPhoto by mutableStateOf(false)
    private var isLoading by mutableStateOf(false)
    private var capturedBitmap by mutableStateOf<Bitmap?>(null)
    private var capturedCharacter by mutableStateOf<CatchCharacter?>(null)
    private var cameraAvailable by mutableStateOf(true)
    private var characterPosition by mutableStateOf(Pair(0.5f, 0.5f)) // X, Y의 비율 (0.0 ~ 1.0)
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var loadingJob: Job? = null
    private var camera: Camera? = null

    // 실패 메시지 목록
    private val failureMessages = listOf(
        Triple(
            "아쉽게도...",
            "이번에는 캐릭터를 찾지 못했어요",
            "다시 도전해보세요! 캐릭터는 언제든 나타날 수 있어요 ✨"
        ),
        Triple(
            "앗! 놓쳤어요!",
            "캐릭터가 숨어버렸어요",
            "조금만 더 찾아보면 찾을 수 있을 거예요 🔍"
        ),
        Triple(
            "헉! 아깝네요!",
            "캐릭터가 도망갔어요",
            "다시 한번 시도해보세요! 행운이 함께할 거예요 🍀"
        ),
        Triple(
            "저런... 안타까워요",
            "캐릭터를 발견하지 못했어요",
            "다음에는 꼭 찾을 수 있을 거예요! 포기하지 마세요 💫"
        )
    )
    
    // 랜덤하게 실패 메시지 선택
    private var selectedFailureMessage by mutableStateOf<Triple<String, String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 스테이터스바 영역까지 확장
        window.setDecorFitsSystemWindows(false)
        window.statusBarColor = Color.TRANSPARENT.toArgb()
        
        outputDirectory = getOutputDirectory()
        cameraExecutor = ContextCompat.getMainExecutor(this)
        scoreManager = ScoreManager(this)
        
        setContent {
            CatchAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CameraScreen()
                }
            }
        }
    }
    
    @Composable
    fun CameraScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        
        // 시스템 UI 컨트롤러 설정
        val systemUiController = rememberSystemUiController()
        
        // 스테이터스바와 네비게이션바를 투명하게 설정
        SideEffect {
            systemUiController.setSystemBarsColor(
                color = Color.Transparent,
                darkIcons = false
            )
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = false
            )
        }

        if (shouldShowPhoto) {
            PhotoResultScreen()
        } else {
            if (cameraAvailable) {
                CameraPreviewScreen()
            } else {
                CameraUnavailableScreen()
            }
        }
    }
    
    @Composable
    fun CameraUnavailableScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp,
                backgroundColor = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "카메라를 사용할 수 없습니다",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "에뮬레이터에서는 카메라 기능이 제한될 수 있습니다. 실제 기기에서 테스트해 보세요.",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 테스트 모드 버튼 - 카메라 없이 기능 시뮬레이션
                    Button(
                        onClick = { simulateCameraCapture() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "테스트 모드로 계속하기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 메인으로 돌아가기 버튼
                    OutlinedButton(
                        onClick = { finish() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "메인으로 돌아가기",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    fun CameraPreviewScreen() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        
        var previewUseCase by remember { mutableStateOf<Preview?>(null) }
        val imageCaptureUseCase by remember {
            mutableStateOf(
                ImageCapture.Builder()
                    .setTargetRotation(android.view.Surface.ROTATION_0)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
            )
        }
        
        Box(modifier = Modifier.fillMaxSize()) {
            // 카메라 미리보기
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    
                    cameraProviderFuture.addListener({
                        try {
                            cameraProvider = cameraProviderFuture.get()
                            
                            // 사용 가능한 카메라 확인
                            val cameraSelector = when {
                                hasBackCamera() -> CameraSelector.DEFAULT_BACK_CAMERA
                                hasFrontCamera() -> CameraSelector.DEFAULT_FRONT_CAMERA
                                else -> {
                                    // 카메라가 없음을 설정
                                    cameraAvailable = false
                                    return@addListener
                                }
                            }
                            
                            previewUseCase = Preview.Builder().build().also { 
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            
                            try {
                                cameraProvider?.unbindAll()
                                camera = cameraProvider?.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    previewUseCase,
                                    imageCaptureUseCase
                                )
                            } catch (e: Exception) {
                                cameraAvailable = false
                                Toast.makeText(context, "카메라를 실행할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            cameraAvailable = false
                            Toast.makeText(context, "카메라 프로바이더 초기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, cameraExecutor)
                    
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 로딩 표시
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colors.secondary,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "캐릭터를 찾고 있어요...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // 사진 촬영 버튼
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = { 
                        if (!isLoading) {
                            isLoading = true
                            takePhoto(imageCaptureUseCase) 
                        }
                    },
                    modifier = Modifier
                        .size(100.dp)
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
                        text = "캐치!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
    
    // 뒤쪽 카메라 사용 가능 여부 확인
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }
    
    // 전면 카메라 사용 가능 여부 확인
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }
    
    // 테스트 모드용 - 카메라 없이 캡처 시뮬레이션
    private fun simulateCameraCapture() {
        isLoading = true
        
        // 임시 파일 생성
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        
        try {
            // 디렉토리가 없으면 생성
            photoFile.parentFile?.mkdirs()
            
            // 테스트용 더미 이미지 생성 (빈 파일이 아닌 검은색 이미지)
            val dummyBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(dummyBitmap)
            canvas.drawColor(android.graphics.Color.BLACK)
            
            val outputStream = photoFile.outputStream()
            dummyBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            photoUri = Uri.fromFile(photoFile)
            capturedBitmap = dummyBitmap
            
            // 랜덤 캐릭터 생성 (테스트 모드에서는 50% 확률로 높임)
            capturedCharacter = characterGenerator.generateRandomCharacter(50)
            
            // 캐릭터가 없는 경우 랜덤 실패 메시지 선택
            if (capturedCharacter == null) {
                selectedFailureMessage = failureMessages.random()
            }
            
            // 캐릭터가 있는 경우 랜덤 위치 지정
            if (capturedCharacter != null) {
                // X, Y 좌표를 랜덤하게 설정 (화면 가장자리는 피함)
                characterPosition = Pair(
                    (0.25f + Math.random().toFloat() * 0.5f),  // 25% ~ 75% 범위
                    (0.25f + Math.random().toFloat() * 0.5f)   // 25% ~ 75% 범위
                )
            }
            
            // 기존 로딩 작업 취소
            loadingJob?.cancel()
            
            // 3초 로딩 후 결과 표시
            loadingJob = lifecycleScope.launch {
                delay(3000) // 3초 지연
                
                // 사진 결과 화면 표시
                isLoading = false
                shouldShowPhoto = true
            }
        } catch (e: Exception) {
            isLoading = false
            Toast.makeText(this, "테스트 모드 초기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun PhotoResultScreen() {
        val context = LocalContext.current
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 여기에 촬영한 사진이 보여질 부분
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                // 실제 촬영한 사진을 배경으로 표시
                capturedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "촬영한 사진",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // 캐릭터가 잡혔다면 캐릭터를 랜덤 위치에 표시
                capturedCharacter?.let { character ->
                    // 캐릭터 애니메이션
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    // 화면 크기 가져오기
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val screenHeight = configuration.screenHeightDp.dp
                    
                    // 캐릭터 모양
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 캐릭터 모양
                        Box(
                            modifier = Modifier
                                .size(112.5.dp * scale)
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (screenWidth * characterPosition.first),
                                    y = (screenHeight * characterPosition.second)
                                )
                                .background(
                                    color = Color(character.color.hashCode()),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.h6,
                                color = getTextColorForBackground(Color(character.color.hashCode())),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // 하단에 메시지 표시
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // 캐릭터 이름
                        Card(
                            modifier = Modifier.padding(top = 8.dp),
                            backgroundColor = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "${character.name}를 잡았어요!",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 점수 획득 메시지
                        Card(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(0.9f),
                            backgroundColor = MaterialTheme.colors.secondary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🎉 축하해요! 🎉",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "특별한 캐릭터를 발견했어요!",
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+10점을 획득했어요!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                // 캐릭터가 없을 때 - 랜덤한 실패 메시지 표시
                if (capturedCharacter == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            backgroundColor = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp),
                            elevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 선택된 실패 메시지 표시
                                selectedFailureMessage?.let { (title, subtitle, message) ->
                                    Text(
                                        text = title,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colors.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = message,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 하단 제어 버튼들 - 캐릭터 성공 화면(저장하기, 다시하기, 그만하기)
            if (capturedCharacter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 저장하기 버튼
                    Button(
                        onClick = { 
                            savePhotoToGallery()
                            // 점수 증가 및 저장
                            lifecycleScope.launch { 
                                scoreManager.increaseScore(10)
                                Toast.makeText(context, "10점 획득! 사진이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                                // 메인 화면으로 돌아가기
                                cleanupAndFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "저장",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "저장하기")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 다시하기 버튼
                    Button(
                        onClick = { 
                            // 카메라 화면으로 돌아가기
                            resetCameraState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "다시하기",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "다시하기")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 그만하기 버튼
                    Button(
                        onClick = { 
                            // 메인 화면으로 돌아가기
                            cleanupAndFinish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF9E9E9E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "그만하기",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "그만하기")
                    }
                }
            } 
            // 하단 제어 버튼들 - 캐릭터 실패 화면(다시하기, 그만하기)
            else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 다시하기 버튼
                    Button(
                        onClick = { 
                            // 카메라 화면으로 돌아가기
                            resetCameraState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "다시하기",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "다시 도전하기")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 그만하기 버튼
                    Button(
                        onClick = { 
                            // 메인 화면으로 돌아가기
                            cleanupAndFinish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF9E9E9E)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "그만하기",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "그만하기")
                    }
                }
            }
        }
    }
    
    private fun resetCameraState() {
        // 로딩 타이머 취소
        loadingJob?.cancel()
        
        // 상태 초기화
        shouldShowPhoto = false
        capturedCharacter = null
        isLoading = false
        
        // 카메라 다시 시작
        safeUnbindCamera()
    }
    
    private fun cleanupAndFinish() {
        // 모든 리소스 정리 후 액티비티 종료
        loadingJob?.cancel()
        safeUnbindCamera()
        finish()
    }
    
    private fun safeUnbindCamera() {
        try {
            camera = null
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // 카메라 디바이스 에러 무시 - 이미 닫혔거나 오류 상태
        }
    }
    
    private fun savePhotoToGallery() {
        try {
            // 캐릭터가 있을 때만 합성 과정 수행
            if (capturedCharacter != null && capturedBitmap != null) {
                // 사진과 캐릭터를 합성
                val mergedBitmap = createCharacterPhotoMerge()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "Catch_${System.currentTimeMillis()}.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Catch")
                    }
                    
                    val resolver = contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    
                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            // 합성된 비트맵 저장
                            mergedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                        }
                    }
                } else {
                    // Android 9 이하 버전에서는 기존 방식 사용
                    val fileName = "Catch_${System.currentTimeMillis()}.jpg"
                    val newFile = File(outputDirectory.parent, "Catch/$fileName")
                    if (!newFile.parentFile?.exists()!!) {
                        newFile.parentFile?.mkdirs()
                    }
                    
                    val outputStream = newFile.outputStream()
                    mergedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    outputStream.close()
                }
            } else {
                // 기존 사진만 저장 (캐릭터 없는 경우)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Catch")
                    }
                    
                    val resolver = contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    
                    uri?.let {
                        resolver.openOutputStream(it)?.use { outputStream ->
                            val inputStream = contentResolver.openInputStream(photoUri)
                            inputStream?.use { input ->
                                input.copyTo(outputStream)
                            }
                        }
                    }
                } else {
                    // Android 9 이하 버전에서는 기존 방식 사용
                    val file = File(photoUri.path ?: "")
                    val newFile = File(outputDirectory.parent, "Catch/${file.name}")
                    file.copyTo(newFile, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "사진 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 사진과 캐릭터를 합성하는 함수
    private fun createCharacterPhotoMerge(): Bitmap {
        val originalBitmap = capturedBitmap!!
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width, 
            originalBitmap.height, 
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(originalBitmap, 0f, 0f, null)
        
        // 캐릭터 정보
        val character = capturedCharacter!!
        
        // 캐릭터 크기 (화면 너비의 22.5% 정도)
        val characterSize = (originalBitmap.width * 0.225).toInt()
        
        // 캐릭터의 위치 계산 (랜덤 위치의 비율 사용)
        val posX = (characterPosition.first * originalBitmap.width - characterSize / 2).toInt()
        val posY = (characterPosition.second * originalBitmap.height - characterSize / 2).toInt()
        
        // 캐릭터 색상을 비트맵으로 그리기
        val characterBitmap = Bitmap.createBitmap(characterSize, characterSize, Bitmap.Config.ARGB_8888)
        val characterCanvas = Canvas(characterBitmap)
        
        // 투명한 배경에 원형 캐릭터 그리기
        val radius = characterSize / 2f
        val paint = android.graphics.Paint()
        paint.color = character.color.hashCode()
        paint.isAntiAlias = true
        characterCanvas.drawCircle(radius, radius, radius, paint)
        
        // 텍스트 그리기
        val textPaint = android.graphics.Paint()
        val backgroundColor = Color(character.color.hashCode())
        textPaint.color = if (getTextColorForBackground(backgroundColor) == Color.White) 
            android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textPaint.textSize = characterSize * 0.5f
        textPaint.textAlign = android.graphics.Paint.Align.CENTER
        textPaint.isFakeBoldText = true
        textPaint.isAntiAlias = true
        
        val x = radius
        val y = radius - (textPaint.descent() + textPaint.ascent()) / 2
        
        characterCanvas.drawText(
            character.name.first().toString(),
            x,
            y,
            textPaint
        )
        
        // 캐릭터 배치
        canvas.drawBitmap(characterBitmap, posX.toFloat(), posY.toFloat(), null)
        
        return resultBitmap
    }

    private fun takePhoto(imageCapture: ImageCapture) {
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA)
                .format(System.currentTimeMillis()) + ".jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    isLoading = false
                    Toast.makeText(this@CameraActivity, "사진 촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    photoUri = Uri.fromFile(photoFile)
                    
                    // 찍은 사진을 비트맵으로 로드하고 회전 문제 처리
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    capturedBitmap = correctImageOrientation(bitmap, photoFile.absolutePath)
                    
                    // 랜덤 캐릭터 생성 (10% 확률)
                    capturedCharacter = characterGenerator.generateRandomCharacter()
                    
                    // 캐릭터가 없는 경우 랜덤 실패 메시지 선택
                    if (capturedCharacter == null) {
                        selectedFailureMessage = failureMessages.random()
                    }
                    
                    // 캐릭터가 있는 경우 랜덤 위치 지정
                    if (capturedCharacter != null) {
                        // X, Y 좌표를 랜덤하게 설정 (화면 가장자리는 피함)
                        characterPosition = Pair(
                            (0.25f + Math.random().toFloat() * 0.5f),  // 25% ~ 75% 범위
                            (0.25f + Math.random().toFloat() * 0.5f)   // 25% ~ 75% 범위
                        )
                    }
                    
                    // 기존 로딩 작업 취소
                    loadingJob?.cancel()
                    
                    // 3초 로딩 후 결과 표시
                    loadingJob = lifecycleScope.launch {
                        delay(3000) // 3초 지연
                        
                        // 사진 결과 화면 표시
                        isLoading = false
                        shouldShowPhoto = true
                    }
                }
            }
        )
    }
    
    // 이미지 회전 문제를 처리하는 함수
    private fun correctImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        // ExifInterface를 사용하여 이미지 방향 정보 읽기
        var rotate = 0
        try {
            val exif = android.media.ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            
            rotate = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 회전이 필요하지 않으면 원본 반환
        if (rotate == 0) {
            return bitmap
        }
        
        // 회전이 필요한 경우 Matrix를 이용하여 회전
        val matrix = Matrix()
        matrix.postRotate(rotate.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "Catch").apply { mkdirs() }
        }
        
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 모든 리소스 해제
        loadingJob?.cancel()
        safeUnbindCamera()
        cameraExecutor.run {}
    }
    
    override fun onPause() {
        super.onPause()
        
        // 액티비티가 일시 중지될 때 카메라 리소스 해제
        safeUnbindCamera()
    }

    // 색상의 밝기를 계산하여 적절한 글자색을 반환하는 함수
    private fun getTextColorForBackground(backgroundColor: Color): Color {
        // 색상의 밝기를 계산 (YIQ 공식 사용)
        val r = backgroundColor.red
        val g = backgroundColor.green
        val b = backgroundColor.blue
        val yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000f
        
        // YIQ가 0.5보다 크면 어두운 색상, 작으면 밝은 색상 반환
        return if (yiq > 0.5f) Color.Black else Color.White
    }
} 