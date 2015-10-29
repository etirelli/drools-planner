/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.result;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.impl.measurement.ScoreDifferencePercentage;
import org.optaplanner.benchmark.impl.ranking.SubSingleBenchmarkRankingComparator;
import org.optaplanner.benchmark.impl.report.BenchmarkReport;
import org.optaplanner.benchmark.impl.statistic.SubSingleStatistic;
import org.optaplanner.benchmark.impl.statistic.StatisticUtils;
import org.optaplanner.core.api.score.FeasibilityScore;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.core.impl.score.ScoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents 1 benchmark for 1 {@link Solver} configuration for 1 problem instance (data set).
 */
@XStreamAlias("singleBenchmarkResult")
public class SingleBenchmarkResult implements BenchmarkResult {

    protected static final transient Logger logger = LoggerFactory.getLogger(SingleBenchmarkResult.class);

    @XStreamOmitField // Bi-directional relationship restored through BenchmarkResultIO
    private SolverBenchmarkResult solverBenchmarkResult;
    @XStreamOmitField // Bi-directional relationship restored through BenchmarkResultIO
    private ProblemBenchmarkResult problemBenchmarkResult;

    @XStreamImplicit(itemFieldName = "subSingleBenchmarkResult")
    private List<SubSingleBenchmarkResult> subSingleBenchmarkResultList = null;

    private Long usedMemoryAfterInputSolution = null;

    private Integer failureCount = null;
    private Integer totalUninitializedVariableCount = null;
    private Score totalScore = null;
    private Integer averageUninitializedVariableCount = null;
    private Score averageScore = null;
    private SubSingleBenchmarkResult median = null;
    private SubSingleBenchmarkResult best = null;
    private SubSingleBenchmarkResult worst = null;
    private Integer uninitializedSolutionCount = null;
    private Integer infeasibleScoreCount = null;
    // Not a Score because
    // - the squaring would cause overflow for relatively small int and long scores.
    // - standard deviation should not be rounded to integer numbers
    private double[] standardDeviationDoubles = null;
    private long timeMillisSpent = -1L;
    private long calculateCount = -1L;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    // Compared to winningSingleBenchmarkResult in the same ProblemBenchmarkResult (which might not be the overall favorite)
    private Score winningScoreDifference = null;
    private ScoreDifferencePercentage worstScoreDifferencePercentage = null;

    // Ranking starts from 0
    private Integer ranking = null;

    public SingleBenchmarkResult(SolverBenchmarkResult solverBenchmarkResult, ProblemBenchmarkResult problemBenchmarkResult) {
        this.solverBenchmarkResult = solverBenchmarkResult;
        this.problemBenchmarkResult = problemBenchmarkResult;
    }

