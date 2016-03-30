package com.rackspacecloud.blueflood.service;

import com.rackspacecloud.blueflood.io.AstyanaxReader;
import com.rackspacecloud.blueflood.rollup.Granularity;
import com.rackspacecloud.blueflood.rollup.SlotKey;
import com.rackspacecloud.blueflood.types.Locator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.*;

public class LocatorFetchRunnableRunTest {


    ScheduleContext scheduleCtx;
    ExecutorService rollupReadExecutor;
    ThreadPoolExecutor rollupWriteExecutor;
    ExecutorService enumValidatorExecutor;
    AstyanaxReader astyanaxReader;

    LocatorFetchRunnable lfr;

    RollupExecutionContext executionContext;
    RollupBatchWriter rollupBatchWriter;

    @Before
    public void setUp() throws IOException {

        Configuration.getInstance().init();

        this.scheduleCtx = mock(ScheduleContext.class);
        this.rollupReadExecutor = mock(ExecutorService.class);
        this.rollupWriteExecutor = mock(ThreadPoolExecutor.class);
        this.enumValidatorExecutor = mock(ExecutorService.class);
        this.astyanaxReader = mock(AstyanaxReader.class);

        this.lfr = mock(LocatorFetchRunnable.class);
        doCallRealMethod().when(lfr).getGranularity();
        doCallRealMethod().when(lfr).getParentSlot();
        doCallRealMethod().when(lfr).getShard();
        doCallRealMethod().when(lfr).initialize(
                Matchers.<ScheduleContext>any(),
                Matchers.<SlotKey>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<ThreadPoolExecutor>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<AstyanaxReader>any());
        doCallRealMethod().when(lfr).run();

        executionContext = mock(RollupExecutionContext.class);
        rollupBatchWriter = mock(RollupBatchWriter.class);

        doReturn(executionContext).when(lfr).createRollupExecutionContext();
        doReturn(rollupBatchWriter).when(lfr).createRollupBatchWriter(
                Matchers.<RollupExecutionContext>any());

        Configuration.getInstance().setProperty(CoreConfig.ENABLE_HISTOGRAMS,
                "false");
    }

    @After
    public void tearDown() throws IOException {
        Configuration.getInstance().init();
    }

    Set<Locator> generateLocators(int n) {
        Set<Locator> locators = new HashSet<Locator>();
        if (n >= 1) locators.add(Locator.createLocatorFromPathComponents("tenant1", "a", "b", "c"));
        if (n >= 2) locators.add(Locator.createLocatorFromPathComponents("tenant2", "a", "b", "x"));
        if (n >= 3) locators.add(Locator.createLocatorFromPathComponents("tenant3", "d", "e", "f"));
        return locators;
    }

    @Test
    public void finestGranularityWillQuitEarly() {

        // given
        SlotKey destSlotKey = SlotKey.of(Granularity.FULL, 0, 0);
        this.lfr.initialize(scheduleCtx, destSlotKey,
                rollupReadExecutor, rollupWriteExecutor, enumValidatorExecutor,
                astyanaxReader);

        // when
        lfr.run();

        // then
        verifyZeroInteractions(executionContext);
        verifyZeroInteractions(rollupBatchWriter);
        verify(scheduleCtx).getCurrentTimeMillis();
        verifyNoMoreInteractions(scheduleCtx);
        verifyZeroInteractions(rollupReadExecutor);
        verifyZeroInteractions(rollupWriteExecutor);
        verifyZeroInteractions(enumValidatorExecutor);
        verifyZeroInteractions(astyanaxReader);
        verify(lfr).initialize(
                Matchers.<ScheduleContext>any(),
                Matchers.<SlotKey>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<ThreadPoolExecutor>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<AstyanaxReader>any());
        verify(lfr).run();
        verify(lfr, times(3)).getGranularity();
        verify(lfr, times(1)).getParentSlot();
        verifyNoMoreInteractions(lfr);
    }

