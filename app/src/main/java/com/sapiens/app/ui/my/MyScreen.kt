package com.sapiens.app.ui.my

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sapiens.app.R
import com.sapiens.app.data.store.UserPreferencesRepository
import com.sapiens.app.messaging.FcmTopicSync
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.OnPrimaryFixed
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.Primary
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** 마이 — 계정·푸시 알림 카드 공통 고정 높이 */
private val MySettingsMenuCardHeight = 88.dp

@Composable
fun MyScreen(
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val authUser by authViewModel.authUser.collectAsState()
    val signInError by authViewModel.signInError.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.onGoogleSignInActivityResult(result.data)
    }

    LaunchedEffect(signInError) {
        val msg = signInError ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        authViewModel.clearSignInError()
    }

    val pushNotificationsEnabled by preferencesRepository.pushNotificationsEnabledFlow.collectAsState(
        initial = true
    )
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                preferencesRepository.setPushNotificationsEnabled(true)
                FcmTopicSync.subscribeAll()
            }
        } else {
            Toast.makeText(
                context,
                "알림을 받으려면 설정에서 알림 권한을 허용해 주세요.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val notifPermitted = FcmTopicSync.hasNotificationPermission(context)
    val pushSubtitle =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            pushNotificationsEnabled &&
            !notifPermitted
        ) {
            "설정에서 알림 권한을 허용해 주세요"
        } else {
            ""
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // 뉴스 헤더와 동일한 시작 위치/타이포를 사용
                .padding(top = Spacing.space36)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.space20,
                        end = Spacing.space20,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "#마이",
                    modifier = Modifier.weight(1f),
                    color = TextPrimary,
                    style = SapiensTextStyles.todayHeadlineTitle,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.height(Spacing.space28))

            AccountMenuRow(
                cardHeight = MySettingsMenuCardHeight,
                subtitle = authUser?.email?.let { email -> "$email 로그인 됨" } ?: "로그인이 필요합니다",
                canOpenGoogleSignIn = authUser == null,
                onClick = {
                    googleSignInLauncher.launch(authViewModel.googleSignInIntent())
                }
            )
            PushNotificationMenuRow(
                cardHeight = MySettingsMenuCardHeight,
                subtitle = pushSubtitle,
                checked = pushNotificationsEnabled,
                onCheckedChange = { wantOn ->
                    if (wantOn) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            when (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            ) {
                                PackageManager.PERMISSION_GRANTED -> {
                                    coroutineScope.launch {
                                        preferencesRepository.setPushNotificationsEnabled(true)
                                        FcmTopicSync.subscribeAll()
                                    }
                                }
                                else -> {
                                    notificationPermissionLauncher.launch(
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    )
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                preferencesRepository.setPushNotificationsEnabled(true)
                                FcmTopicSync.subscribeAll()
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            preferencesRepository.setPushNotificationsEnabled(false)
                            FcmTopicSync.unsubscribeAll()
                        }
                    }
                },
            )
        }
        if (authUser != null) {
            TextButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.space16)
                    .padding(bottom = RowVertical)
            ) {
                Text("로그아웃", color = TextSecondary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PushNotificationMenuRow(
    cardHeight: Dp,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(horizontal = Spacing.space16, vertical = Spacing.space6)
            .fillMaxWidth()
            .height(cardHeight),
        shape = AppShapes.card,
        color = Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = CardPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_app_brand_0z),
                    contentDescription = "푸시 알림",
                    modifier = Modifier.size(Spacing.space28),
                    tint = Primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "푸시 알림",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimaryFixed,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.35f),
                ),
            )
        }
    }
}

@Composable
private fun AccountMenuRow(
    cardHeight: Dp,
    subtitle: String,
    canOpenGoogleSignIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(horizontal = Spacing.space16, vertical = Spacing.space6)
            .fillMaxWidth()
            .height(cardHeight),
        shape = AppShapes.card,
        color = Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(
                    enabled = canOpenGoogleSignIn,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(horizontal = CardPaddingHorizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ico_my_account),
                    contentDescription = "계정",
                    modifier = Modifier.size(Spacing.space28),
                    tint = Primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "계정",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Google 간편 로그인",
                tint = TextSecondary
            )
        }
    }
}
