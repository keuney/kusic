package com.keuney.music.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import javax.inject.Singleton

@HiltAndroidTest
class HiltInjectionTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var first: SampleDependency
    @Inject lateinit var second: SampleDependency
    @Inject @ApplicationContext lateinit var applicationContext: Context

    @Test
    fun injectsConstructorDependencyWithApplicationContextAndSingletonScope() {
        hiltRule.inject()

        assertSame(applicationContext, first.context)
        assertSame(applicationContext.applicationContext, first.context)
        assertEquals("com.keuney.music", first.context.packageName)
        assertSame(first, second)
    }
}

@Singleton
class SampleDependency @Inject constructor(
    @param:ApplicationContext val context: Context,
)