    @Test
    public void noLocatorsMeansNoneAreProcessed() {

        // given
        SlotKey destSlotKey = SlotKey.of(Granularity.MIN_5, 0, 0);
        lfr.initialize(scheduleCtx, destSlotKey,
                rollupReadExecutor, rollupWriteExecutor, enumValidatorExecutor,
                astyanaxReader);
        doReturn(generateLocators(0)).when(lfr).getLocators(
                Matchers.<RollupExecutionContext>any());

        // when
        lfr.run();

        // then
        verifyZeroInteractions(executionContext);
        verifyZeroInteractions(rollupBatchWriter);
        verify(scheduleCtx).getCurrentTimeMillis();
        verifyNoMoreInteractions(scheduleCtx);
        verifyZeroInteractions(rollupReadExecutor);
        verifyZeroInteractions(rollupWriteExecutor);
        verifyZeroInteractions(enumValidatorExecutor);
        verifyZeroInteractions(astyanaxReader);
        verify(lfr).initialize(
                Matchers.<ScheduleContext>any(),
                Matchers.<SlotKey>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<ThreadPoolExecutor>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<AstyanaxReader>any());
        verify(lfr).run();
        verify(lfr, times(2)).getGranularity();
        verify(lfr, times(1)).getParentSlot();
        verify(lfr).createRollupExecutionContext();
        verify(lfr).createRollupBatchWriter(Matchers.<RollupExecutionContext>any());
        verify(lfr).getLocators(Matchers.<RollupExecutionContext>any());

        verify(lfr, never()).processLocator(anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any(),
                Matchers.<Locator>any());

        verify(lfr).drainExecutionContext(anyLong(), anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any());
        verifyNoMoreInteractions(lfr);
    }

    @Test
    public void singleLocatorsMeansOneIsProcessed() {

        // given
        SlotKey destSlotKey = SlotKey.of(Granularity.MIN_5, 0, 0);
        lfr.initialize(scheduleCtx, destSlotKey,
                rollupReadExecutor, rollupWriteExecutor, enumValidatorExecutor,
                astyanaxReader);
        doReturn(generateLocators(1)).when(lfr).getLocators(
                Matchers.<RollupExecutionContext>any());

        // when
        lfr.run();

        // then
        verifyZeroInteractions(executionContext);
        verifyZeroInteractions(rollupBatchWriter);
        verify(scheduleCtx).getCurrentTimeMillis();
        verifyNoMoreInteractions(scheduleCtx);
        verifyZeroInteractions(rollupReadExecutor);
        verifyZeroInteractions(rollupWriteExecutor);
        verifyZeroInteractions(enumValidatorExecutor);
        verifyZeroInteractions(astyanaxReader);
        verify(lfr).initialize(
                Matchers.<ScheduleContext>any(),
                Matchers.<SlotKey>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<ThreadPoolExecutor>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<AstyanaxReader>any());
        verify(lfr).run();
        verify(lfr, times(2)).getGranularity();
        verify(lfr, times(1)).getParentSlot();
        verify(lfr).createRollupExecutionContext();
        verify(lfr).createRollupBatchWriter(Matchers.<RollupExecutionContext>any());
        verify(lfr).getLocators(Matchers.<RollupExecutionContext>any());

        verify(lfr, times(1)).processLocator(anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any(),
                Matchers.<Locator>any());

        verify(lfr).drainExecutionContext(anyLong(), anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any());
        verifyNoMoreInteractions(lfr);
    }

    @Test
    public void whenMultipleLocatorsAreGivenEachIsProcessed() {

        // given
        SlotKey destSlotKey = SlotKey.of(Granularity.MIN_5, 0, 0);
        lfr.initialize(scheduleCtx, destSlotKey,
                rollupReadExecutor, rollupWriteExecutor, enumValidatorExecutor,
                astyanaxReader);
        doReturn(generateLocators(3)).when(lfr).getLocators(
                Matchers.<RollupExecutionContext>any());

        // when
        lfr.run();

        // then
        verifyZeroInteractions(executionContext);
        verifyZeroInteractions(rollupBatchWriter);
        verify(scheduleCtx).getCurrentTimeMillis();
        verifyNoMoreInteractions(scheduleCtx);
        verifyZeroInteractions(rollupReadExecutor);
        verifyZeroInteractions(rollupWriteExecutor);
        verifyZeroInteractions(enumValidatorExecutor);
        verifyZeroInteractions(astyanaxReader);
        verify(lfr).initialize(
                Matchers.<ScheduleContext>any(),
                Matchers.<SlotKey>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<ThreadPoolExecutor>any(),
                Matchers.<ExecutorService>any(),
                Matchers.<AstyanaxReader>any());
        verify(lfr).run();
        verify(lfr, times(2)).getGranularity();
        verify(lfr, times(1)).getParentSlot();
        verify(lfr).createRollupExecutionContext();
        verify(lfr).createRollupBatchWriter(Matchers.<RollupExecutionContext>any());
        verify(lfr).getLocators(Matchers.<RollupExecutionContext>any());

        verify(lfr, times(3)).processLocator(anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any(),
                Matchers.<Locator>any());

        verify(lfr).drainExecutionContext(anyLong(), anyInt(),
                Matchers.<RollupExecutionContext>any(),
                Matchers.<RollupBatchWriter>any());
        verifyNoMoreInteractions(lfr);
    }
}