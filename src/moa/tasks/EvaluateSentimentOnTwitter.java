/*
 *    EvaluateSentimentOnTwitter.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import moa.classifiers.Classifier;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.ClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.options.StringOption;
import moa.streams.twitter.TweetReader;
import weka.core.Instance;
import weka.core.Utils;

/**
 * Task for evaluating the sentiment on a twitter stream by testing and training with emoticons.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 * @version $Revision: 7 $
 */
public class EvaluateSentimentOnTwitter extends MainTask {

    @Override
    public String getPurposeString() {
        return "Evaluates the sentiment on a twitter stream by testing and training with emoticons.";
    }
    private static final long serialVersionUID = 1L;

    public ClassOption learnerOption = new ClassOption("learner", 'l',
            "Classifier to train.", Classifier.class, "bayes.NaiveBayes");

    public StringOption queryTrainOption = new StringOption("queryTrain", 's',
            "Query string to use for obtaining tweets to train the classifier.", "");

    public StringOption queryTestOption = new StringOption("queryTest", 'w',
            "Query string to use for obtaining tweets to test the classifier.", "");

    public StringOption languageFilterOption = new StringOption("languageFilter", 'a',
            "Filter by language.", "en");

    public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
            "Classification performance evaluation method.",
            ClassificationPerformanceEvaluator.class,
            "BasicClassificationPerformanceEvaluator");

    public IntOption instanceLimitOption = new IntOption("instanceLimit", 'i',
            "Maximum number of instances to test/train on  (-1 = no limit).",
            100000000, -1, Integer.MAX_VALUE);

    public IntOption timeLimitOption = new IntOption("timeLimit", 't',
            "Maximum number of seconds to test/train for (-1 = no limit).", -1,
            -1, Integer.MAX_VALUE);

    public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
            'f',
            "How many instances between samples of the learning performance.",
            100000, 0, Integer.MAX_VALUE);

    public IntOption instancesTrainOption = new IntOption("instancesTrain", 'b',
            "Number of instances to train", 1000, 0,
            Integer.MAX_VALUE);


    public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
            "File to append intermediate csv results to.", null, "csv", true);

    @Override
    public Class<?> getTaskResultType() {
        return LearningCurve.class;
    }

    @Override
    protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
        Classifier learner = (Classifier) getPreparedClassOption(this.learnerOption);

        TweetReader streamTrain = new TweetReader(this.queryTrainOption.getValue(), this.languageFilterOption.getValue(), true); //isTraining
        streamTrain.prepareForUse();
        TweetReader streamTest = new TweetReader(this.queryTestOption.getValue(), this.languageFilterOption.getValue(), false);
        streamTest.prepareForUse();

        ClassificationPerformanceEvaluator evaluator = (ClassificationPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
        LearningCurve learningCurve = new LearningCurve(
                "learning evaluation instances");

        learner.setModelContext(streamTrain.getHeader());
        int maxInstances = this.instanceLimitOption.getValue();
        long instancesProcessed = 0;
        long instancesBatchProcessed = 0;
        long instancesBatchProcessedPositive = 0;
        int maxSeconds = this.timeLimitOption.getValue();
        int secondsElapsed = 0;
        monitor.setCurrentActivity("Evaluating learner...", -1.0);

        File dumpFile = this.dumpFileOption.getFile();
        PrintStream immediateResultStream = null;
        if (dumpFile != null) {
            try {
                if (dumpFile.exists()) {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile, true), true);
                } else {
                    immediateResultStream = new PrintStream(
                            new FileOutputStream(dumpFile), true);
                }
            } catch (Exception ex) {
                throw new RuntimeException(
                        "Unable to open immediate result file: " + dumpFile, ex);
            }
        }

        boolean firstDump = true;
        boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
        long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        long lastEvaluateStartTime = evaluateStartTime;
        double RAMHours = 0.0;
        double firstTrainingInstances = 0;
        boolean streamTestStarted = false;
        while (streamTest.hasMoreInstances()
                && ((maxInstances < 0) || (instancesProcessed < maxInstances))
                && ((maxSeconds < 0) || (secondsElapsed < maxSeconds))) {
            Instance instTest = null;
            if (!streamTestStarted && firstTrainingInstances >= instancesTrainOption.getValue() ){
                streamTestStarted = true;
            }
            Instance instTraining = null;
            if (!streamTestStarted && streamTrain.hasMoreInstances()) {
                instTraining = streamTrain.checkIfThereIsAnyInstance();
            }
            if (streamTest.hasMoreInstances()) {
                instTest = streamTest.checkIfThereIsAnyInstance();
            }
            if (instTraining != null) {
                firstTrainingInstances++;
                learner.trainOnInstance(instTraining);
            }
            if (instTest != null) {
                //System.out.println("!! TESTING "+instancesProcessed);
                double[] pred = learner.getVotesForInstance(instTest);
                double prediction = Utils.maxIndex(pred);
                instTest.setClassValue("H"); // accuracy = classes with "H"
                evaluator.addResult(instTest, pred);

                instancesProcessed++;
                instancesBatchProcessed++;
                if (prediction == 0) {
                    instancesBatchProcessedPositive++;
                }
                if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0 ) {
                    long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
                    double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
                    double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
                    double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); //GBs
                    RAMHoursIncrement *= (timeIncrement / 3600.0); //Hours
                    RAMHours += RAMHoursIncrement;
                    lastEvaluateStartTime = evaluateTime;
                    //System.out.println("Learning curve "+ instancesBatchProcessedPositive+" "+instancesBatchProcessed);
                    learningCurve.insertEntry(new LearningEvaluation(
                            new Measurement[]{
                                new Measurement("learning evaluation instances", instancesProcessed),
                                new Measurement("evaluation time (" + (preciseCPUTiming ? "cpu " : "") + "seconds)", time),
                                new Measurement("model cost (RAM-Hours)", RAMHours),
                                new Measurement("positive sentiment instances (percentage)", 100.0 * instancesBatchProcessedPositive / instancesBatchProcessed)
                            },
                            evaluator, learner));
                    instancesBatchProcessed = 0;
                    instancesBatchProcessedPositive = 0;
                    if (immediateResultStream != null) {
                        if (firstDump) {
                            immediateResultStream.println(learningCurve.headerToString());
                            firstDump = false;
                        }
                        immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
                        immediateResultStream.flush();
                    }
                }
                if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
                    if (monitor.taskShouldAbort()) {
                        return null;
                    }
                    long estimatedRemainingInstances = streamTest.estimatedRemainingInstances();
                    if (maxInstances > 0) {
                        long maxRemaining = maxInstances - instancesProcessed;
                        if ((estimatedRemainingInstances < 0)
                                || (maxRemaining < estimatedRemainingInstances)) {
                            estimatedRemainingInstances = maxRemaining;
                        }
                    }
                    monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
                            : (double) instancesProcessed
                            / (double) (instancesProcessed + estimatedRemainingInstances));
                    if (monitor.resultPreviewRequested()) {
                        monitor.setLatestResultPreview(learningCurve.copy());
                    }
                    secondsElapsed = (int) TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread()
                            - evaluateStartTime);
                }
            }
        }
       // System.out.println("Reason to finish : " + streamTest.hasMoreInstances()
       //         + " " + (maxInstances < 0) + " " + (instancesProcessed < maxInstances)
       //         + " " + (maxSeconds < 0) + " " + (secondsElapsed < maxSeconds));
        if (immediateResultStream != null) {
            immediateResultStream.close();
        }
        streamTest.shutdown();
        streamTrain.shutdown();
        return learningCurve;
    }
}
