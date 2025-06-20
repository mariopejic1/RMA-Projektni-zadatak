package com.pejic.campmate.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavController
import com.pejic.campmate.R
import com.pejic.campmate.Routes
import com.pejic.campmate.viewmodel.AuthenticationViewModel
import com.pejic.campmate.viewmodel.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, userViewModel: UserViewModel, authViewModel: AuthenticationViewModel) {
    val user by userViewModel.user.collectAsState()
    val authStatus by authViewModel.authStatus.collectAsState()
    var firstname by remember { mutableStateOf(user?.firstname ?: "") }
    var lastname by remember { mutableStateOf(user?.lastname ?: "") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<Int?>(null) }
    var profileError by remember { mutableStateOf<Int?>(null) }
    var profileSuccess by remember { mutableStateOf<Int?>(null) }
    var passwordSuccess by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(authStatus) {
        if (authStatus == R.string.success_logout) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            authViewModel.clearStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(x = (-20).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.profile),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                lineHeight = 32.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14571C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF14571C)
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF14571C)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = user != null,
                    enter = fadeIn(animationSpec = tween(800)) + slideInVertically(),
                ) {
                    user?.let { userData ->
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
                                    text = stringResource(R.string.email_label, userData.email),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = Color(0xFF14571C)
                                    ),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                    value = firstname,
                                    onValueChange = { firstname = it },
                                    label = {
                                        Text(
                                            text = userData.firstname,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    placeholder = { Text(userData.firstname.takeIf { it.isNotBlank() } ?: "") },
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
                                    value = lastname,
                                    onValueChange = { lastname = it },
                                    label = {
                                        Text(
                                            text = userData.lastname,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp
                                            )
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    placeholder = { Text(userData.lastname.takeIf { it.isNotBlank() } ?: "") },
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
                                    onClick = {
                                        isLoading = true
                                        profileError = null
                                        profileSuccess = null
                                        userViewModel.updateUserProfile(
                                            firstname = firstname,
                                            lastname = lastname,
                                            onSuccess = {
                                                isLoading = false
                                                profileError = null
                                                profileSuccess = R.string.profile_update_success
                                            },
                                            onFailure = { exception ->
                                                isLoading = false
                                                profileError = R.string.profile_update_failed
                                                profileSuccess = null
                                            }
                                        )
                                    },
                                    enabled = !isLoading,
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
                                        text = stringResource(R.string.save_changes),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 18.sp
                                        )
                                    )
                                }

                                profileError?.let {
                                    Text(
                                        text = stringResource(it),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                profileSuccess?.let {
                                    Text(
                                        text = stringResource(it),
                                        color = Color(0xFF14571C),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Divider(thickness = 1.dp, color = Color.LightGray)

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = {
                                        Text(
                                            stringResource(R.string.new_password),
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
                                    isError = newPassword.isNotEmpty() && newPassword.length < 8,
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
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = {
                                        Text(
                                            stringResource(R.string.confirm_password),
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
                                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF14571C),
                                        focusedLabelColor = Color(0xFF14571C),
                                        unfocusedBorderColor = Color(0xFF14571C).copy(alpha = 0.3f),
                                        unfocusedLabelColor = Color(0xFF14571C).copy(alpha = 0.6f),
                                        cursorColor = Color(0xFF14571C)
                                    ),
                                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
                                )

                                passwordError?.let {
                                    Text(
                                        text = stringResource(it),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                passwordSuccess?.let {
                                    Text(
                                        text = stringResource(it),
                                        color = Color(0xFF14571C),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        passwordError = null
                                        passwordSuccess = null
                                        when {
                                            newPassword.isBlank() || confirmPassword.isBlank() -> {
                                                passwordError = R.string.password_fields_empty
                                            }
                                            newPassword.length < 8 -> {
                                                passwordError = R.string.password_too_short
                                            }
                                            newPassword != confirmPassword -> {
                                                passwordError = R.string.passwords_do_not_match
                                            }
                                            else -> {
                                                isLoading = true
                                                userViewModel.changePassword(
                                                    newPassword = newPassword,
                                                    onSuccess = {
                                                        isLoading = false
                                                        newPassword = ""
                                                        confirmPassword = ""
                                                        passwordError = null
                                                        passwordSuccess = R.string.password_change_success
                                                    },
                                                    onFailure = { exception ->
                                                        isLoading = false
                                                        passwordError = R.string.password_change_failed
                                                        passwordSuccess = null
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isLoading,
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
                                        text = stringResource(R.string.change_password),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 18.sp
                                        )
                                    )
                                }

                                Divider(thickness = 1.dp, color = Color.LightGray)

                                Button(
                                    onClick = {
                                        isLoading = true
                                        authViewModel.logout()
                                    },
                                    enabled = !isLoading,
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
                                        text = stringResource(R.string.sign_out),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 18.sp
                                        )
                                    )
                                }
                            }
                        }
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF14571C)
                        )
                    }
                }
            }
        }
    }
}