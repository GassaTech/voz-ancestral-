package com.example.presentation.territories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.IndigenousLand
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TerritoryDetailsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerritoryDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("territory_details_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("territory_details_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Voltar"
                )
            }

            Text(
                text = "Detalhes do território",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        when (val state = uiState) {
            TerritoryDetailsUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("territory_details_loading"),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Carregando dados do território...")
                }
            }

            is TerritoryDetailsUiState.Success -> {
                TerritoryDetailsContent(land = state.land)
            }

            TerritoryDetailsUiState.NotFound -> {
                DetailsFeedback(
                    title = "Território não encontrado",
                    message = "Não existem dados locais para o território selecionado.",
                    testTag = "territory_details_not_found"
                )
            }

            is TerritoryDetailsUiState.Error -> {
                DetailsFeedback(
                    title = "Não foi possível carregar os dados",
                    message = state.message,
                    testTag = "territory_details_error"
                )
            }
        }
    }
}

@Composable
private fun TerritoryDetailsContent(
    land: IndigenousLand
) {
    val ethnicities = land.ethnicities
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank { land.rawEthnicity?.takeIf { it.isNotBlank() } ?: "Não informado" }

    val municipalities = land.municipalities
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank {
            land.rawMunicipality?.takeIf { it.isNotBlank() } ?: "Não informado"
        }

    val states = land.statesUf
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .ifBlank {
            land.rawStateUf?.takeIf { it.isNotBlank() } ?: "Não informado"
        }

    val area = land.surfaceArea?.let {
        NumberFormat.getNumberInstance(Locale("pt", "BR")).apply {
            maximumFractionDigits = 2
        }.format(it)
    } ?: "Não informado"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .testTag("territory_details_content"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = land.name?.takeIf { it.isNotBlank() }
                        ?: "Território sem nome informado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Código: ${land.territoryCode ?: land.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Informações territoriais",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                DetailField("Povos e etnias", ethnicities)
                DetailField("Municípios", municipalities)
                DetailField("Estado(s)", states)
                DetailField("Área registrada", area)
                DetailField("Fase do processo", land.phase ?: "Não informado")
                DetailField("Modalidade", land.modality ?: "Não informado")
                DetailField("Última atualização", land.updatedAtRaw ?: "Não informado")
                land.objectId?.let {
                    DetailField("Identificador do registro", it.toString())
                }
            }
        }

        Text(
            text = "Fonte: cadastro territorial armazenado no aplicativo. " +
                "Os dados apresentados correspondem aos campos disponíveis no registro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DetailsFeedback(
    title: String,
    message: String,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(testTag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}