    public void initSubSingleStatisticMaps() {
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.initSubSingleStatisticMap();
        }
    }

    public SolverBenchmarkResult getSolverBenchmarkResult() {
        return solverBenchmarkResult;
    }

    public void setSolverBenchmarkResult(SolverBenchmarkResult solverBenchmarkResult) {
        this.solverBenchmarkResult = solverBenchmarkResult;
    }

    public ProblemBenchmarkResult getProblemBenchmarkResult() {
        return problemBenchmarkResult;
    }

    public void setProblemBenchmarkResult(ProblemBenchmarkResult problemBenchmarkResult) {
        this.problemBenchmarkResult = problemBenchmarkResult;
    }

    public List<SubSingleBenchmarkResult> getSubSingleBenchmarkResultList() {
        return subSingleBenchmarkResultList;
    }

    public void setSubSingleBenchmarkResultList(List<SubSingleBenchmarkResult> subSingleBenchmarkResultList) {
        this.subSingleBenchmarkResultList = subSingleBenchmarkResultList;
    }

    /**
     * @return null if {@link PlannerBenchmarkResult#hasMultipleParallelBenchmarks()} return true
     */
    public Long getUsedMemoryAfterInputSolution() {
        return usedMemoryAfterInputSolution;
    }

    public void setUsedMemoryAfterInputSolution(Long usedMemoryAfterInputSolution) {
        this.usedMemoryAfterInputSolution = usedMemoryAfterInputSolution;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public long getTimeMillisSpent() {
        return timeMillisSpent;
    }

    public void setTimeMillisSpent(long timeMillisSpent) {
        this.timeMillisSpent = timeMillisSpent;
    }

    public long getCalculateCount() {
        return calculateCount;
    }

    public void setCalculateCount(long calculateCount) {
        this.calculateCount = calculateCount;
    }

    public Score getWinningScoreDifference() {
        return winningScoreDifference;
    }

    public void setWinningScoreDifference(Score winningScoreDifference) {
        this.winningScoreDifference = winningScoreDifference;
    }

    public ScoreDifferencePercentage getWorstScoreDifferencePercentage() {
        return worstScoreDifferencePercentage;
    }

    public void setWorstScoreDifferencePercentage(ScoreDifferencePercentage worstScoreDifferencePercentage) {
        this.worstScoreDifferencePercentage = worstScoreDifferencePercentage;
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    @Override
    public Score getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Score averageScore) {
        this.averageScore = averageScore;
    }

    public SubSingleBenchmarkResult getMedian() {
        return median;
    }

    public SubSingleBenchmarkResult getBest() {
        return best;
    }

    public SubSingleBenchmarkResult getWorst() {
        return worst;
    }

    public double[] getStandardDeviationDoubles() {
        return standardDeviationDoubles;
    }

    @Override
    public Integer getAverageUninitializedVariableCount() {
        return averageUninitializedVariableCount;
    }

    public void setAverageUninitializedVariableCount(Integer averageUninitializedVariableCount) {
        this.averageUninitializedVariableCount = averageUninitializedVariableCount;
    }

    public Integer getInfeasibleScoreCount() {
        return infeasibleScoreCount;
    }

    public Integer getTotalUninitializedVariableCount() {
        return totalUninitializedVariableCount;
    }

    public Integer getUninitializedSolutionCount() {
        return uninitializedSolutionCount;
    }

    public Score getTotalScore() {
        return totalScore;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    /**
     * @return never null, filename safe
     */
    public String getName() {
        return problemBenchmarkResult.getName() + "_" + solverBenchmarkResult.getName();
    }

    public File getBenchmarkReportDirectory() {
        return problemBenchmarkResult.getBenchmarkReportDirectory();
    }

    @Override
    public boolean hasAllSuccess() {
        return failureCount != null && failureCount == 0;
    }

    public boolean isInitialized() {
        return averageUninitializedVariableCount != null && averageUninitializedVariableCount == 0;
    }

    @Override
    public boolean hasAnyFailure() {
        return failureCount != null && failureCount != 0;
    }

    public boolean isScoreFeasible() {
        if (averageScore instanceof FeasibilityScore) {
            return ((FeasibilityScore) averageScore).isFeasible();
        } else {
            return true;
        }
    }

    public Long getAverageCalculateCountPerSecond() {
        long timeMillisSpent = this.timeMillisSpent;
        if (timeMillisSpent == 0L) {
            // Avoid divide by zero exception on a fast CPU
            timeMillisSpent = 1L;
        }
        return calculateCount * 1000L / timeMillisSpent;
    }

    public boolean isWinner() {
        return ranking != null && ranking.intValue() == 0;
    }

    public SubSingleStatistic getSubSingleStatistic(ProblemStatisticType problemStatisticType) {
        return getMedian().getEffectiveSubSingleStatisticMap().get(problemStatisticType);
    }

    public String getAverageScoreWithUninitializedPrefix() {
        return ScoreUtils.getScoreWithUninitializedPrefix(
                ConfigUtils.ceilDivide(getTotalUninitializedVariableCount(), getSuccessCount()),
                getAverageScore());
    }

    public int getSuccessCount() {
        return subSingleBenchmarkResultList.size() - failureCount;
    }

    public String getStandardDeviationString() {
        return StatisticUtils.getStandardDeviationString(standardDeviationDoubles);
    }

    // ************************************************************************
    // Accumulate methods
    // ************************************************************************

    public String getResultDirectoryName() {
        return solverBenchmarkResult.getName();
    }

    public File getResultDirectory() {
        return new File(problemBenchmarkResult.getProblemReportDirectory(), getResultDirectoryName());
    }

    public void makeDirs() {
        File singleReportDirectory = getResultDirectory();
        singleReportDirectory.mkdirs();
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.makeDirs();
        }
    }

    public int getTotalSubSingleCount() {
        return subSingleBenchmarkResultList.size();
    }

    public void accumulateResults(BenchmarkReport benchmarkReport) {
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : subSingleBenchmarkResultList) {
            subSingleBenchmarkResult.accumulateResults(benchmarkReport);
        }
        determineTotalsAndAveragesAndRanking();
        standardDeviationDoubles = StatisticUtils.determineStandardDeviationDoubles(subSingleBenchmarkResultList, averageScore, getSuccessCount());
        determineRepresentativeSubSingleBenchmarkResult();
    }

    private void determineRepresentativeSubSingleBenchmarkResult() {
        if (subSingleBenchmarkResultList == null || subSingleBenchmarkResultList.isEmpty()) {
            throw new IllegalStateException("Cannot get representative subSingleBenchmarkResult from empty subSingleBenchmarkResultList.");
        }
        List<SubSingleBenchmarkResult> subSingleBenchmarkResultListCopy = new ArrayList<SubSingleBenchmarkResult>(subSingleBenchmarkResultList);
        // sort (according to ranking) so that the best subSingle is at index 0
        Collections.sort(subSingleBenchmarkResultListCopy, new Comparator<SubSingleBenchmarkResult>() {
            @Override
            public int compare(SubSingleBenchmarkResult o1, SubSingleBenchmarkResult o2) {
                return new CompareToBuilder()
                        .append(o1.hasAnyFailure(), o2.hasAnyFailure())
                        .append(o1.getRanking(), o2.getRanking())
                        .toComparison();
            }
        });
        best = subSingleBenchmarkResultListCopy.get(0);
        worst = subSingleBenchmarkResultListCopy.get(subSingleBenchmarkResultListCopy.size() - 1);
        median = subSingleBenchmarkResultListCopy.get(ConfigUtils.ceilDivide(subSingleBenchmarkResultListCopy.size() - 1, 2));
        usedMemoryAfterInputSolution = median.getUsedMemoryAfterInputSolution();
        timeMillisSpent = median.getTimeMillisSpent();
        calculateCount = median.getCalculateCount();
        winningScoreDifference = median.getWinningScoreDifference();
        worstScoreDifferencePercentage = median.getWorstScoreDifferencePercentage();
    }

    private void determineTotalsAndAveragesAndRanking() {
        failureCount = 0;
        boolean firstNonFailure = true;
        totalScore = null;
        uninitializedSolutionCount = 0;
        totalUninitializedVariableCount = 0;
        infeasibleScoreCount = 0;
        List<SubSingleBenchmarkResult> successResultList = new ArrayList<SubSingleBenchmarkResult>(subSingleBenchmarkResultList);
        // Do not rank a SubSingleBenchmarkResult that has a failure
        for (Iterator<SubSingleBenchmarkResult> it = successResultList.iterator(); it.hasNext(); ) {
            SubSingleBenchmarkResult subSingleBenchmarkResult = it.next();
            if (subSingleBenchmarkResult.hasAnyFailure()) {
                failureCount++;
                it.remove();
            } else {
                if (!subSingleBenchmarkResult.isInitialized()) {
                    uninitializedSolutionCount++;
                    totalUninitializedVariableCount += subSingleBenchmarkResult.getUninitializedVariableCount();
                } else if (!subSingleBenchmarkResult.isScoreFeasible()) {
                    infeasibleScoreCount++;
                }
                if (firstNonFailure) {
                    totalScore = subSingleBenchmarkResult.getAverageScore();
                    firstNonFailure = false;
                } else {
                    totalScore = totalScore.add(subSingleBenchmarkResult.getAverageScore());
                }
            }
        }
        if (!firstNonFailure) {
            averageScore = totalScore.divide(getSuccessCount());
            averageUninitializedVariableCount = ConfigUtils.ceilDivide(totalUninitializedVariableCount, getSuccessCount());
        }
        determineRanking(successResultList);
    }

    private void determineRanking(List<SubSingleBenchmarkResult> rankedSubSingleBenchmarkResultList) {
        Comparator subSingleBenchmarkRankingComparator = new SubSingleBenchmarkRankingComparator();
        Collections.sort(rankedSubSingleBenchmarkResultList, Collections.reverseOrder(subSingleBenchmarkRankingComparator));
        int ranking = 0;
        SubSingleBenchmarkResult previousSubSingleBenchmarkResult = null;
        int previousSameRankingCount = 0;
        for (SubSingleBenchmarkResult subSingleBenchmarkResult : rankedSubSingleBenchmarkResultList) {
            if (previousSubSingleBenchmarkResult != null
                    && subSingleBenchmarkRankingComparator.compare(previousSubSingleBenchmarkResult, subSingleBenchmarkResult) != 0) {
                ranking += previousSameRankingCount;
                previousSameRankingCount = 0;
            }
            subSingleBenchmarkResult.setRanking(ranking);
            previousSubSingleBenchmarkResult = subSingleBenchmarkResult;
            previousSameRankingCount++;
        }
    }

    // ************************************************************************
    // Merger methods
    // ************************************************************************

    protected static SingleBenchmarkResult createMerge(SolverBenchmarkResult solverBenchmarkResult,
            ProblemBenchmarkResult problemBenchmarkResult, SingleBenchmarkResult oldResult) {
        SingleBenchmarkResult newResult = new SingleBenchmarkResult(solverBenchmarkResult, problemBenchmarkResult);
        newResult.subSingleBenchmarkResultList = new ArrayList<SubSingleBenchmarkResult>(oldResult.getSubSingleBenchmarkResultList().size());
        int subSingleBenchmarkIndex = 0;
        for (SubSingleBenchmarkResult oldSubResult : oldResult.subSingleBenchmarkResultList) {
            SubSingleBenchmarkResult.createMerge(newResult, oldSubResult, subSingleBenchmarkIndex);
            subSingleBenchmarkIndex++;
        }
        newResult.median = oldResult.median;
        newResult.best = oldResult.best;
        newResult.worst = oldResult.worst;
        solverBenchmarkResult.getSingleBenchmarkResultList().add(newResult);
        problemBenchmarkResult.getSingleBenchmarkResultList().add(newResult);
        return newResult;
    }

    @Override
    public String toString() {
        return getName();
    }

}
