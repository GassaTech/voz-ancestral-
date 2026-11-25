package com.example.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ParintintinTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResponsiveComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleBadge = "CATÁLOGO DIGITAL • OFFLINE-FIRST"
    private val sampleHeroTitle = "Terras e Povos Indígenas do Brasil"
    private val sampleHeroSubtitle =
        "Plataforma offline-first para consulta de terras indígenas, saberes culturais e preservação linguística."

    private val sampleIntroTitle = "Sobre o catálogo e suas fontes"
    private val sampleIntroBody =
        "A aba Territórios reúne dados territoriais oficiais e etnias associadas da API ArcGIS REST " +
            "do IBAMA/SIGAGEO (Camada 25), e a aba Dicionário exibe registros demonstrativos em português."

    @Test
    fun `IntroNoticeCard em 280dp amplia largura do texto abaixo da imagem sem overflow horizontal`() {
        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(280.dp)) {
                    IntroNoticeCard(
                        title = sampleIntroTitle,
                        body = sampleIntroBody
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("intro_notice_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("intro_notice_image").assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleIntroTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleIntroBody).assertIsDisplayed()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("intro_notice_title")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(titleBounds.left >= imageBounds.right)
        assertTrue(bodyBounds.top >= imageBounds.bottom)
        assertTrue(bodyBounds.left <= imageBounds.left + 2f)
        assertTrue(bodyBounds.width > imageBounds.width * 2f)
        assertTrue(titleBounds.right <= cardBounds.right)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `IntroNoticeCard em 320dp preserva layout horizontal em fontScale 1_0 e sem overflow`() {
        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    IntroNoticeCard(
                        title = sampleIntroTitle,
                        body = sampleIntroBody
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("intro_notice_title")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(titleBounds.left >= imageBounds.right)
        assertTrue(bodyBounds.left >= imageBounds.right)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `IntroNoticeCard em 320dp com fontScale 1_5 adapta corpo para largura total sem overflow`() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = baseDensity.density,
                    fontScale = 1.5f
                )
            ) {
                ParintintinTheme {
                    Box(modifier = Modifier.width(320.dp)) {
                        IntroNoticeCard(
                            title = sampleIntroTitle,
                            body = sampleIntroBody
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sampleIntroTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleIntroBody).assertIsDisplayed()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bodyBounds.top >= imageBounds.bottom)
        assertTrue(bodyBounds.left <= imageBounds.left + 2f)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `IntroNoticeCard em 328dp com fontScale 1_3 adapta corpo para largura total sem overflow`() {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = baseDensity.density,
                    fontScale = 1.3f
                )
            ) {
                ParintintinTheme {
                    Box(modifier = Modifier.width(328.dp)) {
                        IntroNoticeCard(
                            title = sampleIntroTitle,
                            body = sampleIntroBody
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sampleIntroTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleIntroBody).assertIsDisplayed()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bodyBounds.top >= imageBounds.bottom)
        assertTrue(bodyBounds.left <= imageBounds.left + 2f)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `IntroNoticeCard em 360dp preserva disposicao horizontal sem overflow`() {
        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    IntroNoticeCard(
                        title = sampleIntroTitle,
                        body = sampleIntroBody
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bodyBounds.left >= imageBounds.right)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `IntroNoticeCard em 400dp preserva disposicao horizontal sem overflow`() {
        composeTestRule.setContent {
            ParintintinTheme {
                Box(modifier = Modifier.width(400.dp)) {
                    IntroNoticeCard(
                        title = sampleIntroTitle,
                        body = sampleIntroBody
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val cardBounds = composeTestRule.onNodeWithTag("intro_notice_card")
            .fetchSemanticsNode().boundsInRoot
        val imageBounds = composeTestRule.onNodeWithTag("intro_notice_image")
            .fetchSemanticsNode().boundsInRoot
        val bodyBounds = composeTestRule.onNodeWithTag("intro_notice_body")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bodyBounds.left >= imageBounds.right)
        assertTrue(bodyBounds.right <= cardBounds.right)
    }

    @Test
    fun `HeroPreservationBanner em 280dp com fontScale 1_5 nao apresenta corte nem sobreposicao`() {
        var minHeightPx = 0f

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(density = baseDensity.density, fontScale = 1.5f)
            minHeightPx = with(scaledDensity) { 232.dp.toPx() }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                ParintintinTheme {
                    Box(modifier = Modifier.width(280.dp)) {
                        HeroPreservationBanner(
                            badgeText = sampleBadge,
                            title = sampleHeroTitle,
                            subtitle = sampleHeroSubtitle
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sampleBadge).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleHeroTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(sampleHeroSubtitle).assertIsDisplayed()

        val bannerBounds = composeTestRule.onNodeWithTag("home_hero_banner")
            .fetchSemanticsNode().boundsInRoot
        val badgeBounds = composeTestRule.onNodeWithTag("home_hero_badge")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("home_hero_title")
            .fetchSemanticsNode().boundsInRoot
        val subtitleBounds = composeTestRule.onNodeWithTag("home_hero_subtitle")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected banner height (${bannerBounds.height}) >= minHeightPx ($minHeightPx)",
            bannerBounds.height >= minHeightPx - 1f
        )
        assertTrue(
            "Expected title top (${titleBounds.top}) >= badge bottom (${badgeBounds.bottom})",
            titleBounds.top >= badgeBounds.bottom
        )
        assertTrue(
            "Expected subtitle top (${subtitleBounds.top}) >= title bottom (${titleBounds.bottom})",
            subtitleBounds.top >= titleBounds.bottom
        )
        assertTrue(
            "Expected subtitle bottom (${subtitleBounds.bottom}) <= banner bottom (${bannerBounds.bottom})",
            subtitleBounds.bottom <= bannerBounds.bottom
        )
        assertTrue(badgeBounds.right <= bannerBounds.right)
        assertTrue(titleBounds.right <= bannerBounds.right)
        assertTrue(subtitleBounds.right <= bannerBounds.right)
    }

    @Test
    fun `HeroPreservationBanner expande altura acima de 232dp quando conteudo excede altura minima`() {
        var minHeightPx = 0f
        val multilineTitle = "Terras e Povos\nIndígenas do Brasil em\nCatálogo Digital"
        val multilineSubtitle =
            "Plataforma digital aberta para consulta territorial,\n" +
                "memória cultural, pesquisa acadêmica\n" +
                "e acesso offline em dispositivos móveis\n" +
                "com suporte a fontes ampliadas."

        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(density = baseDensity.density, fontScale = 1.5f)
            minHeightPx = with(scaledDensity) { 232.dp.toPx() }

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                ParintintinTheme {
                    Box(modifier = Modifier.width(248.dp)) {
                        HeroPreservationBanner(
                            badgeText = sampleBadge,
                            title = multilineTitle,
                            subtitle = multilineSubtitle
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(sampleBadge).assertIsDisplayed()
        composeTestRule.onNodeWithText(multilineTitle).assertIsDisplayed()
        composeTestRule.onNodeWithText(multilineSubtitle).assertIsDisplayed()

        val bannerBounds = composeTestRule.onNodeWithTag("home_hero_banner")
            .fetchSemanticsNode().boundsInRoot
        val badgeBounds = composeTestRule.onNodeWithTag("home_hero_badge")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("home_hero_title")
            .fetchSemanticsNode().boundsInRoot
        val subtitleBounds = composeTestRule.onNodeWithTag("home_hero_subtitle")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Expected banner height (${bannerBounds.height}) > minHeightPx ($minHeightPx); " +
                "badge=$badgeBounds, title=$titleBounds, subtitle=$subtitleBounds",
            bannerBounds.height > minHeightPx
        )
        assertTrue(
            "Expected title top (${titleBounds.top}) >= badge bottom (${badgeBounds.bottom})",
            titleBounds.top >= badgeBounds.bottom
        )
        assertTrue(
            "Expected subtitle top (${subtitleBounds.top}) >= title bottom (${titleBounds.bottom})",
            subtitleBounds.top >= titleBounds.bottom
        )
        assertTrue(
            "Expected subtitle bottom (${subtitleBounds.bottom}) <= banner bottom (${bannerBounds.bottom})",
            subtitleBounds.bottom <= bannerBounds.bottom
        )
        assertTrue(badgeBounds.right <= bannerBounds.right)
        assertTrue(titleBounds.right <= bannerBounds.right)
        assertTrue(subtitleBounds.right <= bannerBounds.right)
    }

    @Test
    fun `HeroPreservationBanner em 400dp com fontScale 1_0 mantem proporcao minima de 232dp sem overflow`() {
        var minHeightPx = 0f

        composeTestRule.setContent {
            minHeightPx = with(LocalDensity.current) { 232.dp.toPx() }
            ParintintinTheme {
                Box(modifier = Modifier.width(400.dp)) {
                    HeroPreservationBanner(
                        badgeText = sampleBadge,
                        title = sampleHeroTitle,
                        subtitle = sampleHeroSubtitle
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val bannerBounds = composeTestRule.onNodeWithTag("home_hero_banner")
            .fetchSemanticsNode().boundsInRoot
        val titleBounds = composeTestRule.onNodeWithTag("home_hero_title")
            .fetchSemanticsNode().boundsInRoot
        val subtitleBounds = composeTestRule.onNodeWithTag("home_hero_subtitle")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(bannerBounds.height >= minHeightPx - 1f)
        assertTrue(subtitleBounds.bottom <= bannerBounds.bottom)
        assertTrue(titleBounds.right <= bannerBounds.right)
        assertTrue(subtitleBounds.right <= bannerBounds.right)
    }
}
