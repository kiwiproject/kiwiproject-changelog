package org.kiwiproject.changelog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwiproject.changelog.github.GitHubMilestoneManager
import org.kiwiproject.changelog.github.GitHubMilestoneManager.GitHubMilestone
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

@DisplayName("ChangelogGeneratorMain")
class ChangelogGeneratorMainTest {

    @Test
    fun shouldGetVersion() {
        val versionArray = ChangelogGeneratorMain.VersionProvider().version
        assertThat(versionArray)
            .describedAs("This should be 'unknown' in unit test environment")
            .contains("[unknown]")
    }

    @Nested
    inner class CloseMilestone {

        private val url = "https://github.com/acme/space-modulator/milestone/1"

        private lateinit var milestoneManager : GitHubMilestoneManager

        @BeforeEach
        fun setUp() {
            milestoneManager = mock(GitHubMilestoneManager::class.java)
        }

        @Test
        fun shouldClose_BasedOnRevision() {
            val number = 1
            val title = "1.4.2"
            val milestone = GitHubMilestone(number, title, url)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val closedMilestone = ChangelogGeneratorMain.closeMilestone(
                revision = "v$title",
                maybeMilestoneTitle = null,
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestonManagerCalls(title, number)
        }

        @Test
        fun shouldClose_UsingExplicitMilestone() {
            val number = 4
            val title = "1.5.0"
            val milestone = GitHubMilestone(number, title, url)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val closedMilestone = ChangelogGeneratorMain.closeMilestone(
                revision = "rel-1.5.0",
                maybeMilestoneTitle = "1.5.0",
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestonManagerCalls(title, number)
        }

        private fun verifyMilestonManagerCalls(title: String, number: Int) {
            verify(milestoneManager).getOpenMilestoneByTitle(title)
            verify(milestoneManager).closeMilestone(number)
            verifyNoMoreInteractions(milestoneManager)
        }
    }
}
