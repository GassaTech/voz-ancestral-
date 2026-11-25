package com.example.presentation.territories

import com.example.core.common.AppResult
import com.example.domain.model.ArchiveStats
import com.example.domain.model.IndigenousLand
import com.example.domain.model.RemoteSyncOutcome
import com.example.domain.model.SyncFailureType
import com.example.domain.model.SyncState
import com.example.domain.model.TerritoryFilter
import com.example.domain.model.TerritoryFilterOptions
import com.example.domain.model.TerritorySortDirection
import com.example.domain.model.TerritorySortField
import com.example.domain.model.TerritorySortOption
import com.example.domain.repository.IndigenousLandRepository
import com.example.domain.repository.SyncStatusRepository
import com.example.domain.sync.ArchiveSyncCoordinator
import com.example.domain.sync.ArchiveSyncOutcome
import com.example.domain.usecase.ObserveIndigenousLandFilterOptionsUseCase
import com.example.domain.usecase.ObserveIndigenousLandsCountUseCase
import com.example.domain.usecase.ObserveIndigenousLandsUseCase
import com.example.domain.usecase.ObserveSyncStateUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerritoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `1 - estado inicial do ViewModel possui filtros vazios e Loading sem consultas prematuras`() = runTest(testDispatcher) {
        val fakeRepository = FakeIndigenousLandRepository()
        val fakeSyncStatus = FakeSyncStatusRepository(initialSyncState = SyncState.Loading)
        val fakeCoordinator = FakeArchiveSyncCoordinator()

        val viewModel = createViewModel(fakeRepository, fakeSyncStatus, fakeCoordinator)

        assertEquals(
            TerritoriesUiState(
                query = "",
                selectedStateUf = null,
                selectedPhase = null,
                selectedModality = null,
                sortOption = TerritorySortOption(
                    field = TerritorySortField.NAME,
                    direction = TerritorySortDirection.ASCENDING
                ),
                filterOptions = TerritoryFilterOptions(),
                totalLocalTerritoriesCount = 0,
                syncState = SyncState.Loading,
                contentState = TerritoriesContentState.Loading
            ),
            viewModel.uiState.value
        )
        assertFalse(viewModel.uiState.value.hasActiveFilter)
        assertTrue(fakeRepository.recordedFilters.isEmpty())
        assertEquals(0, fakeCoordinator.autoSyncCallCount)
        assertEquals(0, fakeCoordinator.manualSyncCallCount)
    }

    @Test
    fun `2 - carregamento inicial emite Loading e depois Success com os territorios do repositorio`() = runTest(testDispatcher) {
        val sample = listOf(
            sampleLand(id = "ti_1", name = "Terra Indígena Arara", territoryCode = "1001", statesUf = listOf("PA")),
            sampleLand(id = "ti_2", name = "Terra Indígena Yanomami", territoryCode = "1002", statesUf = listOf("AM", "RR"))
        )
        val gate = CompletableDeferred<Unit>()
        val fakeRepository = FakeIndigenousLandRepository(
            initialLands = sample,
            customObserveTerritories = { filter ->
                flow {
                    gate.await()
                    emit(applyFakeFilterAndSort(sample, filter))
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        runCurrent()
        assertEquals(TerritoriesContentState.Loading, viewModel.uiState.value.contentState)

        gate.complete(Unit)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TerritoriesContentState.Success(sample), state.contentState)
        assertEquals(2, state.totalLocalTerritoriesCount)
        assertEquals(listOf(TerritoryFilter()), fakeRepository.recordedFilters)

        collectorJob.cancel()
    }

    @Test
    fun `3 - estado Empty sem filtros informa hasActiveFilter false`() = runTest(testDispatcher) {
        val fakeRepository = FakeIndigenousLandRepository(initialLands = emptyList())
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()

        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = false),
            viewModel.uiState.value.contentState
        )
        assertFalse(viewModel.uiState.value.hasActiveFilter)

        collectorJob.cancel()
    }

    @Test
    fun `4 - estado Empty com busca ou filtros ativos informa hasActiveFilter true`() = runTest(testDispatcher) {
        val fakeRepository = FakeIndigenousLandRepository(
            initialLands = listOf(
                sampleLand(
                    id = "ti_1",
                    name = "Nove de Janeiro",
                    statesUf = listOf("RO"),
                    phase = "Regularizada",
                    modality = "Tradicionalmente Ocupada"
                )
            )
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.contentState is TerritoriesContentState.Success)

        viewModel.onQueryChange("inexistente")
        advanceUntilIdle()
        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )
        assertTrue(viewModel.uiState.value.hasActiveFilter)

        viewModel.onQueryChange("")
        viewModel.onSelectStateUf("AC")
        advanceUntilIdle()
        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        viewModel.onSelectStateUf(null)
        viewModel.onSelectPhase("Em Estudo")
        advanceUntilIdle()
        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        viewModel.onSelectPhase(null)
        viewModel.onSelectModality("Reserva Indígena")
        advanceUntilIdle()
        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `5 - onQueryChange atualiza query imediatamente e debounce de 150 ms evita consultas intermediarias`() = runTest(testDispatcher) {
        val fakeRepository = FakeIndigenousLandRepository(
            initialLands = listOf(sampleLand(id = "ti_1", name = "Parintintin"))
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedFilters.size)

        viewModel.onQueryChange("P")
        advanceTimeBy(60L)
        runCurrent()
        assertEquals("P", viewModel.uiState.value.query)
        assertEquals(1, fakeRepository.recordedFilters.size)

        viewModel.onQueryChange("Par")
        advanceTimeBy(80L)
        runCurrent()
        assertEquals("Par", viewModel.uiState.value.query)
        assertEquals(1, fakeRepository.recordedFilters.size)

        viewModel.onQueryChange("Parintintin")
        advanceTimeBy(149L)
        runCurrent()
        assertEquals("Parintintin", viewModel.uiState.value.query)
        assertEquals(1, fakeRepository.recordedFilters.size)

        advanceTimeBy(1L)
        advanceUntilIdle()
        assertEquals(2, fakeRepository.recordedFilters.size)
        assertEquals("Parintintin", fakeRepository.recordedFilters.last().query)

        collectorJob.cancel()
    }

    @Test
    fun `6 - filtros combinaveis por UF fase e modalidade junto com pesquisa e limpeza de filtros`() = runTest(testDispatcher) {
        val target = sampleLand(
            id = "ti_target",
            name = "Ipixuna",
            rawEthnicity = "Parintintin",
            ethnicities = listOf("Parintintin"),
            rawMunicipality = "Humaitá",
            municipalities = listOf("Humaitá"),
            statesUf = listOf("AM"),
            phase = "Regularizada",
            modality = "Tradicionalmente Ocupada"
        )
        val wrongUf = target.copy(id = "ti_wrong_uf", name = "Ipixuna Sul", statesUf = listOf("RO"))
        val wrongPhase = target.copy(id = "ti_wrong_phase", name = "Ipixuna Norte", phase = "Declarada")
        val wrongModality = target.copy(id = "ti_wrong_mod", name = "Ipixuna Leste", modality = "Reserva Indígena")

        val allLands = listOf(target, wrongUf, wrongPhase, wrongModality)
        val fakeRepository = FakeIndigenousLandRepository(initialLands = allLands)
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.onSelectStateUf("AM")
        viewModel.onSelectPhase("Regularizada")
        viewModel.onSelectModality("Tradicionalmente Ocupada")
        viewModel.onQueryChange("Ipixuna")
        viewModel.onSelectSortOption(
            TerritorySortOption(
                field = TerritorySortField.SURFACE_AREA,
                direction = TerritorySortDirection.DESCENDING
            )
        )
        advanceUntilIdle()

        val filteredState = viewModel.uiState.value
        assertEquals("Ipixuna", filteredState.query)
        assertEquals("AM", filteredState.selectedStateUf)
        assertEquals("Regularizada", filteredState.selectedPhase)
        assertEquals("Tradicionalmente Ocupada", filteredState.selectedModality)
        assertTrue(filteredState.hasActiveFilter)
        assertEquals(TerritoriesContentState.Success(listOf(target)), filteredState.contentState)
        assertEquals(
            TerritoryFilter(
                query = "Ipixuna",
                stateUf = "AM",
                phase = "Regularizada",
                modality = "Tradicionalmente Ocupada",
                sortOption = TerritorySortOption(
                    field = TerritorySortField.SURFACE_AREA,
                    direction = TerritorySortDirection.DESCENDING
                )
            ),
            fakeRepository.recordedFilters.last()
        )

        // Strings em branco nos seletores devem normalizar para null.
        viewModel.onSelectStateUf("   ")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedStateUf)

        viewModel.onSelectStateUf("AM")
        advanceUntilIdle()

        viewModel.onClearFilters()
        runCurrent()
        assertEquals("", viewModel.uiState.value.query)
        assertNull(viewModel.uiState.value.selectedStateUf)
        assertNull(viewModel.uiState.value.selectedPhase)
        assertNull(viewModel.uiState.value.selectedModality)
        assertFalse(viewModel.uiState.value.hasActiveFilter)
        // Ordenação é preferência de exibição e permanece preservada na limpeza de filtros.
        assertEquals(
            TerritorySortOption(
                field = TerritorySortField.SURFACE_AREA,
                direction = TerritorySortDirection.DESCENDING
            ),
            viewModel.uiState.value.sortOption
        )

        advanceUntilIdle()
        val restoredSuccess = viewModel.uiState.value.contentState as TerritoriesContentState.Success
        assertEquals(4, restoredSuccess.territories.size)

        collectorJob.cancel()
    }

    @Test
    fun `7 - quatro opcoes de ordenacao e direcoes ascendente e descendente funcionam corretamente`() = runTest(testDispatcher) {
        val landA = sampleLand(
            id = "a",
            name = "Alto Rio Negro",
            territoryCode = "0200",
            surfaceArea = 5000.0,
            updatedAtRaw = "2023-05-10"
        )
        val landB = sampleLand(
            id = "b",
            name = "Bari",
            territoryCode = "0100",
            surfaceArea = 15000.0,
            updatedAtRaw = "2025-01-20"
        )
        val landC = sampleLand(
            id = "c",
            name = "Carajás",
            territoryCode = "0300",
            surfaceArea = 1000.0,
            updatedAtRaw = "2024-08-15"
        )

        val fakeRepository = FakeIndigenousLandRepository(initialLands = listOf(landB, landC, landA))
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // Padrão: NAME + ASCENDING
        assertEquals(
            TerritorySortOption(TerritorySortField.NAME, TerritorySortDirection.ASCENDING),
            viewModel.uiState.value.sortOption
        )
        assertEquals(
            listOf("Alto Rio Negro", "Bari", "Carajás"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.name }
        )

        // Mesmo campo -> alterna para DESCENDING
        viewModel.onSelectSortField(TerritorySortField.NAME)
        advanceUntilIdle()
        assertEquals(
            TerritorySortOption(TerritorySortField.NAME, TerritorySortDirection.DESCENDING),
            viewModel.uiState.value.sortOption
        )
        assertEquals(
            listOf("Carajás", "Bari", "Alto Rio Negro"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.name }
        )

        // TERRITORY_CODE ASC -> DESC via onToggleSortDirection
        viewModel.onSelectSortField(TerritorySortField.TERRITORY_CODE)
        advanceUntilIdle()
        assertEquals(
            TerritorySortOption(TerritorySortField.TERRITORY_CODE, TerritorySortDirection.ASCENDING),
            viewModel.uiState.value.sortOption
        )
        assertEquals(
            listOf("0100", "0200", "0300"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.territoryCode }
        )

        viewModel.onToggleSortDirection()
        advanceUntilIdle()
        assertEquals(
            listOf("0300", "0200", "0100"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.territoryCode }
        )

        // SURFACE_AREA ASC e DESC
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.ASCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf(1000.0, 5000.0, 15000.0),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.surfaceArea }
        )

        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.DESCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf(15000.0, 5000.0, 1000.0),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.surfaceArea }
        )

        // UPDATED_AT ASC e DESC
        viewModel.onSelectSortField(TerritorySortField.UPDATED_AT)
        advanceUntilIdle()
        assertEquals(
            listOf("2023-05-10", "2024-08-15", "2025-01-20"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.updatedAtRaw }
        )

        viewModel.onSelectSortField(TerritorySortField.UPDATED_AT)
        advanceUntilIdle()
        assertEquals(
            listOf("2025-01-20", "2024-08-15", "2023-05-10"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.updatedAtRaw }
        )

        collectorJob.cancel()
    }

    @Test
    fun `8 - atualizacao reativa das opcoes de filtro e contagem total preserva total mesmo com filtros`() = runTest(testDispatcher) {
        val initialLands = listOf(
            sampleLand(id = "ti_1", name = "Arara", statesUf = listOf("PA"), phase = "Regularizada", modality = "Tradicionalmente Ocupada"),
            sampleLand(id = "ti_2", name = "Kaxinawá", statesUf = listOf("AC"), phase = "Declarada", modality = "Reserva Indígena")
        )
        val fakeRepository = FakeIndigenousLandRepository(initialLands = initialLands)
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.totalLocalTerritoriesCount)
        assertEquals(
            TerritoryFilterOptions(
                statesUf = listOf("AC", "PA"),
                phases = listOf("Declarada", "Regularizada"),
                modalities = listOf("Reserva Indígena", "Tradicionalmente Ocupada")
            ),
            viewModel.uiState.value.filterOptions
        )

        // Filtro sem correspondência preserva contagem total local real.
        viewModel.onQueryChange("SEM_RESULTADO")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.totalLocalTerritoriesCount)
        assertEquals(
            TerritoriesContentState.Empty(hasActiveFilter = true),
            viewModel.uiState.value.contentState
        )

        // Nova sincronização insere mais territórios -> contagem total e opções de filtro atualizam reativamente.
        val updatedLands = initialLands + sampleLand(
            id = "ti_3",
            name = "Raposa Serra do Sol",
            statesUf = listOf("RR"),
            phase = "Homologada",
            modality = "Dominial"
        )
        fakeRepository.replaceLands(updatedLands)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.totalLocalTerritoriesCount)
        assertEquals(
            TerritoryFilterOptions(
                statesUf = listOf("AC", "PA", "RR"),
                phases = listOf("Declarada", "Homologada", "Regularizada"),
                modalities = listOf("Dominial", "Reserva Indígena", "Tradicionalmente Ocupada")
            ),
            viewModel.uiState.value.filterOptions
        )

        collectorJob.cancel()
    }

    @Test
    fun `9 - falha do repositorio emite Error e onRetry recupera consulta com sucesso`() = runTest(testDispatcher) {
        var shouldFail = true
        val expected = listOf(sampleLand(id = "ti_1", name = "Terra Recuperada"))
        val fakeRepository = FakeIndigenousLandRepository(
            initialLands = expected,
            customObserveTerritories = { _ ->
                flow {
                    if (shouldFail) {
                        throw IllegalStateException("Erro ao consultar SQLite")
                    }
                    emit(expected)
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        advanceUntilIdle()
        assertEquals(1, fakeRepository.recordedFilters.size)
        assertEquals(
            TerritoriesContentState.Error("Erro ao consultar SQLite"),
            viewModel.uiState.value.contentState
        )

        shouldFail = false
        viewModel.onRetry()
        advanceUntilIdle()

        assertEquals(2, fakeRepository.recordedFilters.size)
        assertEquals(
            TerritoriesContentState.Success(expected),
            viewModel.uiState.value.contentState
        )

        collectorJob.cancel()
    }

    @Test
    fun `10 - erro sem mensagem usa fallback legivel e nao converte CancellationException em Error`() = runTest(testDispatcher) {
        val blankErrorRepository = FakeIndigenousLandRepository(
            customObserveTerritories = { _ ->
                flow {
                    throw IllegalStateException("")
                }
            }
        )
        val errorViewModel = createViewModel(blankErrorRepository)
        val errorCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            errorViewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertEquals(
            TerritoriesContentState.Error("Erro ao consultar Terras Indígenas no banco de dados local."),
            errorViewModel.uiState.value.contentState
        )
        errorCollector.cancel()

        val cancellingRepository = FakeIndigenousLandRepository(
            customObserveTerritories = { _ ->
                flow {
                    throw CancellationException("Cancelamento estruturado")
                }
            },
            customObserveFilterOptions = {
                flow {
                    throw CancellationException("Cancelamento de opções")
                }
            },
            customObserveTotalCount = {
                flow {
                    throw CancellationException("Cancelamento de contagem")
                }
            }
        )
        val cancellingViewModel = createViewModel(cancellingRepository)
        val cancellingCollector = launch(UnconfinedTestDispatcher(testScheduler)) {
            cancellingViewModel.uiState.collect {}
        }
        advanceUntilIdle()

        assertFalse(cancellingViewModel.uiState.value.contentState is TerritoriesContentState.Error)
        assertEquals(TerritoriesContentState.Loading, cancellingViewModel.uiState.value.contentState)
        assertEquals(TerritoryFilterOptions(), cancellingViewModel.uiState.value.filterOptions)
        assertEquals(0, cancellingViewModel.uiState.value.totalLocalTerritoriesCount)

        cancellingCollector.cancel()
    }

    @Test
    fun `11 - alteracao de filtros cancela coleta anterior via flatMapLatest antes da nova consulta`() = runTest(testDispatcher) {
        var startedFlows = 0
        var cancelledFlows = 0
        val events = mutableListOf<String>()

        val fakeRepository = FakeIndigenousLandRepository(
            customObserveTerritories = { filter ->
                callbackFlow {
                    val flowIndex = ++startedFlows
                    events += "start:$flowIndex:${filter.stateUf}:${filter.phase}:${filter.modality}"
                    trySend(
                        listOf(
                            sampleLand(
                                id = "ti_$flowIndex",
                                name = "Terra $flowIndex",
                                statesUf = listOfNotNull(filter.stateUf),
                                phase = filter.phase,
                                modality = filter.modality
                            )
                        )
                    )
                    awaitClose {
                        cancelledFlows += 1
                        events += "cancel:$flowIndex"
                    }
                }
            }
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertEquals(1, startedFlows)
        assertEquals(0, cancelledFlows)

        viewModel.onSelectStateUf("AM")
        advanceUntilIdle()
        assertEquals(2, startedFlows)
        assertEquals(1, cancelledFlows)

        viewModel.onSelectPhase("Regularizada")
        advanceUntilIdle()
        assertEquals(3, startedFlows)
        assertEquals(2, cancelledFlows)

        assertEquals(
            listOf(
                "start:1:null:null:null",
                "cancel:1",
                "start:2:AM:null:null",
                "cancel:2",
                "start:3:AM:Regularizada:null"
            ),
            events
        )

        collectorJob.cancel()
        advanceUntilIdle()
        assertEquals(3, cancelledFlows)
    }

    @Test
    fun `12 - syncState e propagado e syncManually coordena via ArchiveSyncCoordinator bloqueando duplicadas`() = runTest(testDispatcher) {
        val syncGate = CompletableDeferred<ArchiveSyncOutcome>()
        val fakeRepository = FakeIndigenousLandRepository()
        val fakeSyncStatus = FakeSyncStatusRepository(initialSyncState = SyncState.Idle)
        var attempt = 0
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                attempt += 1
                if (attempt == 1) {
                    fakeSyncStatus.syncStateFlow.value = SyncState.Syncing
                    syncGate.await()
                } else {
                    fakeSyncStatus.syncStateFlow.value = SyncState.Synced(
                        lastSyncedEpochMillis = 1_700_000_200_000L,
                        territoriesCount = 12
                    )
                    ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
                }
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeSyncStatus, fakeCoordinator)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertEquals(SyncState.Idle, viewModel.uiState.value.syncState)

        // Chamadas repetidas concorrentes devem acionar apenas uma execução.
        viewModel.syncManually()
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        assertEquals(SyncState.Syncing, viewModel.uiState.value.syncState)

        // Simula cancelamento / fallback e verifica que controle é liberado para nova tentativa.
        fakeSyncStatus.syncStateFlow.value = SyncState.OfflineFallback(
            reason = "Sem conexão",
            lastAttemptEpochMillis = 1_700_000_100_000L,
            failureType = SyncFailureType.NO_INTERNET
        )
        syncGate.completeExceptionally(CancellationException("Cancelada"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.syncState is SyncState.OfflineFallback)

        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(2, fakeCoordinator.manualSyncCallCount)
        assertEquals(
            SyncState.Synced(lastSyncedEpochMillis = 1_700_000_200_000L, territoriesCount = 12),
            viewModel.uiState.value.syncState
        )

        collectorJob.cancel()
    }

    @Test
    fun `13 - ordenacao com valores nulos mantem nulos no final em ordem crescente e decrescente nos quatro campos`() = runTest(testDispatcher) {
        val landBeta = sampleLand(
            id = "ti_beta",
            name = "Bari",
            territoryCode = "200",
            surfaceArea = 2500.0,
            updatedAtRaw = "2024-06-01"
        )
        val landAlfa = sampleLand(
            id = "ti_alfa",
            name = "Alto Turiaçu",
            territoryCode = "100",
            surfaceArea = 1000.0,
            updatedAtRaw = "2023-01-15"
        )
        val landGama = sampleLand(
            id = "ti_gama",
            name = "Canela",
            territoryCode = "300",
            surfaceArea = 5000.0,
            updatedAtRaw = "2025-03-20"
        )
        val landNulls = sampleLand(
            id = "ti_null",
            name = null,
            territoryCode = null,
            surfaceArea = null,
            updatedAtRaw = null
        )
        val landBlank = sampleLand(
            id = "ti_blank",
            name = "   ",
            territoryCode = " ",
            surfaceArea = null,
            updatedAtRaw = ""
        )

        val fakeRepository = FakeIndigenousLandRepository(
            initialLands = listOf(landNulls, landBeta, landBlank, landGama, landAlfa)
        )
        val viewModel = createViewModel(fakeRepository)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        // 1) NAME ASC e DESC: válidos ordenados, nulos/em branco sempre no final
        assertEquals(
            listOf("ti_alfa", "ti_beta", "ti_gama", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.NAME, TerritorySortDirection.DESCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_gama", "ti_beta", "ti_alfa", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )

        // 2) TERRITORY_CODE ASC e DESC: nulos/em branco sempre no final
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.TERRITORY_CODE, TerritorySortDirection.ASCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_alfa", "ti_beta", "ti_gama", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.TERRITORY_CODE, TerritorySortDirection.DESCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_gama", "ti_beta", "ti_alfa", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )

        // 3) SURFACE_AREA ASC e DESC: nulos sempre no final
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.ASCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_alfa", "ti_beta", "ti_gama", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.SURFACE_AREA, TerritorySortDirection.DESCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_gama", "ti_beta", "ti_alfa", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )

        // 4) UPDATED_AT ASC e DESC: nulos/em branco sempre no final
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.UPDATED_AT, TerritorySortDirection.ASCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_alfa", "ti_beta", "ti_gama", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )
        viewModel.onSelectSortOption(
            TerritorySortOption(TerritorySortField.UPDATED_AT, TerritorySortDirection.DESCENDING)
        )
        advanceUntilIdle()
        assertEquals(
            listOf("ti_gama", "ti_beta", "ti_alfa", "ti_blank", "ti_null"),
            (viewModel.uiState.value.contentState as TerritoriesContentState.Success).territories.map { it.id }
        )

        collectorJob.cancel()
    }

    @Test
    fun `14 - syncManually trata excecao comum sem falha silenciosa mantendo bloqueio de chamadas simultaneas`() = runTest(testDispatcher) {
        val syncGate = CompletableDeferred<ArchiveSyncOutcome>()
        val fakeRepository = FakeIndigenousLandRepository()
        val fakeSyncStatus = FakeSyncStatusRepository(initialSyncState = SyncState.Idle)
        var attempt = 0
        val fakeCoordinator = FakeArchiveSyncCoordinator(
            onManualSync = {
                attempt += 1
                when (attempt) {
                    1 -> syncGate.await()
                    2 -> ArchiveSyncOutcome.Executed(AppResult.Error("Falha de resposta do servidor"))
                    else -> {
                        fakeSyncStatus.syncStateFlow.value = SyncState.Synced(
                            lastSyncedEpochMillis = 1_700_000_500_000L,
                            territoriesCount = 9
                        )
                        ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
                    }
                }
            }
        )
        val viewModel = createViewModel(fakeRepository, fakeSyncStatus, fakeCoordinator)

        val collectorJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.manualSyncErrorMessage)

        // Dispara chamadas simultâneas antes da conclusão: apenas 1 execução deve iniciar.
        viewModel.syncManually()
        viewModel.syncManually()
        viewModel.syncManually()
        runCurrent()
        assertEquals(1, fakeCoordinator.manualSyncCallCount)

        // Falha com exceção comum (não cancelante) durante a execução em voo.
        syncGate.completeExceptionally(IllegalStateException("Erro inesperado de I/O"))
        advanceUntilIdle()

        assertEquals(1, fakeCoordinator.manualSyncCallCount)
        assertEquals("Erro inesperado de I/O", viewModel.uiState.value.manualSyncErrorMessage)

        viewModel.onDismissManualSyncError()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.manualSyncErrorMessage)

        // Falha via AppResult.Error sem OfflineFallback também deve ser exposta na UI.
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(2, fakeCoordinator.manualSyncCallCount)
        assertEquals("Falha de resposta do servidor", viewModel.uiState.value.manualSyncErrorMessage)

        // Nova tentativa bem-sucedida limpa o erro anterior.
        viewModel.syncManually()
        advanceUntilIdle()
        assertEquals(3, fakeCoordinator.manualSyncCallCount)
        assertNull(viewModel.uiState.value.manualSyncErrorMessage)
        assertEquals(
            SyncState.Synced(lastSyncedEpochMillis = 1_700_000_500_000L, territoriesCount = 9),
            viewModel.uiState.value.syncState
        )

        collectorJob.cancel()
    }

    private fun createViewModel(
        repository: IndigenousLandRepository,
        syncStatusRepository: SyncStatusRepository = FakeSyncStatusRepository(),
        syncCoordinator: ArchiveSyncCoordinator = FakeArchiveSyncCoordinator()
    ): TerritoriesViewModel = TerritoriesViewModel(
        observeIndigenousLands = ObserveIndigenousLandsUseCase(repository),
        observeFilterOptions = ObserveIndigenousLandFilterOptionsUseCase(repository),
        observeTotalCount = ObserveIndigenousLandsCountUseCase(repository),
        observeSyncState = ObserveSyncStateUseCase(syncStatusRepository),
        syncCoordinator = syncCoordinator
    )

    private fun sampleLand(
        id: String,
        name: String? = "Terra $id",
        territoryCode: String? = "1000",
        rawEthnicity: String? = "Parintintin",
        ethnicities: List<String> = listOfNotNull(rawEthnicity),
        rawMunicipality: String? = "Humaitá",
        municipalities: List<String> = listOfNotNull(rawMunicipality),
        statesUf: List<String> = listOf("AM"),
        surfaceArea: Double? = 12000.0,
        phase: String? = "Regularizada",
        modality: String? = "Tradicionalmente Ocupada",
        updatedAtRaw: String? = "2025-01-01"
    ): IndigenousLand = IndigenousLand(
        id = id,
        objectId = 1L,
        territoryCode = territoryCode,
        name = name,
        rawEthnicity = rawEthnicity,
        ethnicities = ethnicities,
        rawMunicipality = rawMunicipality,
        municipalities = municipalities,
        rawStateUf = statesUf.joinToString(", "),
        statesUf = statesUf,
        surfaceArea = surfaceArea,
        phase = phase,
        modality = modality,
        updatedAtRaw = updatedAtRaw
    )

    private fun applyFakeFilterAndSort(
        lands: List<IndigenousLand>,
        filter: TerritoryFilter
    ): List<IndigenousLand> {
        val query = filter.query.trim()
        val filtered = lands.filter { land ->
            val matchesQuery = query.isEmpty() || listOfNotNull(
                land.name,
                land.rawEthnicity,
                land.rawMunicipality,
                land.territoryCode
            ).any { it.contains(query, ignoreCase = true) }

            val matchesUf = filter.stateUf == null || land.statesUf.contains(filter.stateUf)
            val matchesPhase = filter.phase == null || land.phase == filter.phase
            val matchesModality = filter.modality == null || land.modality == filter.modality

            matchesQuery && matchesUf && matchesPhase && matchesModality
        }

        val direction = filter.sortOption.direction
        val primaryComparator: Comparator<IndigenousLand> = when (filter.sortOption.field) {
            TerritorySortField.NAME -> nullLastDirectionalComparator(direction) {
                it.name?.trim()?.takeIf { value -> value.isNotEmpty() }
            }
            TerritorySortField.TERRITORY_CODE -> nullLastDirectionalComparator(direction) {
                it.territoryCode?.trim()?.takeIf { value -> value.isNotEmpty() }
            }
            TerritorySortField.SURFACE_AREA -> nullLastDirectionalComparator(direction) {
                it.surfaceArea
            }
            TerritorySortField.UPDATED_AT -> nullLastDirectionalComparator(direction) {
                it.updatedAtRaw?.trim()?.takeIf { value -> value.isNotEmpty() }
            }
        }

        return filtered.sortedWith(primaryComparator.thenBy { it.id })
    }

    private fun <T : Comparable<T>> nullLastDirectionalComparator(
        direction: TerritorySortDirection,
        selector: (IndigenousLand) -> T?
    ): Comparator<IndigenousLand> = Comparator { first, second ->
        val firstValue = selector(first)
        val secondValue = selector(second)
        when {
            firstValue == null && secondValue == null -> 0
            firstValue == null -> 1
            secondValue == null -> -1
            direction == TerritorySortDirection.ASCENDING -> firstValue.compareTo(secondValue)
            else -> secondValue.compareTo(firstValue)
        }
    }

    private inner class FakeIndigenousLandRepository(
        initialLands: List<IndigenousLand> = emptyList(),
        private val customObserveTerritories: ((TerritoryFilter) -> Flow<List<IndigenousLand>>)? = null,
        private val customObserveFilterOptions: (() -> Flow<TerritoryFilterOptions>)? = null,
        private val customObserveTotalCount: (() -> Flow<Int>)? = null
    ) : IndigenousLandRepository {

        private val landsFlow = MutableStateFlow(initialLands)
        val recordedFilters = mutableListOf<TerritoryFilter>()

        override fun observeAll(): Flow<List<IndigenousLand>> = landsFlow

        override fun observeTerritories(
            query: String,
            stateUf: String?,
            phase: String?,
            modality: String?,
            sortOption: TerritorySortOption
        ): Flow<List<IndigenousLand>> {
            val filter = TerritoryFilter(
                query = query,
                stateUf = stateUf,
                phase = phase,
                modality = modality,
                sortOption = sortOption
            )
            recordedFilters += filter
            customObserveTerritories?.let { return it(filter) }

            return landsFlow.map { lands -> applyFakeFilterAndSort(lands, filter) }
        }

        override fun observeFilterOptions(): Flow<TerritoryFilterOptions> {
            customObserveFilterOptions?.let { return it() }
            return landsFlow.map { lands ->
                TerritoryFilterOptions(
                    statesUf = lands.flatMap { it.statesUf }.distinct().sorted(),
                    phases = lands.mapNotNull { it.phase }.distinct().sorted(),
                    modalities = lands.mapNotNull { it.modality }.distinct().sorted()
                )
            }
        }

        override fun observeById(id: String): Flow<IndigenousLand?> =
            landsFlow.map { lands -> lands.find { it.id == id } }

        override suspend fun findByTerritoryCode(territoryCode: String): IndigenousLand? =
            landsFlow.value.find { it.territoryCode == territoryCode }

        override fun observeTotalCount(): Flow<Int> {
            customObserveTotalCount?.let { return it() }
            return landsFlow.map { it.size }
        }

        override suspend fun countOnce(): Int = landsFlow.value.size

        override suspend fun upsert(territory: IndigenousLand) = Unit

        override suspend fun upsertAll(territories: List<IndigenousLand>) {
            landsFlow.value = territories
        }

        override suspend fun update(territory: IndigenousLand): Boolean = false

        override suspend fun deleteAll() {
            landsFlow.value = emptyList()
        }

        override suspend fun syncFromRemote(): AppResult<RemoteSyncOutcome> =
            AppResult.Success(RemoteSyncOutcome.Empty)

        fun replaceLands(lands: List<IndigenousLand>) {
            landsFlow.value = lands
        }
    }

    private class FakeSyncStatusRepository(
        initialSyncState: SyncState = SyncState.Idle
    ) : SyncStatusRepository {
        val syncStateFlow = MutableStateFlow(initialSyncState)
        override val syncState: StateFlow<SyncState> = syncStateFlow
        override fun observeArchiveStats(): Flow<ArchiveStats> = flowOf(ArchiveStats())
        override suspend fun syncArchive(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private class FakeArchiveSyncCoordinator(
        private val onAutoSync: suspend () -> ArchiveSyncOutcome = {
            ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
        },
        private val onManualSync: suspend () -> ArchiveSyncOutcome = {
            ArchiveSyncOutcome.Executed(AppResult.Success(Unit))
        }
    ) : ArchiveSyncCoordinator {
        var autoSyncCallCount: Int = 0
            private set
        var manualSyncCallCount: Int = 0
            private set

        override suspend fun requestAutoSync(): ArchiveSyncOutcome {
            autoSyncCallCount += 1
            return onAutoSync()
        }

        override suspend fun requestManualSync(): ArchiveSyncOutcome {
            manualSyncCallCount += 1
            return onManualSync()
        }
    }
}
