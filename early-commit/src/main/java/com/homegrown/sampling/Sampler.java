package com.homegrown.sampling;

import java.io.File;
import java.io.IOException;

import com.homegrown.Main;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.log4j.Logger;

import javax.sound.sampled.LineUnavailableException;


public final class Sampler {
	private static final Logger logger = Logger.getLogger (Sampler.class);

	private final double[] trainFFT;

	public Sampler () {trainFFT = null;}
	/*public Sampler (float sampleRate, int samplesNumber, int transformationSize, String recordingsDirectory) {
		trainFFT = recordAndTransform (sampleRate,samplesNumber,transformationSize,recordingsDirectory);

		while (true) {
			double[] testFFT = recordAndTransform (sampleRate,samplesNumber,transformationSize,recordingsDirectory);
			System.out.println ("%% Similarity: "+(int)Math.round(cosineSimilarity(testFFT,trainFFT)*100)+" %.");
		}
	}*/

	public double cosineSimilarity (double[] u, double[] v) {
		String debugMsg = "cosineSimilarity::";
		logger.info(debugMsg+"nominator="+innerProduct(u,v));
		logger.info(debugMsg+"denominatior="+Math.sqrt(innerProduct(u, u)*innerProduct(v, v)));
		return innerProduct(u,v) / Math.sqrt(innerProduct(u,u)*innerProduct(v,v));
	}

	private double innerProduct (double[] u, double[] v) {
		if (u.length != v.length) {
			System.err.println ("%% ERROR - Vector dimensions do not match "+u.length+" != "+v.length+"...");
			return 0;
		}else{
			double sum = 0.0;
			for (int i=0; i<u.length; ++i) 	{
				sum += u[i]*v[i];
			}
			return sum;
		}
	}

	public double[] recordAndTransform (float sampleRate, int samplesNumber, int transformationSize, String recordingsDirectory) {
		System.out.println ("\nfs = "+sampleRate+";\n");
		/**
		int[] sample = record (sampleRate,samplesNumber);
		System.out.print ("\n\ny = [");
		for (int i=0; i<sample.length; ++i) {
			System.out.print (sample[i]+" ");
		}
		System.out.println ("];\n");
		return powerSpectrum(normalizeQuantiz(sample),transformationSize);
		**/
		return powerSpectrum(normalizeQuantiz(record(sampleRate,samplesNumber,recordingsDirectory)),transformationSize);
	}

	public double[] normalizeQuantiz (byte[] signal) {
		int maxAbsValue = 0;
		int powerOfTwoLength = 2;
		while (powerOfTwoLength < signal.length) {
			powerOfTwoLength<<=1;
		}
		double[] normalized = new double[powerOfTwoLength>>1];
		for (int i=0; i<normalized.length; ++i) {
			int temp = signal[i]<0?-signal[i]:signal[i];
			if (temp > maxAbsValue) maxAbsValue = temp;
		}
		System.out.print ("\n\nnormalized = [");
		for (int i=0; i<normalized.length; ++i) {
			normalized[i] = ((double)signal[i])/maxAbsValue;
			System.out.print (normalized[i]+" ");
		}
		System.out.println ("];\n");
		return normalized;
	}

	public double[] powerSpectrum (double[] signal, int N) {
		final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

		//System.out.print ("\nN = "+N+";\nY = [");
		final Complex[] spectrum = fft.transform (signal,TransformType.FORWARD);
		final double[] powerSpectrum = new double[N];
		int step = spectrum.length / powerSpectrum.length ;
		for (int i=0; i<powerSpectrum.length; ++i) {
			powerSpectrum[i] = 0.0;
			for (int j=i*step; j<(i*step+step); ++j) {
				powerSpectrum[i] += spectrum[j].abs();
			}
			//System.out.print (powerSpectrum[i]+" ");
		}
		//System.out.println ("];\n");

		return powerSpectrum;
	}

	public byte[] record (final float sampleRate, int samplesNumber, String recordingsDirectory) {
		final SoundRecordingUtil recorder = new SoundRecordingUtil();
		final long durationMsec = (long)(1000.0*Math.ceil(samplesNumber/sampleRate));
		Thread recordThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.err.println ("%% Start recording for "+durationMsec+" msec...");
					recorder.start (sampleRate);
				} catch (LineUnavailableException ex) {
					ex.printStackTrace();
					System.exit(-1);
				}
			}
		});
		recordThread.start();

		try{Thread.sleep(durationMsec);
		}catch (InterruptedException ex) {
			ex.printStackTrace();
		}

		try{
			recorder.stop();
			System.err.println("%% STOPPED");
			recorder.save (new File(recordingsDirectory+"/recording"+System.currentTimeMillis()+".wav"));
			return recorder.getBytes();
		}catch (IOException ex){ex.printStackTrace();
		}finally{ System.err.println("%% DONE"); }
		return null;
	}
}
