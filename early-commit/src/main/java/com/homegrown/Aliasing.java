package com.homegrown;

import com.homegrown.sampling.*;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.util.Properties;
import java.io.IOException;

public class Aliasing {

    private static final Logger logger = Logger.getLogger (Aliasing.class);
    private static final Sampler sampler = new Sampler();

    private static int N1 = -1;
    private static int N2 = -1;
    private static float fs1 = 2000.0f;
    private static float fs2 = 40000.0f;

    public static float compute () throws IOException{
        String debugMsg = "compute::";
        ApplicationContext ctx = new ClassPathXmlApplicationContext("producerContext.xml");
        Properties appProperties = (Properties) ctx.getBean("appProperties");
        String recordingsDirectory = appProperties.getProperty("recordingsDirectory");
        int samplesNumber = Integer.parseInt(appProperties.getProperty("samplesNumber"));

        for  (int counter=1; Math.abs(fs2-fs1)>100.0f; ++counter) {
            logger.info (debugMsg+" NOW RUNNING ITERATION #"+counter);
            byte[] sample1 = sampler.record (fs1,samplesNumber,recordingsDirectory);
            /*StringBuffer print = new StringBuffer();
            for (int i=0; i<sample1.length; ++i) {
                print.append(",");
                print.append(sample1[i]);
            }
            if (sample1.length > 0) {
                print.deleteCharAt(0);
            }
            logger.info(debugMsg+" sample1 = ["+print.toString()+"]");*/
            if (getMin(sample1) == getMax(sample1)) {
                fs1 = 2*fs1;
                continue;
            }
            byte[] sample2 = sampler.record (fs2,samplesNumber,recordingsDirectory);

            N1 = Integer.parseInt(appProperties.getProperty("transformationSize"));
            N2 = (int)Math.ceil(N1*fs2/fs1);
            logger.info (debugMsg+" N1="+N1);
            logger.info (debugMsg+" N2="+N2);

            double[] fft1 = sampler.powerSpectrum(sampler.normalizeQuantiz(sample1),N1);
            double[] fft2 = sampler.powerSpectrum(sampler.normalizeQuantiz(sample2),N2);
            int tail_length = (int)Math.floor((N2-N1)/2);
            double[] fft2pruned = new double[N1];
            for (int i=0; i<N1; ++i) {
                if (i<((N2>>1)-tail_length) || i>((N2>>1)+tail_length)) {
                    fft2pruned[i] = fft2[i];
                }
            }
            double similarity = sampler.cosineSimilarity (fft1,fft2pruned);
            if (similarity < .9f) {
                fs1 = (fs2+fs1)/2.0f;
            }else{
                fs2 = fs1;
                fs1 = fs1/2.0f;
            }
            logger.info (debugMsg+"fs1="+fs1+",fs2="+fs2);
        }
        float fs_converg = (fs1+fs1)/2.0f;
        logger.info(debugMsg+"fs_converg="+fs_converg);
        return fs_converg;
    }

    private static byte getMin (byte[] audioData) throws IOException {
        byte min = Byte.MAX_VALUE;
        for (int i=0; i<audioData.length; ++i) {
            if (audioData[i] < min) {
                min = audioData[i];
            }
        }
        logger.info ("getMin::MIN="+min);
        return min;
    }

    private static byte getMax (byte[] audioData) throws IOException{
        byte max = Byte.MIN_VALUE;
        for (int i=0; i<audioData.length; ++i) {
            if (audioData[i] > max) {
                max = audioData[i];
            }
        }
        logger.info ("getMax::MAX="+max);
        return max;
    }
}
