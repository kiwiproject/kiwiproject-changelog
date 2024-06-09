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
import org.mockito.Mockito.only
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

        private val urlPrefix = "https://github.com/acme/space-modulator/milestones/"

        private lateinit var milestoneManager : GitHubMilestoneManager

        @BeforeEach
        fun setUp() {
            milestoneManager = mock(GitHubMilestoneManager::class.java)
        }

        @Test
        fun shouldClose_BasedOnRevision() {
            val number = 1
            val title = "1.4.2"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val closedMilestone = ChangelogGeneratorMain.closeMilestone(
                revision = "v$title",
                maybeMilestoneTitle = null,
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestoneManagerCalls(title, number)
        }

        @Test
        fun shouldClose_UsingExplicitMilestone() {
            val number = 4
            val title = "1.5.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitle(anyString())).thenReturn(milestone)
            `when`(milestoneManager.closeMilestone(anyInt())).thenReturn(milestone)

            val closedMilestone = ChangelogGeneratorMain.closeMilestone(
                revision = "rel-1.5.0",
                maybeMilestoneTitle = "1.5.0",
                milestoneManager = milestoneManager
            )

            assertThat(closedMilestone).isSameAs(milestone)

            verifyMilestoneManagerCalls(title, number)
        }

        private fun verifyMilestoneManagerCalls(title: String, number: Int) {
            verify(milestoneManager).getOpenMilestoneByTitle(title)
            verify(milestoneManager).closeMilestone(number)
            verifyNoMoreInteractions(milestoneManager)
        }
    }

    @Nested
    inner class CreateMilestone {

        private val urlPrefix = "https://github.com/acme/space-modulator/milestones/"

        private lateinit var milestoneManager: GitHubMilestoneManager

        @BeforeEach
        fun setUp() {
            milestoneManager = mock(GitHubMilestoneManager::class.java)
        }

        @Test
        fun shouldCreateNewMilestone() {
            val number = 3
            val title = "4.2.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitleOrNull(anyString())).thenReturn(null)
            `when`(milestoneManager.createMilestone(anyString())).thenReturn(milestone)

            val newMilestone = ChangelogGeneratorMain.createMilestone(title, milestoneManager)

            assertThat(newMilestone).isSameAs(milestone)

            verify(milestoneManager).getOpenMilestoneByTitleOrNull(title)
            verify(milestoneManager).createMilestone(title)
            verifyNoMoreInteractions(milestoneManager)
        }

        @Test
        fun shouldReturnExistingMilestone_WhenAlreadyExists() {
            val number = 3
            val title = "4.2.0"
            val htmlUrl = urlPrefix + number
            val milestone = GitHubMilestone(number, title, htmlUrl)
            `when`(milestoneManager.getOpenMilestoneByTitleOrNull(anyString())).thenReturn(milestone)

            val existingMilestone = ChangelogGeneratorMain.createMilestone(title, milestoneManager)

            assertThat(existingMilestone).isSameAs(milestone)

            verify(milestoneManager, only()).getOpenMilestoneByTitleOrNull(title)
        }
    }
}
