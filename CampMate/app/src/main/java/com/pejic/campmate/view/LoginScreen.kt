package com.pejic.campmate.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pejic.campmate.R
import com.pejic.campmate.viewmodel.AuthenticationViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthenticationViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val status by authViewModel.authStatus.collectAsState()

    LaunchedEffect(status) {
        if (status == R.string.success_login) {
            onLoginSuccess()
            authViewModel.clearStatus()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14571C)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.login),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                lineHeight = 38.sp,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFF14571C)
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = {
                                Text(
                                    stringResource(R.string.email),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF14571C),
                                focusedLabelColor = Color(0xFF14571C),
                                unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                cursorColor = Color(0xFF14571C)
                            ),
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = {
                                Text(
                                    stringResource(R.string.password),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF14571C),
                                focusedLabelColor = Color(0xFF14571C),
                                unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                cursorColor = Color(0xFF14571C)
                            ),
                            textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
                        )

                        Button(
                            onClick = { authViewModel.loginUser(email, password, onLoginSuccess) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF14571C),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.log_in),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp
                                )
                            )
                        }

                        status?.let { statusResId ->
                            Text(
                                text = stringResource(statusResId),
                                color = if (statusResId == R.string.success_login) {
                                    Color(0xFF14571C)
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp
                                )
                            )
                        }

                        Divider(thickness = 1.dp, color = Color.LightGray)

                        TextButton(onClick = onNavigateToRegister) {
                            Text(
                                text = stringResource(R.string.dont_have_account),
                                color = Color(0xFF14571C),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}