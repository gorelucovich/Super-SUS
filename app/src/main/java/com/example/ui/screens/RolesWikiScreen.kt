package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Faction
import com.example.model.SuperSusRoles
import com.example.model.SusRole

@Composable
fun RolesWikiScreen(
    modifier: Modifier = Modifier
) {
    var selectedFaction by remember { mutableStateOf<Faction?>(null) }

    val filteredRoles = remember(selectedFaction) {
        if (selectedFaction == null) SuperSusRoles.allRoles
        else SuperSusRoles.allRoles.filter { it.faction == selectedFaction }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("roles_wiki_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "База ролей Super Sus",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Способности, повадки и тактики детекции",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Faction Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFaction == null,
                    onClick = { selectedFaction = null },
                    label = { Text("Все (${SuperSusRoles.allRoles.size})") }
                )
                FilterChip(
                    selected = selectedFaction == Faction.SPACECREW,
                    onClick = { selectedFaction = if (selectedFaction == Faction.SPACECREW) null else Faction.SPACECREW },
                    label = { Text("Космонавты") }
                )
                FilterChip(
                    selected = selectedFaction == Faction.IMPOSTOR,
                    onClick = { selectedFaction = if (selectedFaction == Faction.IMPOSTOR) null else Faction.IMPOSTOR },
                    label = { Text("Предатели") }
                )
                FilterChip(
                    selected = selectedFaction == Faction.NEUTRAL,
                    onClick = { selectedFaction = if (selectedFaction == Faction.NEUTRAL) null else Faction.NEUTRAL },
                    label = { Text("Нейтралы") }
                )
            }
        }

        items(filteredRoles, key = { it.id }) { role ->
            RoleDetailCard(role = role)
        }
    }
}

@Composable
fun RoleDetailCard(role: SusRole) {
    val factionColor = Color(role.faction.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(factionColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${role.iconBadge} ${role.nameRu}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = factionColor.copy(alpha = 0.15f),
                    contentColor = factionColor
                ) {
                    Text(
                        text = role.faction.ruName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = role.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (role.winCondition.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏆 Цель победы: ${role.winCondition}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Как детектор вычисляет эту роль:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            role.keyBehaviors.forEach { behavior ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = behavior,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
