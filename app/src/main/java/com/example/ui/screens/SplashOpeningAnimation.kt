package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashOpeningAnimation(
    onAnimationFinished: () -> Unit
) {
    // Animation States
    val transitionState = remember { MutableTransitionState(0) }
    
    // Core animation values using Animatable
    val boxScale = remember { Animatable(0f) }
    val tag1Y = remember { Animatable(-300f) }
    val tag1Rot = remember { Animatable(-90f) }
    val tag2Y = remember { Animatable(-300f) }
    val tag2Rot = remember { Animatable(90f) }
    val tag3Y = remember { Animatable(-300f) }
    val tag3Rot = remember { Animatable(-45f) }
    
    val textAlpha = remember { Animatable(0f) }
    val textTranslationY = remember { Animatable(40f) }
    
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // 1. Box enters with an energetic pop
        boxScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        
        // 2. Drop Tag 1 (Green) - starts at 150ms
        delay(150)
        launch {
            tag1Rot.animateTo(
                targetValue = -12f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        tag1Y.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        
        // Impact bounce on box
        launch {
            boxScale.animateTo(0.92f, animationSpec = tween(50))
            boxScale.animateTo(1.03f, animationSpec = spring(Spring.DampingRatioMediumBouncy))
            boxScale.animateTo(1f, animationSpec = spring())
        }
        
        // 3. Drop Tag 2 (Blue) - starts 150ms after Tag 1 lands
        delay(150)
        launch {
            tag2Rot.animateTo(
                targetValue = 15f,
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
        tag2Y.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        
        // Impact bounce on box
        launch {
            boxScale.animateTo(0.92f, animationSpec = tween(50))
            boxScale.animateTo(1.03f, animationSpec = spring(Spring.DampingRatioMediumBouncy))
            boxScale.animateTo(1f, animationSpec = spring())
        }
        
        // 4. Drop Tag 3 (Vibrant Orange with ₹) - starts 150ms after Tag 2 lands
        delay(150)
        launch {
            tag3Rot.animateTo(
                targetValue = 5f,
                animationSpec = tween(450, easing = FastOutSlowInEasing)
            )
        }
        tag3Y.animateTo(
            targetValue = -8f, // Rises slightly out of the box to represent the prominent main logo tag
            animationSpec = spring(
                dampingRatio = 0.65f, // Bouncy & physical
                stiffness = Spring.StiffnessMedium
            )
        )
        
        // Final big impact bounce on box
        launch {
            boxScale.animateTo(0.88f, animationSpec = tween(60))
            boxScale.animateTo(1.06f, animationSpec = spring(Spring.DampingRatioMediumBouncy))
            boxScale.animateTo(1f, animationSpec = spring())
        }
        
        // 5. Fade in & translate text
        launch {
            textTranslationY.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessLow))
        }
        textAlpha.animateTo(1f, animationSpec = tween(500))
        
        // 6. Hold and display fully-assembled logo
        delay(600)
        
        // 7. Premium exit sweep: scale up slightly and fade out smoothly
        launch {
            exitScale.animateTo(1.08f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
        exitAlpha.animateTo(0f, animationSpec = tween(350, easing = LinearOutSlowInEasing))
        
        // Animation complete!
        onAnimationFinished()
    }

    // Modern Deep Space/Dark Slate Gradient backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E293B), // Slate 800
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF020617)  // Slate 950
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer(
                    alpha = exitAlpha.value,
                    scaleX = exitScale.value,
                    scaleY = exitScale.value
                )
        ) {
            // Animated Canvas rendering the layered box and dropping tags
            Box(
                modifier = Modifier
                    .size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background soft glowing ambiance matching launcher design
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0x123B82F6),
                        radius = size.minDimension * 0.45f
                    )
                    drawCircle(
                        color = Color(0x0A06B6D4),
                        radius = size.minDimension * 0.3f
                    )
                    
                    // Elegant concentric guidance rings
                    drawCircle(
                        color = Color(0x0C94A3B8),
                        radius = size.minDimension * 0.38f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                    )
                }

                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(
                            scaleX = boxScale.value,
                            scaleY = boxScale.value
                        )
                ) {
                    val w = size.width
                    val h = size.height
                    
                    // Scale coordinates to fit standard 108dp viewport
                    val scaleFactor = w / 108f
                    
                    // LAYER 1: Back interior walls of the box (drawn first so tags fall in front of them)
                    val backLeftWall = Path().apply {
                        moveTo(42f * scaleFactor, 54f * scaleFactor)
                        lineTo(58f * scaleFactor, 46f * scaleFactor)
                        lineTo(58f * scaleFactor, 62f * scaleFactor)
                        lineTo(42f * scaleFactor, 54f * scaleFactor)
                        close()
                    }
                    drawPath(backLeftWall, color = Color(0xFF101F42))

                    val backRightWall = Path().apply {
                        moveTo(74f * scaleFactor, 54f * scaleFactor)
                        lineTo(58f * scaleFactor, 46f * scaleFactor)
                        lineTo(58f * scaleFactor, 62f * scaleFactor)
                        lineTo(74f * scaleFactor, 54f * scaleFactor)
                        close()
                    }
                    drawPath(backRightWall, color = Color(0xFF081024))

                    // LAYER 2: Falling Price Tags
                    
                    // Tag 1 (Green) - Falling on the Left side of the box
                    if (tag1Y.value > -140f) {
                        withTransform({
                            // Translate and rotate based on live animation values
                            translate(
                                left = -18f * scaleFactor,
                                top = (tag1Y.value + 40f) * scaleFactor
                            )
                            rotate(
                                degrees = tag1Rot.value,
                                pivot = Offset(40f * scaleFactor, 34f * scaleFactor)
                            )
                        }) {
                            drawIndividualTag(
                                scaleFactor = scaleFactor,
                                bodyColor = Color(0xFF10B981), // Emerald Green
                                hasRupee = false
                            )
                        }
                    }

                    // Tag 2 (Blue) - Falling on the Right side of the box
                    if (tag2Y.value > -140f) {
                        withTransform({
                            translate(
                                left = 14f * scaleFactor,
                                top = (tag2Y.value + 44f) * scaleFactor
                            )
                            rotate(
                                degrees = tag2Rot.value,
                                pivot = Offset(40f * scaleFactor, 34f * scaleFactor)
                            )
                        }) {
                            drawIndividualTag(
                                scaleFactor = scaleFactor,
                                bodyColor = Color(0xFF3B82F6), // Ocean Blue
                                hasRupee = false
                            )
                        }
                    }

                    // Tag 3 (Vibrant Orange with ₹) - The hero tag dropping straight in the center!
                    if (tag3Y.value > -140f) {
                        withTransform({
                            translate(
                                left = 0f,
                                top = tag3Y.value * scaleFactor
                            )
                            rotate(
                                degrees = tag3Rot.value,
                                pivot = Offset(58f * scaleFactor, 40f * scaleFactor)
                            )
                        }) {
                            drawIndividualTag(
                                scaleFactor = scaleFactor,
                                bodyColor = Color(0xFFF97316), // Safety Orange
                                hasRupee = true
                            )
                        }
                    }

                    // LAYER 3: Outward Flaps and Front Outer Faces of the box (drawn on top of the tags!)
                    // Left Outward Flap
                    val leftFlap = Path().apply {
                        moveTo(42f * scaleFactor, 54f * scaleFactor)
                        lineTo(28f * scaleFactor, 49f * scaleFactor)
                        lineTo(36f * scaleFactor, 43f * scaleFactor)
                        lineTo(50f * scaleFactor, 48f * scaleFactor)
                        close()
                    }
                    drawPath(leftFlap, color = Color(0xFF1E40AF))

                    // Right Outward Flap
                    val rightFlap = Path().apply {
                        moveTo(74f * scaleFactor, 54f * scaleFactor)
                        lineTo(88f * scaleFactor, 49f * scaleFactor)
                        lineTo(80f * scaleFactor, 43f * scaleFactor)
                        lineTo(66f * scaleFactor, 48f * scaleFactor)
                        close()
                    }
                    drawPath(rightFlap, color = Color(0xFF1D4ED8))

                    // Front Left Face
                    val frontLeftFace = Path().apply {
                        moveTo(58f * scaleFactor, 74f * scaleFactor)
                        lineTo(42f * scaleFactor, 66f * scaleFactor)
                        lineTo(42f * scaleFactor, 54f * scaleFactor)
                        lineTo(58f * scaleFactor, 62f * scaleFactor)
                        close()
                    }
                    drawPath(frontLeftFace, color = Color(0xFF2563EB))

                    // Front Right Face
                    val frontRightFace = Path().apply {
                        moveTo(58f * scaleFactor, 74f * scaleFactor)
                        lineTo(74f * scaleFactor, 66f * scaleFactor)
                        lineTo(74f * scaleFactor, 54f * scaleFactor)
                        lineTo(58f * scaleFactor, 62f * scaleFactor)
                        close()
                    }
                    drawPath(frontRightFace, color = Color(0xFF1D4ED8))

                    // Rim Highlights
                    val rimHighlight = Path().apply {
                        moveTo(42f * scaleFactor, 54f * scaleFactor)
                        lineTo(58f * scaleFactor, 62f * scaleFactor)
                        lineTo(74f * scaleFactor, 54f * scaleFactor)
                    }
                    drawPath(
                        rimHighlight,
                        color = Color(0xFF93C5FD),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 0.8f * scaleFactor,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Animated Headline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = textAlpha.value,
                        translationY = textTranslationY.value
                    )
            ) {
                Text(
                    text = "SHOP PRICE LIST",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Smart Price Register & Sync",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8), // Slate 400
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Helper to draw a fully vector-styled price tag inside our Canvas
private fun DrawScope.drawIndividualTag(
    scaleFactor: Float,
    bodyColor: Color,
    hasRupee: Boolean
) {
    // We design the tag path relative to its localized anchor space
    // Standard Tag dimensions: Width from 50 to 66 (diff 16), Height from 26 to 52 (diff 26)
    // Normalized box tag: M 50,34 L 58,26 L 66,34 L 66,52 L 50,52 Z
    
    // Tag Shadow (behind tag)
    val shadowPath = Path().apply {
        moveTo(51f * scaleFactor, 35f * scaleFactor)
        lineTo(58f * scaleFactor, 28f * scaleFactor)
        lineTo(65f * scaleFactor, 35f * scaleFactor)
        lineTo(65f * scaleFactor, 53f * scaleFactor)
        lineTo(51f * scaleFactor, 53f * scaleFactor)
        close()
    }
    drawPath(shadowPath, color = Color(0x35000000))

    // Main Body
    val tagPath = Path().apply {
        moveTo(50f * scaleFactor, 34f * scaleFactor)
        lineTo(58f * scaleFactor, 26f * scaleFactor)
        lineTo(66f * scaleFactor, 34f * scaleFactor)
        lineTo(66f * scaleFactor, 52f * scaleFactor)
        lineTo(50f * scaleFactor, 52f * scaleFactor)
        close()
    }
    drawPath(tagPath, color = bodyColor)

    // White Ribbon Eyelet Ring
    drawCircle(
        color = Color.White,
        radius = 1.2f * scaleFactor,
        center = Offset(58f * scaleFactor, 31f * scaleFactor)
    )
    drawCircle(
        color = bodyColor,
        radius = 0.5f * scaleFactor,
        center = Offset(58f * scaleFactor, 31f * scaleFactor)
    )

    // If it's the main Orange hero tag, draw the elegant Indian Rupee (₹) symbol inside it
    if (hasRupee) {
        // Top Horizontal Bar
        drawLine(
            color = Color.White,
            start = Offset(53f * scaleFactor, 37f * scaleFactor),
            end = Offset(63f * scaleFactor, 37f * scaleFactor),
            strokeWidth = 1.2f * scaleFactor,
            cap = StrokeCap.Round
        )

        // Middle Horizontal Bar
        drawLine(
            color = Color.White,
            start = Offset(53f * scaleFactor, 41f * scaleFactor),
            end = Offset(61f * scaleFactor, 41f * scaleFactor),
            strokeWidth = 1.2f * scaleFactor,
            cap = StrokeCap.Round
        )

        // Upper semi-circle loop
        val upperLoopPath = Path().apply {
            moveTo(57.5f * scaleFactor, 37f * scaleFactor)
            cubicTo(
                60.5f * scaleFactor, 37f * scaleFactor,
                62.5f * scaleFactor, 38.5f * scaleFactor,
                62.5f * scaleFactor, 41f * scaleFactor
            )
            cubicTo(
                62.5f * scaleFactor, 43.5f * scaleFactor,
                60.5f * scaleFactor, 45f * scaleFactor,
                57.5f * scaleFactor, 45f * scaleFactor
            )
        }
        drawPath(
            upperLoopPath,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.2f * scaleFactor,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Base connector bar
        drawLine(
            color = Color.White,
            start = Offset(53f * scaleFactor, 45f * scaleFactor),
            end = Offset(57.5f * scaleFactor, 45f * scaleFactor),
            strokeWidth = 1.2f * scaleFactor,
            cap = StrokeCap.Round
        )

        // Diagonal leg
        drawLine(
            color = Color.White,
            start = Offset(56.5f * scaleFactor, 45f * scaleFactor),
            end = Offset(62f * scaleFactor, 50.5f * scaleFactor),
            strokeWidth = 1.2f * scaleFactor,
            cap = StrokeCap.Round
        )
    } else {
        // Draw 3 generic mini ledger lines on standard falling tags to represent a stock item listing
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(53f * scaleFactor, 38f * scaleFactor),
            end = Offset(63f * scaleFactor, 38f * scaleFactor),
            strokeWidth = 1.0f * scaleFactor,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(53f * scaleFactor, 43f * scaleFactor),
            end = Offset(61f * scaleFactor, 43f * scaleFactor),
            strokeWidth = 1.0f * scaleFactor,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(53f * scaleFactor, 48f * scaleFactor),
            end = Offset(59f * scaleFactor, 48f * scaleFactor),
            strokeWidth = 1.0f * scaleFactor,
            cap = StrokeCap.Round
        )
    }
}
