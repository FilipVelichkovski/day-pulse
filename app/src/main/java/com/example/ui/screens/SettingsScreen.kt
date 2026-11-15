package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusOffline
import com.example.ui.theme.StatusPaused
import com.example.ui.viewmodel.MainUiState

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSaveConnection: (String, String) -> Unit,
    onTestConnection: (String, String) -> Unit,
    onSetRequireVpn: (Boolean) -> Unit,
    onSetLiveSyncEnabled: (Boolean) -> Unit,
    onSetSyncIntervalSeconds: (Int) -> Unit,
    onSetWidgetTopCount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(uiState.settings.baseUrl) }
    var tokenInput by remember { mutableStateOf(uiState.settings.bearerToken) }
    var tokenVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.settings.baseUrl, uiState.settings.bearerToken) {
        if (urlInput.isEmpty() && uiState.settings.baseUrl.isNotEmpty()) {
            urlInput = uiState.settings.baseUrl
        }
        if (tokenInput.isEmpty() && uiState.settings.bearerToken.isNotEmpty()) {
            tokenInput = uiState.settings.bearerToken
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { _ ->
        onSetLiveSyncEnabled(true)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .testTag("settings_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Configure your WireGuard server endpoint and widget sync behavior",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Server Endpoint & Token Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Server Endpoint",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("server_url_input"),
                            label = { Text("Base URL (WireGuard IP or LAN)") },
                            placeholder = { Text("http://10.0.0.2:7879") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Link, contentDescription = null)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            shape = RoundedCornerShape(14.dp),
                        )

                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bearer_token_input"),
                            label = { Text("Bearer Token (optional)") },
                            placeholder = { Text("Server --token secret") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { tokenVisible = !tokenVisible },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        imageVector = if (tokenVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (tokenVisible) "Hide token" else "Show token",
                                    )
                                }
                            },
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onTestConnection(urlInput, tokenInput) },
                                enabled = !uiState.isTestingConnection,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("test_connection_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                if (uiState.isTestingConnection) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.NetworkCheck,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test")
                            }

                            Button(
                                onClick = { onSaveConnection(urlInput, tokenInput) },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_connection_button"),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Save & Sync")
                            }
                        }

                        uiState.healthTestResult?.let { result ->
                            val tint = if (result.ok) StatusActive else StatusOffline
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = tint.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = if (result.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = tint,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = result.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tint,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // WireGuard VPN & Background Sync Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WireGuard VPN & Widget Sync",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        // Live VPN diagnostics
                        val vpnConnected = uiState.vpnStatus.isVpnConnected
                        val vpnTint = if (vpnConnected) StatusActive else StatusPaused
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = if (vpnConnected) {
                                        "VPN Tunnel Detected${uiState.vpnStatus.interfaceName?.let { " ($it)" } ?: ""}"
                                    } else {
                                        "No Active VPN Tunnel Detected"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = vpnTint,
                                )
                                Text(
                                    text = "Transports: ${uiState.vpnStatus.activeTransports.ifEmpty { listOf("None") }.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Require VPN toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Require VPN Connection",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Only sync when WireGuard or another VPN tunnel is active. Turn off to allow direct LAN Wi-Fi.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.settings.requireVpn,
                                onCheckedChange = onSetRequireVpn,
                                modifier = Modifier.testTag("require_vpn_switch"),
                            )
                        }

                        // Live background widget sync toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Background Widget Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Runs a lightweight monitor while VPN is active to keep your home screen widget updated every minute.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.settings.liveSyncEnabled,
                                onCheckedChange = { enable ->
                                    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onSetLiveSyncEnabled(true)
                                        }
                                    } else {
                                        onSetLiveSyncEnabled(enable)
                                    }
                                },
                                modifier = Modifier.testTag("live_sync_switch"),
                            )
                        }

                        if (uiState.settings.liveSyncEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Background Poll Interval",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(30 to "30s", 60 to "1 min", 300 to "5 min").forEach { (sec, label) ->
                                        FilterChip(
                                            selected = uiState.settings.syncIntervalSeconds == sec,
                                            onClick = { onSetSyncIntervalSeconds(sec) },
                                            label = { Text(label) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("sync_interval_${sec}s"),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Widget Density Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Widget Top Items (?top=${uiState.settings.widgetTopCount})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Maximum categories & apps requested from /api/v1/widget/today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(2, 3, 4, 6).forEach { count ->
                                FilterChip(
                                    selected = uiState.settings.widgetTopCount == count,
                                    onClick = { onSetWidgetTopCount(count) },
                                    label = { Text("Top $count") },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("widget_top_$count"),
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
